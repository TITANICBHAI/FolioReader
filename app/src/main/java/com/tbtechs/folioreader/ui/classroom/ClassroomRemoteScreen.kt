package com.tbtechs.folioreader.ui.classroom

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tbtechs.folioreader.domain.model.PdfWord

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClassroomRemoteScreen(
    pdfWords: List<PdfWord> = emptyList(),
    onNavigateBack: () -> Unit = {},
    viewModel: ClassroomRemoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentWord by viewModel.currentWord.collectAsStateWithLifecycle()
    val currentWordIndex by viewModel.currentWordIndex.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWords.collectAsStateWithLifecycle()
    val isVoiceOn by viewModel.isVoiceOn.collectAsStateWithLifecycle()
    val connectedSessionName by viewModel.connectedSessionName.collectAsStateWithLifecycle()

    val nearbyPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }
    val nearbyPermissionState = rememberMultiplePermissionsState(permissions = nearbyPermissions)

    val voicePermissions = remember {
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    val voicePermissionState = rememberMultiplePermissionsState(permissions = voicePermissions)

    LaunchedEffect(pdfWords) {
        if (pdfWords.isNotEmpty()) {
            viewModel.loadPdfWords(pdfWords)
        }
    }

    LaunchedEffect(nearbyPermissionState.allPermissionsGranted) {
        if (nearbyPermissionState.allPermissionsGranted) {
            viewModel.startDiscovery(context)
        } else {
            nearbyPermissionState.launchMultiplePermissionRequest()
        }
    }

    val isVoiceAvailable = remember(context) { viewModel.isVoiceAvailable(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is RemoteUiState.Connected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = connectedSessionName.ifBlank { "Connected" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text("Classroom Remote")
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("classroom_remote_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is RemoteUiState.Connected) {
                        IconButton(
                            onClick = { viewModel.disconnect(context) },
                            modifier = Modifier.testTag("classroom_remote_disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is RemoteUiState.Searching -> {
                    if (!nearbyPermissionState.allPermissionsGranted) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Classroom Remote requires nearby permissions to discover classroom sessions.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { nearbyPermissionState.launchMultiplePermissionRequest() },
                                modifier = Modifier.testTag("grant_nearby_permissions_button")
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("searching_indicator"),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching for classroom sessions…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is RemoteUiState.SessionsFound -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Found sessions:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.sessions, key = { it.first }) { (endpointId, name) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("session_card_$endpointId"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = { viewModel.connect(endpointId, context) },
                                            modifier = Modifier.testTag("connect_button_$endpointId")
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is RemoteUiState.Connecting -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("connecting_indicator"),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is RemoteUiState.Connected -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Centre: currentWord at 32sp bold centred (what is on TV right now)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentWord.ifBlank { "Ready" },
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .testTag("classroom_remote_current_word")
                                )
                                if (currentWordIndex != null && totalWords > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Word ${currentWordIndex!! + 1} of $totalWords",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Bottom controls container (~45% of screen height)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large filled blue Button "▶  NEXT"
                            Button(
                                onClick = { viewModel.sendNext() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(80.dp)
                                    .testTag("classroom_remote_next_button")
                            ) {
                                Text(
                                    text = "▶  NEXT",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Row below NEXT: OutlinedButton "◀ BACK" (left) + IconToggleButton for voice (right)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.sendBack() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("classroom_remote_back_word_button")
                                ) {
                                    Text(
                                        text = "◀ BACK",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconToggleButton(
                                    checked = isVoiceOn,
                                    onCheckedChange = {
                                        if (!isVoiceOn) {
                                            if (voicePermissionState.allPermissionsGranted) {
                                                viewModel.toggleVoice(context)
                                            } else {
                                                voicePermissionState.launchMultiplePermissionRequest()
                                            }
                                        } else {
                                            viewModel.toggleVoice(context)
                                        }
                                    },
                                    enabled = isVoiceAvailable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isVoiceOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(
                                            color = if (isVoiceOn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .testTag("classroom_remote_voice_toggle")
                                ) {
                                    Text(
                                        text = if (isVoiceOn) "🎤 Voice ON" else "🎤 Voice OFF",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isVoiceOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!isVoiceAvailable) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Voice needs internet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.testTag("voice_offline_hint")
                                )
                            }

                            if (!voicePermissionState.allPermissionsGranted && voicePermissionState.shouldShowRationale) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Microphone & location permissions required for voice.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
