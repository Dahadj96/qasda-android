package pro.qasdatrip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
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
 * The same tokens as public/style.css and the Figma `Color` collection, by the
 * same names. The site, the file and the app are one product; a colour that
 * exists in one and not the others is a bug.
 *
 * A class rather than an object because there are now two of them. Every call
 * site still reads `Ink.muted`, because [Ink] below resolves to whichever
 * palette the surrounding theme provides - which is the whole trick that let
 * dark mode land without touching six hundred call sites.
 */
@Immutable
data class Palette(
    val canvas: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    /**
     * A filled control: the search button, a chosen date, an active chip.
     *
     * Separate from [ink] because the two only agree in light mode. On paper
     * the strongest thing on the screen is near-black, and so is the body
     * text. In the dark they part company: text goes pale and a filled button
     * goes to the accent, because a pale button would be a slab of white on a
     * dark screen.
     */
    val solid: Color,
    /** What is legible on [solid]. */
    val onSolid: Color,
    val line: Color,
    /**
     * The hairline between the two times on a flight card.
     *
     * Darker than [line], which is for borders: this one has to read as a
     * route with an aircraft sitting on it, and at [line] it disappeared into
     * the card.
     */
    val rail: Color,
    val lineStrong: Color,
    val accent: Color,
    val accentUi: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val alert: Color,
    val alertSoft: Color,
    val notice: Color,
    val noticeSoft: Color,
)

val LightPalette = Palette(
    canvas = Color(0xFFFAF9F6),
    surface = Color(0xFFFFFFFF),
    surfaceSoft = Color(0xFFF2F3EF),
    ink = Color(0xFF232522),
    inkSoft = Color(0xFF505650),
    muted = Color(0xFF6D746E),
    solid = Color(0xFF232522),
    onSolid = Color(0xFFFFFFFF),
    line = Color(0xFFDEDFD9),
    rail = Color(0xFF868D85),
    lineStrong = Color(0xFFC8CBC4),
    accent = Color(0xFF11B99A),
    accentUi = Color(0xFF0D9880),
    accentDeep = Color(0xFF08745F),
    accentSoft = Color(0xFFE5F7F2),
    alert = Color(0xFFA8443D),
    alertSoft = Color(0xFFFAEEEB),
    notice = Color(0xFF8F6412),
    noticeSoft = Color(0xFFFBF1DE),
)

/**
 * The dark palette, as drawn on the Figma page "Android · Dark".
 *
 * Not an inversion. The greens are lifted and desaturated so the accent still
 * reads as the brand at low luminance, the paper white becomes a near-black
 * with the same slight green cast the paper had, and the two warning colours
 * are pulled towards their tints rather than their full-strength versions,
 * which glow on a dark ground.
 */
val DarkPalette = Palette(
    canvas = Color(0xFF101312),
    surface = Color(0xFF191D1B),
    surfaceSoft = Color(0xFF232826),
    ink = Color(0xFFECEFEC),
    inkSoft = Color(0xFFB9C0BB),
    muted = Color(0xFFA7AFA8),
    solid = Color(0xFF2BD3B0),
    onSolid = Color(0xFF0B1F1A),
    line = Color(0xFF2C322F),
    rail = Color(0xFF6E766F),
    lineStrong = Color(0xFF3A423E),
    accent = Color(0xFF2BD3B0),
    accentUi = Color(0xFF2BD3B0),
    accentDeep = Color(0xFF7DE9CE),
    accentSoft = Color(0xFF12312B),
    alert = Color(0xFFF08A81),
    alertSoft = Color(0xFF3A211F),
    notice = Color(0xFFE8B65C),
    noticeSoft = Color(0xFF332818),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** True when the app is painting itself dark, whatever the reason. */
val LocalDark = staticCompositionLocalOf { false }

/**
 * The palette in force here.
 *
 * A composable property, so `Ink.muted` keeps working everywhere it already
 * appears while now answering differently in the dark. The one place it
 * cannot be read is a non-composable lambda - a `drawBehind`, a `Canvas`
 * draw scope - where the colour has to be lifted into a local first.
 */
val Ink: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

/** What somebody chose in Réglages. */
enum class ThemeMode { SYSTEM, LIGHT, DARK;
    val tag: String get() = name.lowercase()
    companion object {
        fun of(tag: String?): ThemeMode =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

private fun schemeFor(p: Palette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = p.solid,
        onPrimary = p.onSolid,
        secondary = p.accentDeep,
        onSecondary = p.onSolid,
        background = p.canvas,
        onBackground = p.ink,
        surface = p.surface,
        onSurface = p.ink,
        surfaceVariant = p.surfaceSoft,
        onSurfaceVariant = p.inkSoft,
        // The container roles too. Menus, sheets and dialogs draw on these,
        // and Material's defaults for them are a lilac tint from the
        // baseline purple seed - which is how the sort menu came out mauve
        // on an app with no purple anywhere in it.
        surfaceContainerLowest = p.surface,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceSoft,
        surfaceContainerHighest = p.surfaceSoft,
        surfaceTint = p.surface,
        outline = p.line,
        outlineVariant = p.line,
        error = p.alert,
        onError = p.onSolid,
        tertiary = p.notice,
        onTertiary = p.onSolid,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.accentDeep,
        secondaryContainer = p.accentSoft,
        onSecondaryContainer = p.accentDeep,
    )
} else {
    lightColorScheme(
        primary = p.solid,
        onPrimary = p.onSolid,
        secondary = p.accentDeep,
        onSecondary = p.onSolid,
        background = p.canvas,
        onBackground = p.ink,
        surface = p.surface,
        onSurface = p.ink,
        surfaceVariant = p.surfaceSoft,
        onSurfaceVariant = p.inkSoft,
        // The container roles too. Menus, sheets and dialogs draw on these,
        // and Material's defaults for them are a lilac tint from the
        // baseline purple seed - which is how the sort menu came out mauve
        // on an app with no purple anywhere in it.
        surfaceContainerLowest = p.surface,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceSoft,
        surfaceContainerHighest = p.surfaceSoft,
        surfaceTint = p.surface,
        outline = p.line,
        outlineVariant = p.line,
        error = p.alert,
        onError = p.onSolid,
        tertiary = p.notice,
        onTertiary = p.onSolid,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.accentDeep,
        secondaryContainer = p.accentSoft,
        onSecondaryContainer = p.accentDeep,
    )
}

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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalLang provides lang,
        LocalWords provides pro.qasdatrip.core.Words.of(lang),
        LocalPalette provides palette,
        LocalDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(palette, dark),
            typography = if (lang == pro.qasdatrip.core.Lang.AR) ArabicType else LatinType,
            content = content,
        )
    }
}
