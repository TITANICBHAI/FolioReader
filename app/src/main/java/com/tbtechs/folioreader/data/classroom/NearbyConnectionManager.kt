package com.tbtechs.folioreader.data.classroom

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyConnectionManager @Inject constructor() {

    companion object {
        private const val TAG = "NearbyConnManager"
        const val SERVICE_ID = "com.tbtechs.folioreader.classroom"
    }

    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Advertising : ConnectionState()
        object Discovering : ConnectionState()
        data class SessionsFound(val list: List<Pair<String, String>>) : ConnectionState()
        data class Connected(val endpointId: String, val name: String) : ConnectionState()
        object Disconnected : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    var onCommandReceived: ((json: String) -> Unit)? = null

    private var connectionsClient: ConnectionsClient? = null
    private var connectedEndpointId: String? = null
    private var connectedEndpointName: String? = null

    private val discoveredEndpoints = ConcurrentHashMap<String, String>()
    private val pendingEndpointNames = ConcurrentHashMap<String, String>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: $endpointId (${connectionInfo.endpointName})")
            pendingEndpointNames[endpointId] = connectionInfo.endpointName
            connectionsClient?.acceptConnection(endpointId, payloadCallback)
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to accept connection with $endpointId", e)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                val name = pendingEndpointNames[endpointId] ?: "Classroom Device"
                connectedEndpointId = endpointId
                connectedEndpointName = name
                Log.d(TAG, "Connected to $name ($endpointId)")
                _connectionState.value = ConnectionState.Connected(endpointId, name)
            } else {
                Log.w(TAG, "Connection failed to $endpointId: ${resolution.status}")
                pendingEndpointNames.remove(endpointId)
                if (connectedEndpointId == endpointId) {
                    connectedEndpointId = null
                    connectedEndpointName = null
                }
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            pendingEndpointNames.remove(endpointId)
            if (endpointId == connectedEndpointId) {
                connectedEndpointId = null
                connectedEndpointName = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            discoveredEndpoints[endpointId] = info.endpointName
            _connectionState.value = ConnectionState.SessionsFound(
                discoveredEndpoints.map { Pair(it.key, it.value) }
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            discoveredEndpoints.remove(endpointId)
            _connectionState.value = ConnectionState.SessionsFound(
                discoveredEndpoints.map { Pair(it.key, it.value) }
            )
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val json = String(bytes, Charsets.UTF_8)
                    Log.d(TAG, "Payload received from $endpointId: $json")
                    onCommandReceived?.invoke(json)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No action needed for BytesPayload
        }
    }

    fun startAdvertising(sessionName: String, context: Context) {
        val client = Nearby.getConnectionsClient(context.applicationContext).also {
            connectionsClient = it
        }
        _connectionState.value = ConnectionState.Advertising
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        client.startAdvertising(sessionName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                Log.d(TAG, "Advertising started: $sessionName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
                _connectionState.value = ConnectionState.Disconnected
            }
    }

    fun startDiscovery(context: Context) {
        val client = Nearby.getConnectionsClient(context.applicationContext).also {
            connectionsClient = it
        }
        discoveredEndpoints.clear()
        _connectionState.value = ConnectionState.Discovering
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        client.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                Log.d(TAG, "Discovery started for $SERVICE_ID")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
                _connectionState.value = ConnectionState.Disconnected
            }
    }

    fun requestConnection(endpointId: String, context: Context) {
        val client = connectionsClient ?: Nearby.getConnectionsClient(context.applicationContext).also {
            connectionsClient = it
        }
        val localName = Build.MODEL ?: "Folio Reader Remote"
        client.requestConnection(localName, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Connection requested to $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to request connection to $endpointId", e)
                _connectionState.value = ConnectionState.Disconnected
            }
    }

    fun sendCommand(json: String) {
        val endpoint = connectedEndpointId
        if (endpoint == null) {
            Log.w(TAG, "Cannot send command - not connected")
            return
        }
        val client = connectionsClient
        if (client == null) {
            Log.w(TAG, "Cannot send command - ConnectionsClient is null")
            return
        }
        val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
        client.sendPayload(endpoint, payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send payload to $endpoint", e)
            }
    }

    fun stop() {
        try {
            connectionsClient?.stopAdvertising()
            connectionsClient?.stopDiscovery()
            connectionsClient?.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error while stopping NearbyConnectionManager: ${e.message}")
        }
        discoveredEndpoints.clear()
        pendingEndpointNames.clear()
        connectedEndpointId = null
        connectedEndpointName = null
        _connectionState.value = ConnectionState.Idle
    }
}
