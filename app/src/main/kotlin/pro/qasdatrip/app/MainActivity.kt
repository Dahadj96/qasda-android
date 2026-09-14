package pro.qasdatrip.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import pro.qasdatrip.app.data.Settings
import pro.qasdatrip.app.data.onlineFlow
import pro.qasdatrip.app.ui.AlertPrefs
import pro.qasdatrip.app.ui.LocalHaptics
import pro.qasdatrip.app.ui.LocalOnline
import pro.qasdatrip.app.ui.rememberHaptics
import pro.qasdatrip.app.ui.QasdaNavHost
import pro.qasdatrip.app.ui.theme.LocalDark
import pro.qasdatrip.app.ui.theme.QasdaTheme
import pro.qasdatrip.app.ui.theme.ThemeMode
import pro.qasdatrip.core.Lang

/** How long the mark stays up on a start that is faster than the eye. */
private const val SPLASH_MS = 850L

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super, always: the platform reads the splash theme while the
        // activity window is being made, and installing it afterwards is a
        // frame too late.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold it briefly.
        //
        // Left alone, the splash vanishes the instant the first frame is
        // ready, which on a warm start is quick enough to look like a flicker
        // - the mark appears and is gone before the eye lands on it, which
        // reads as a glitch rather than as a launch. A short floor makes it a
        // deliberate moment. It is a floor, not a delay: a cold start that
        // takes longer than this is not slowed down by a single millisecond.
        val started = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - started < SPLASH_MS
        }

        // And leave gracefully. The default is a hard cut to the app; this
        // lifts the mark slightly and fades it out over the home screen, so
        // the two are one movement instead of two pictures.
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f),
            ).apply {
                duration = 260L
                interpolator = DecelerateInterpolator()
                doOnEnd { provider.remove() }
            }.start()
        }

        enableEdgeToEdge()
        val settings = Settings(this)
        // FCM tokens exist independently of the Android 13 notification
        // permission. Registering now means granting the permission while
        // creating a watch makes that watch reachable immediately.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNotBlank()) {
                settings.pushToken = token
                lifecycleScope.launch {
                    (application as QasdaApplication).api
                        .registerDevice(settings.deviceKey, token, locale = settings.language?.tag ?: "fr")
                        ?.let { settings.deviceKey = it }
                }
            }
        }
        setContent {
            // The phone's language decides until somebody says otherwise, as
            // on the site. Their choice then outlives the app being closed,
            // and null still means "follow the phone" rather than "French".
            var chosen by remember { mutableStateOf(settings.language) }
            var recent by remember { mutableStateOf(settings.recent) }
            var manageKey by remember { mutableStateOf(settings.manageKey) }
            var homeAirport by remember { mutableStateOf(settings.homeAirport) }
            var alertsSeenAt by remember { mutableStateOf(settings.alertsSeenAt) }
            var themeMode by remember { mutableStateOf(ThemeMode.of(settings.themeMode)) }
            var alertPrefs by remember {
                mutableStateOf(
                    AlertPrefs(settings.alertDrops, settings.alertSeats, settings.alertEnded),
                )
            }
            val lang = chosen ?: Lang.of(resources.configuration.locales[0].language)

            // True until the first reading arrives: a screen drawn before the
            // system has answered should not accuse anybody of being offline.
            val online by remember { onlineFlow() }.collectAsStateWithLifecycle(initialValue = true)

            QasdaTheme(lang, themeMode) {
                // The status and navigation bar icons are the system's, not
                // ours, and they only have two settings: dark glyphs or light
                // ones. Left alone they stay dark, which on a near-black
                // canvas is a black clock on a black bar.
                SystemBarIcons(dark = LocalDark.current)
                // Arabic mirrors the whole layout rather than a hand-written
                // RTL sheet, which is why this wraps everything.
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalOnline provides online,
                    LocalHaptics provides rememberHaptics(),
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
                        // Read on demand rather than hoisted into state: the
                        // key is written once, from a background coroutine,
                        // and nothing on screen re-draws when it changes.
                        readDevice = { settings.deviceKey },
                        onDeviceKey = { settings.deviceKey = it },
                        homeAirport = homeAirport,
                        onHomeAirport = { iata ->
                            settings.homeAirport = iata
                            homeAirport = iata
                        },
                        alertPrefs = alertPrefs,
                        onAlertPrefs = { next ->
                            settings.alertDrops = next.drops
                            settings.alertSeats = next.seats
                            settings.alertEnded = next.ended
                            alertPrefs = next
                        },
                        onClearRecent = {
                            settings.recent = emptyList()
                            recent = emptyList()
                        },
                        themeMode = themeMode,
                        onThemeMode = { picked ->
                            settings.themeMode = picked.tag
                            themeMode = picked
                        },
                        alertsSeenAt = alertsSeenAt,
                        onAlertsSeen = {
                            val now = System.currentTimeMillis()
                            settings.alertsSeenAt = now
                            alertsSeenAt = now
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

/**
 * Dark glyphs on a light app, light glyphs on a dark one.
 *
 * Under edge-to-edge the app paints behind both system bars, so the clock and
 * the gesture pill are drawn on top of whatever the app has put there. This is
 * the only control there is over them, and it has to move with the theme
 * rather than being set once at startup.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    // Keyed on the theme rather than run after every composition. As a
    // SideEffect this fought the home screen, which asks for light glyphs in
    // both themes because its photograph is dark under the clock: every
    // keystroke elsewhere in the tree re-ran this and took them back.
    LaunchedEffect(dark) {
        val window = (view.context as android.app.Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
