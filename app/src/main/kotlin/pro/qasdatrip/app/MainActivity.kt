package pro.qasdatrip.app

import android.content.Intent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pro.qasdatrip.app.data.Settings
import pro.qasdatrip.app.data.onlineFlow
import pro.qasdatrip.app.ui.LocalOnline
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
            var recent by remember { mutableStateOf(settings.recent) }
            var manageKey by remember { mutableStateOf(settings.manageKey) }
            val lang = chosen ?: Lang.of(resources.configuration.locales[0].language)

            // True until the first reading arrives: a screen drawn before the
            // system has answered should not accuse anybody of being offline.
            val online by remember { onlineFlow() }.collectAsStateWithLifecycle(initialValue = true)

            QasdaTheme(lang) {
                // Arabic mirrors the whole layout rather than a hand-written
                // RTL sheet, which is why this wraps everything.
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalOnline provides online,
                ) {
                    QasdaNavHost(
                        api = (application as QasdaApplication).api,
                        lang = lang,
                        chosenLang = chosen,
                        onLang = { picked ->
                            settings.language = picked
                            chosen = picked
                        },
                        recent = recent,
                        onRemember = { query ->
                            settings.remember(query)
                            recent = settings.recent
                        },
                        manageKey = manageKey,
                        onManageKey = { key ->
                            settings.manageKey = key
                            manageKey = key
                        },
                    )
                }
            }
        }
    }

    /**
     * A second link, while we are already running.
     *
     * singleTask keeps one instance of this activity, so a link tapped in a
     * second email arrives here rather than through onCreate. Navigation's
     * deep-link handling reads the activity's current intent, so replacing
     * it is what makes the new link land.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
