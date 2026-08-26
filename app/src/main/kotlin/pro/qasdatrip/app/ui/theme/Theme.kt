package pro.qasdatrip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The same tokens as public/style.css, by the same names. The site and the app
 * are one product; a colour that exists in one and not the other is a bug.
 */
object Ink {
    val canvas = Color(0xFFFAF9F6)
    val surface = Color(0xFFFFFFFF)
    val surfaceSoft = Color(0xFFF2F3EF)
    val ink = Color(0xFF232522)
    val inkSoft = Color(0xFF505650)
    val muted = Color(0xFF6D746E)
    val inverse = Color(0xFFFFFFFF)
    val line = Color(0xFFDEDFD9)
    val lineStrong = Color(0xFFC8CBC4)
    val accent = Color(0xFF11B99A)
    val accentUi = Color(0xFF0D9880)
    val accentDeep = Color(0xFF08745F)
    val accentSoft = Color(0xFFE5F7F2)
    val alert = Color(0xFFA8443D)
    val alertSoft = Color(0xFFFAEEEB)
    val notice = Color(0xFF8F6412)
    val noticeSoft = Color(0xFFFBF1DE)
}

private val LightColors = lightColorScheme(
    primary = Ink.ink,
    onPrimary = Ink.inverse,
    secondary = Ink.accentDeep,
    onSecondary = Ink.inverse,
    background = Ink.canvas,
    onBackground = Ink.ink,
    surface = Ink.surface,
    onSurface = Ink.ink,
    surfaceVariant = Ink.surfaceSoft,
    onSurfaceVariant = Ink.inkSoft,
    outline = Ink.line,
    error = Ink.alert,
)

// Dark comes later, deliberately: the brand is a paper-and-ink palette and a
// naive inversion of it looks like a different product. Until it is designed,
// the app stays light so nothing is unreadable.
private val DarkColors = LightColors

val Radius = object {
    val sm = 8.dp
    val md = 14.dp
    val lg = 20.dp
    val pill = 999.dp
}

val Space = object {
    val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp
    val s5 = 20.dp; val s6 = 24.dp; val s8 = 32.dp; val s10 = 40.dp
}

private val QasdaTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
)

val LocalWords = staticCompositionLocalOf { pro.qasdatrip.core.Words.of(pro.qasdatrip.core.Lang.FR) }
val LocalLang = staticCompositionLocalOf { pro.qasdatrip.core.Lang.FR }

@Composable
fun QasdaTheme(
    lang: pro.qasdatrip.core.Lang,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLang provides lang,
        LocalWords provides pro.qasdatrip.core.Words.of(lang),
    ) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
            typography = QasdaTypography,
            content = content,
        )
    }
}
