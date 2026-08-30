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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.qasdatrip.app.R

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
    /**
     * The hairline between the two times on a flight card.
     *
     * Darker than `line`, which is for borders: this one has to read as a
     * route across white with an aircraft sitting on it, and at `line` it
     * disappeared into the card. Needs adding to public/style.css so the site
     * and the app keep agreeing.
     */
    val rail = Color(0xFF868D85)
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

// Named objects, not `val Radius = object { ... }`: the type of an anonymous
// object only survives on a private or local declaration. On a public top-level
// val Kotlin infers the supertype instead - Any - and every Radius.sm below
// stops resolving. That is why the app module had never compiled.
object Radius {
    /** The small label inside a card. */
    val tag = 6.dp
    /** A filter chip, and a text field. */
    val sm = 8.dp
    /** A card. */
    val md = 16.dp
    val lg = 20.dp
    val pill = 999.dp
}

object Space {
    val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp
    val s5 = 20.dp; val s6 = 24.dp; val s8 = 32.dp; val s10 = 40.dp
}

/**
 * Manrope, the same face the design is drawn in.
 *
 * One variable file rather than five static ones: 165 KB against roughly
 * 400 KB, and every weight the design uses is a coordinate on the same axis.
 *
 * Each weight is a font-family resource pinning that coordinate, not a
 * `variationSettings` argument here. The argument is honoured on the
 * emulator and ignored on HyperOS, where every weight came back as the
 * file's default instance — a screen built from four weights rendering in
 * one, and looking lighter than the design everywhere. The XML path goes
 * through the framework's own resource loader, which applies the axis when
 * it creates the typeface.
 *
 * Without any of this the app rendered in whatever the phone's default
 * happened to be — Roboto on stock, MiSans on the Xiaomi — which is most of
 * why screens built to the right measurements still did not look like the
 * design.
 */
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/**
 * Manrope carries no Arabic, and a missing glyph falls back to whatever the
 * phone has — which is a different face mid-sentence. Cairo is the Arabic
 * companion the site and the Figma file already use, so Arabic gets it
 * outright rather than by accident.
 */
private val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
)

/**
 * The type scale, in the sizes and weights the Figma screens actually use.
 *
 * displaySmall  — the screen title ("Qasda", "Mes suivis")
 * headlineSmall — a card's headline number
 * titleLarge    — the route in an app bar, and the times on a flight card
 * titleMedium   — a field's value, a list row's headline
 * bodyLarge     — running text
 * bodyMedium    — a field's label, a card's secondary line
 * labelLarge    — a chip, a button, a text link
 * labelMedium   — the small grey line under a title
 * labelSmall    — a section heading in small caps, a tag
 */
private fun typographyFor(family: FontFamily, tracking: Boolean) = Typography(
    displaySmall = TextStyle(fontFamily = family, fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = if (tracking) (-0.8).sp else 0.sp),
    headlineSmall = TextStyle(fontFamily = family, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = if (tracking) (-0.4).sp else 0.sp),
    titleLarge = TextStyle(fontFamily = family, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = if (tracking) (-0.2).sp else 0.sp),
    titleMedium = TextStyle(fontFamily = family, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = family, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = family, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = family, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = family, fontSize = 10.sp, lineHeight = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = if (tracking) 0.6.sp else 0.sp),
)

private val LatinType = typographyFor(Manrope, tracking = true)

/**
 * Arabic gets no letter-spacing at all.
 *
 * Latin tightens at display sizes and opens up in small caps; Arabic is
 * cursive, and moving letters apart breaks the joins and the shaping with
 * them. Negative tracking on a 30sp Arabic title measured it wrong enough
 * to break "الإعدادات" across two lines mid-word. Taller line heights for
 * the same reason: Arabic ascenders and descenders need the room.
 */
private val ArabicType = typographyFor(Cairo, tracking = false)

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
            typography = if (lang == pro.qasdatrip.core.Lang.AR) ArabicType else LatinType,
            content = content,
        )
    }
}
