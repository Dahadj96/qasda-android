package pro.qasdatrip.app.ui

import pro.qasdatrip.core.Lang

/**
 * Where the website keeps each page, in each language.
 *
 * Three screens used to carry their own copy of this table and two of them
 * had no entry for the contact page, so "Write to us" opened the home page
 * of the site and left the person to find the address themselves.
 */
fun sitePath(page: String, lang: Lang): String {
    val slug = when (page) {
        "how" -> mapOf(Lang.FR to "comment-ca-marche", Lang.AR to "kayf-yaamal", Lang.EN to "how-it-works")
        "about" -> mapOf(Lang.FR to "a-propos", Lang.AR to "man-nahnu", Lang.EN to "about")
        "contact" -> mapOf(Lang.FR to "nous-contacter", Lang.AR to "ittisal", Lang.EN to "contact")
        "faq" -> mapOf(Lang.FR to "faq", Lang.AR to "faq", Lang.EN to "faq")
        else -> mapOf(Lang.FR to "confidentialite", Lang.AR to "khususiya", Lang.EN to "privacy")
    }
    return "/${lang.tag}/${slug[lang]}"
}
