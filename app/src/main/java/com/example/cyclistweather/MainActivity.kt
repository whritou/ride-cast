package com.example.cyclistweather

import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.cyclistweather.domain.model.ThemeMode
import com.example.cyclistweather.presentation.app.AppViewModelFactory
import com.example.cyclistweather.presentation.app.CyclistWeatherApp
import com.example.cyclistweather.presentation.app.CyclistWeatherViewModel
import com.example.cyclistweather.ui.theme.CyclistWeatherTheme
import java.util.Locale
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    private val cyclistWeatherViewModel: CyclistWeatherViewModel by viewModels {
        AppViewModelFactory((application as CyclistWeatherApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: system bar icon contrast adapts automatically to light/dark content.
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        setContent {
            val state by cyclistWeatherViewModel.uiState.collectAsState()
            val darkTheme = when (state.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Apply the in-app language without recreating the Activity: build a localized
            // resource context and provide it (plus its configuration) so every stringResource
            // and stringResource-backed formatter resolves in the selected language.
            val baseContext = LocalContext.current
            val languageTag = state.language.tag
            val localizedContext = remember(languageTag, baseContext) {
                if (languageTag == null) {
                    baseContext
                } else {
                    // IMPORTANT: keep the Activity as the wrapper's base context. The
                    // ActivityResultRegistry / lifecycle / saved-state owners that Compose APIs
                    // (e.g. rememberLauncherForActivityResult) resolve are found by walking the
                    // ContextWrapper chain up to the Activity. A bare createConfigurationContext()
                    // does not wrap the Activity, which is what previously crashed the app.
                    val config = Configuration(baseContext.resources.configuration)
                    config.setLocale(Locale.forLanguageTag(languageTag))
                    val localizedResources = baseContext.createConfigurationContext(config).resources
                    object : ContextWrapper(baseContext) {
                        override fun getResources(): Resources = localizedResources
                        override fun getAssets(): AssetManager = localizedResources.assets
                    }
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                CyclistWeatherTheme(darkTheme = darkTheme) {
                    CyclistWeatherApp(viewModel = cyclistWeatherViewModel)
                }
            }
        }
        if (savedInstanceState == null) {
            handleIncomingGpxIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingGpxIntent(intent)
    }

    private fun handleIncomingGpxIntent(intent: Intent?) {
        val uri = intent?.gpxUri() ?: return
        cyclistWeatherViewModel.importGpx(uri)
    }

    private fun Intent.gpxUri(): Uri? {
        return when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> sharedStream()
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStream(): Uri? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}
