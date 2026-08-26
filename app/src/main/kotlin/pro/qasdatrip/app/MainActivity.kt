package pro.qasdatrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import pro.qasdatrip.app.ui.QasdaNavHost
import pro.qasdatrip.app.ui.theme.QasdaTheme
import pro.qasdatrip.core.Lang

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The phone's own language decides, as it does on the site. Arabic
            // mirrors the whole layout rather than a hand-written RTL sheet.
            var lang by remember { mutableStateOf(Lang.of(resources.configuration.locales[0].language)) }
            QasdaTheme(lang) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    QasdaNavHost(
                        api = (application as QasdaApplication).api,
                        lang = lang,
                        onLang = { lang = it },
                    )
                }
            }
        }
    }
}
