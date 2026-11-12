package app.inkling

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.inkling.data.DemoSeed
import app.inkling.data.Settings
import app.inkling.domain.Lang
import app.inkling.notify.Evening
import app.inkling.ui.Onboarding
import app.inkling.ui.Root
import app.inkling.ui.Start
import app.inkling.ui.theme.InkTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        if (BuildConfig.DEMO) {
            container.scope.launch { DemoSeed.seedIfEmpty(container) }
            Start.tab = intent.getIntExtra("tab", 0); Start.open = intent.getStringExtra("open")
        }
        // a notification asked for a place: the candle or the sphinx
        when (intent.getStringExtra("go")) { "candle" -> Start.open = "candle"; "quiz" -> Start.open = "quiz" }
        Evening.schedule(this)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val sys = isSystemInDarkTheme()
            val dark = when (settings?.theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t), navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t))
            }
            InkTheme(dark) {
                settings?.let { s -> if (s.onboarded) Root(s) else Onboarding() }
            }
        }
    }
}

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. The activity restarts in the new language. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
