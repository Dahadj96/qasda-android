package pro.qasdatrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import pro.qasdatrip.app.data.Settings
import pro.qasdatrip.app.ui.QasdaNavHost
import pro.qasdatrip.app.ui.theme.QasdaTheme
import pro.qasdatrip.core.Lang

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = Settings(this)
        setContent {
            // The phone's language decides until somebody says otherwise, as
            // on the site. Their choice then outlives the app being closed,
            // and null still means "follow the phone" rather than "French".
            var chosen by remember { mutableStateOf(settings.language) }
            val lang = chosen ?: Lang.of(resources.configuration.locales[0].language)

            QasdaTheme(lang) {
                // Arabic mirrors the whole layout rather than a hand-written
                // RTL sheet, which is why this wraps everything.
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    QasdaNavHost(
                        api = (application as QasdaApplication).api,
                        lang = lang,
                        chosenLang = chosen,
                        onLang = { picked ->
                            settings.language = picked
                            chosen = picked
                        },
                    )
                }
            }
        }
    }
}
