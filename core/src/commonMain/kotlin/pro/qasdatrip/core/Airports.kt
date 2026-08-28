package pro.qasdatrip.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * An airport somebody here actually flies from or to. 213 of them, shipped
 * with the app: the full 9,000-airport database is a web problem, not a phone
 * one, and everything Algeria flies is in this list.
 */
@Serializable
data class Airport(
    @SerialName("i") val iata: String,
    @SerialName("c") val city: Names,
    @SerialName("n") val name: String = "",
    /** Other names it answers to: the commune the databases file it under,
     *  the person it is named after, the exonym another language uses. */
    @SerialName("a") val alt: List<String> = emptyList(),
) {
    fun cityIn(lang: Lang): String = when (lang) {
        Lang.FR -> city.fr
        Lang.AR -> city.ar
        Lang.EN -> city.en
    }
}

@Serializable
data class Names(val en: String, val fr: String, val ar: String)

object Airports {
    private val json = Json { ignoreUnknownKeys = true }
    private var all: List<Airport> = emptyList()

    /** Called once at startup with the contents of the bundled airports.json. */
    fun load(rawJson: String) {
        all = runCatching { json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Airport.serializer()), rawJson) }
            .getOrElse { emptyList() }
    }

    fun byIata(code: String): Airport? = all.firstOrNull { it.iata.equals(code, ignoreCase = true) }

    /** What to show before anybody types: where people here actually go. */
    fun suggestions(originSide: Boolean): List<Airport> {
        val seeds = if (originSide) listOf("ALG", "ORN", "CZL", "AAE", "TLM", "BJA")
        else listOf("CDG", "MRS", "IST", "BCN", "DXB", "YUL", "LYS", "MAD")
        return seeds.mapNotNull(::byIata)
    }

    /**
     * Search, with both sides folded first — see [fold]. Scored by where the
     * query hits rather than whether it hits at all, so "oran" cannot rank
     * Andovoranto above Oran.
     */
    fun search(query: String, limit: Int = 30): List<Airport> {
        val q = fold(query)
        if (q.isEmpty()) return emptyList()
        return all.asSequence()
            .map { it to score(it, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Airport, Int>> { it.second }.thenBy { it.first.iata })
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(a: Airport, q: String): Int {
        var best = 0
        val cityNames = listOf(a.city.en, a.city.fr, a.city.ar)
        for (candidate in cityNames + a.alt) {
            val alias = if (candidate in cityNames) 0 else 5
            val f = fold(candidate)
            if (f.isEmpty()) continue
            val words = f.split(WORD_SPLIT).filter { it.isNotEmpty() }
            val hit = when {
                f == q -> 95
                words.any { it == q } -> 90
                f.startsWith(q) -> 80
                words.any { it.startsWith(q) } -> 70
                f.contains(q) -> 30
                else -> 0
            }
            if (hit > 0) best = maxOf(best, hit - alias)
        }
        // An exact code is strong evidence, not proof. Three letters that spell
        // a code are just as often the first three letters of a city: "ora" is
        // an airstrip in Argentina and the start of Oran, and somebody typing it
        // into this app means Oran. So an exact code now ranks just BELOW a city
        // that starts with the query instead of ending the contest outright.
        //
        // It still wins when the SAME airport matches both, which is what keeps
        // "mad" on Madrid rather than handing it to Madinah.
        if (a.iata.lowercase() == q) {
            return if (best > 0) maxOf(best, EXACT_CODE) + CODE_AND_CITY else EXACT_CODE
        }

        if (best >= 70) return best
        if (a.iata.lowercase().startsWith(q)) best = maxOf(best, 60)
        val name = fold(a.name)
        if (name.split(WORD_SPLIT).any { it.startsWith(q) }) best = maxOf(best, 45)
        else if (name.contains(q)) best = maxOf(best, 20)
        return best
    }

    /** Just under a city that starts with the query (80). See [score]. */
    private const val EXACT_CODE = 78

    /** Matching the code and the city beats matching either one alone. */
    private const val CODE_AND_CITY = 25

    private val WORD_SPLIT = Regex("[\\s,'’\\-/()]+")
}

/**
 * Two spellings of one name have to meet somewhere.
 *
 * Nobody types the hamza on أدرار, قسنطينة is written with a final ه as often
 * as with ة, and a French keyboard is not why Séville should be unfindable.
 * Latin loses its accents; Arabic loses the marks that carry no meaning here.
 */
fun fold(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input.lowercase()) {
        when (ch) {
            // combining marks, tatweel, superscript alef: gone
            in 'ً'..'ْ', 'ـ', 'ٰ', 'ٓ', 'ٔ', 'ٕ' -> {}
            'آ', 'أ', 'إ', 'ٱ' -> sb.append('ا')  // آ أ إ ٱ → ا
            'ى' -> sb.append('ي')                                 // ى → ي
            'ة' -> sb.append('ه')                                 // ة → ه
            'ؤ' -> sb.append('و')                                 // ؤ → و
            'ئ' -> sb.append('ي')                                 // ئ → ي
            in '٠'..'٩' -> sb.append('0' + (ch - '٠'))       // ٠١٢ → 012
            else -> sb.append(deburr(ch))
        }
    }
    return sb.toString().trim()
}

/** é → e, ü → u, ñ → n. Enough of Latin-1 for the names on these routes. */
private fun deburr(ch: Char): Char = when (ch) {
    in 'à'..'å', 'ā', 'ă', 'ą' -> 'a'
    'ç', 'ć', 'č' -> 'c'
    in 'è'..'ë', 'ē', 'ė', 'ę' -> 'e'
    in 'ì'..'ï', 'ī', 'į' -> 'i'
    'ñ', 'ń' -> 'n'
    in 'ò'..'ö', 'ø', 'ō' -> 'o'
    in 'ù'..'ü', 'ū' -> 'u'
    'ý', 'ÿ' -> 'y'
    'ß' -> 's'
    else -> ch
}
