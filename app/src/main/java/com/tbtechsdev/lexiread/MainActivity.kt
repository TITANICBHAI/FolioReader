package com.tbtechsdev.lexiread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbtechsdev.lexiread.data.preferences.ReaderPreferencesRepository
import com.tbtechsdev.lexiread.ui.LexiReadMainScreen
import com.tbtechsdev.lexiread.ui.theme.LexiReadTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: ReaderPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by preferencesRepository.darkModePreference.collectAsStateWithLifecycle(initialValue = "system")

            // Keep AppCompatDelegate in sync
            when (darkMode.lowercase()) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            LexiReadTheme(darkMode = darkMode) {
                LexiReadMainScreen()
            }
        }
    }
}
