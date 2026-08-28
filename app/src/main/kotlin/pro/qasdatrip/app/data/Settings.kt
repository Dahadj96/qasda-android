package pro.qasdatrip.app.data

import android.content.Context
import pro.qasdatrip.core.Lang

/**
 * The handful of choices that outlive one run of the app.
 *
 * SharedPreferences rather than DataStore, deliberately: the language has to
 * be known before the first frame is composed, and an asynchronous read means
 * the app paints in French and then flips to Arabic in front of somebody who
 * chose Arabic. A blocking read of one string at startup is the cheaper
 * mistake.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("qasda", Context.MODE_PRIVATE)

    /**
     * The chosen language, or null for "whatever the phone is set to".
     *
     * Null is a real answer rather than a missing one: somebody who has never
     * touched this should keep following their phone when they change its
     * language, and writing FR at first launch would quietly stop that.
     */
    var language: Lang?
        get() = prefs.getString(KEY_LANG, null)?.let { tag -> Lang.entries.firstOrNull { it.tag == tag } }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_LANG) else putString(KEY_LANG, value.tag)
            }.apply()
        }

    private companion object {
        const val KEY_LANG = "lang"
    }
}
