package pro.qasdatrip.core

/**
 * A price, written the way each language writes one.
 *
 * The digits are always Latin and always left-to-right; what changes is which
 * side of them the currency sits on. In Arabic it reads number first, so the
 * دج ends up to the left of it — the opposite of what happens if the whole
 * run is forced left-to-right, which is the bug this exists to avoid.
 */
object Money {
    private const val LRI = '⁦'   // left-to-right isolate
    private const val PDI = '⁩'   // pop directional isolate

    fun format(amount: Double, lang: Lang): String {
        val digits = isolate(group(amount))
        val currency = when (lang) { Lang.AR -> "دج"; else -> "DZD" }
        return "$digits $currency"
    }

    /** Just the number, isolated, for places that print the currency themselves. */
    fun amount(amount: Double): String = isolate(group(amount))

    /** Wrap a digit run so the surrounding Arabic cannot reorder it. */
    fun isolate(text: String): String = "$LRI$text$PDI"

    private fun group(amount: Double): String {
        val whole = amount.toLong()
        val s = whole.toString()
        val out = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) out.append(' ')   // narrow no-break space
            out.append(c)
        }
        return out.toString()
    }
}

/**
 * Seats, and the one rule about them: suppliers cap the number they report at
 * nine, so nine means "at least nine" and is never worth saying. Below five it
 * is a real count and worth knowing; null is "nobody said", which is not the
 * same as none.
 */
object Seats {
    private const val WORTH_SAYING = 4

    fun left(count: Int?): Int? = count?.takeIf { it in 1..WORTH_SAYING }
}
