package pro.qasdatrip.core

/**
 * The arrow between two cities, pointing the way the language reads.
 *
 * Bidi reorders the words around it but never turns the glyph around, so a
 * literal "→" in an Arabic string points back at where the journey started.
 */
fun routeArrow(lang: Lang): String = if (lang.rtl) "←" else "→"

enum class Lang(val tag: String, val rtl: Boolean) {
    FR("fr", false), AR("ar", true), EN("en", false);

    companion object {
        fun of(tag: String?): Lang = entries.firstOrNull { it.tag == tag?.take(2)?.lowercase() } ?: FR
    }
}
