package ch.tichu.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.core.ui.theme.TichuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var observePreferences: ObservePreferencesUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by observePreferences().collectAsStateWithLifecycle(initialValue = null)
            TichuTheme(themeMode = preferences?.themeMode ?: if (isSystemInDarkTheme()) ch.tichu.counter.core.model.ThemeMode.DARK else ch.tichu.counter.core.model.ThemeMode.LIGHT) {
                TichuApp()
            }
        }
    }
}
