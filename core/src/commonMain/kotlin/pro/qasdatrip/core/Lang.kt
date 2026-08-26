package pro.qasdatrip.core

enum class Lang(val tag: String, val rtl: Boolean) {
    FR("fr", false), AR("ar", true), EN("en", false);

    companion object {
        fun of(tag: String?): Lang = entries.firstOrNull { it.tag == tag?.take(2)?.lowercase() } ?: FR
    }
}
