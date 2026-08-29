package pro.qasdatrip.app.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.ManageKey
import pro.qasdatrip.core.SearchQuery

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

    /**
     * The last few searches, newest first.
     *
     * The question is kept, never the answer: a price from last week is not a
     * price, and showing one next to a route would be a quote we cannot stand
     * behind. Re-running the search is the only way to say what it costs now.
     */
    var recent: List<SearchQuery>
        get() = prefs.getString(KEY_RECENT, null)
            ?.let { runCatching { json.decodeFromString(SEARCHES, it) }.getOrNull() }
            .orEmpty()
        set(value) {
            prefs.edit().putString(KEY_RECENT, json.encodeToString(SEARCHES, value)).apply()
        }

    /**
     * The signed link out of the confirmation email, if one has arrived here.
     *
     * The only credential in the product, and not a login: it grants exactly
     * the alerts belonging to one mailbox. The address itself is deliberately
     * not stored — the phone has no use for it, and a mailbox address sitting
     * in a preferences file is a liability with no matching benefit.
     */
    var manageKey: ManageKey?
        get() {
            val id = prefs.getLong(KEY_WATCHER, -1L).takeIf { it >= 0 } ?: return null
            val signature = prefs.getString(KEY_SIGNATURE, null) ?: return null
            return ManageKey(id, signature)
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_WATCHER); remove(KEY_SIGNATURE)
                } else {
                    putLong(KEY_WATCHER, value.watcherId)
                    putString(KEY_SIGNATURE, value.signature)
                }
            }.apply()
        }

    /** Newest first, no duplicates, and never more than [KEEP]. */
    fun remember(query: SearchQuery) {
        recent = (listOf(query) + recent.filterNot { it.sameTrip(query) }).take(KEEP)
    }

    /**
     * Two searches are the same shortcut when they ask about the same trip.
     * Somebody searching Alger → Paris again with one more passenger does not
     * want two rows that differ by a number they cannot see.
     */
    private fun SearchQuery.sameTrip(other: SearchQuery): Boolean =
        from == other.from && to == other.to &&
            departDate == other.departDate && returnDate == other.returnDate

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_RECENT = "recent_searches"
        const val KEY_WATCHER = "alert_watcher_id"
        const val KEY_SIGNATURE = "alert_signature"
        const val KEEP = 5

        val json = Json { ignoreUnknownKeys = true }
        val SEARCHES = ListSerializer(SearchQuery.serializer())
    }
}
