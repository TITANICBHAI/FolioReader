package com.tbtechs.folioreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbtechs.folioreader.data.preferences.ReaderPreferencesRepository
import com.tbtechs.folioreader.ui.FolioReaderMainScreen
import com.tbtechs.folioreader.ui.theme.FolioReaderTheme
import com.tbtechs.folioreader.util.ScreenOrientationMode
import com.tbtechs.folioreader.util.applyOrientation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: ReaderPreferencesRepository

    private var pendingPdfUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingPdfUri = extractPdfUri(intent)

        setContent {
            val darkMode by preferencesRepository.darkModePreference.collectAsStateWithLifecycle(initialValue = "system")
            val orientationPref by preferencesRepository.screenOrientationPreference.collectAsStateWithLifecycle(initialValue = "sensor")

            // Keep AppCompatDelegate in sync
            when (darkMode.lowercase()) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            // Keep Activity screen orientation in sync with preference
            LaunchedEffect(orientationPref) {
                applyOrientation(ScreenOrientationMode.fromKey(orientationPref))
            }

            FolioReaderTheme(darkMode = darkMode) {
                FolioReaderMainScreen(
                    pendingPdfUri = pendingPdfUri,
                    onPdfHandled = { pendingPdfUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPdfUri = extractPdfUri(intent)
    }

    private fun extractPdfUri(intent: Intent?): Uri? {
        if (intent == null) return null
        val action = intent.action
        if (action == Intent.ACTION_VIEW) {
            return intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        } else if (action == Intent.ACTION_SEND) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            return streamUri ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri ?: intent.data
        }
        return null
    }
}
