package com.simplecalculatordeign

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.simplecalculatordeign.data.repository.AppSettings
import com.simplecalculatordeign.data.repository.ThemeMode
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.navigation.AppRoot
import com.simplecalculatordeign.ui.theme.SimpleCalculatorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var settings by mutableStateOf<AppSettings?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the system splash until settings (theme, onboarding) are loaded: no flicker.
        splash.setKeepOnScreenCondition { settings == null }

        lifecycleScope.launch {
            appContainer.settingsRepository.settings.collect { settings = it }
        }

        setContent {
            val current = settings
            if (current != null) AppContent(current)
        }
    }

    @androidx.compose.runtime.Composable
    private fun AppContent(current: AppSettings) {
        run {
            val dark = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose { }
            }
            CompositionLocalProvider(LocalAppSettings provides current) {
                SimpleCalculatorTheme(darkTheme = dark) {
                    AppRoot(settings = current)
                }
            }
        }
    }
}
