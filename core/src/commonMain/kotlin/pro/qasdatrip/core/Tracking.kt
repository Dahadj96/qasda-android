package pro.qasdatrip.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Watching a route and a date — never a flight.
 *
 * A flight is not a thing that persists: the 14:25 with one stop that a site
 * quoted this morning can be gone, renumbered or repriced by tonight, and a
 * watch attached to it would either fire on nothing or go silent forever.
 * What lasts is "Alger to Paris on the 12th", and that is what gets watched.
 *
 * There are no accounts anywhere in here. An address, a confirmation link,
 * and a signed URL to manage everything afterwards is the whole model.
 */
@Serializable
data class Watch(
    val id: Long,
    val origin: String,
    val destination: String,
    @SerialName("depart_date") val departDate: String,
    @SerialName("return_date") val returnDate: String? = null,
    @SerialName("cabin_class") val cabinClass: String? = null,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    @SerialName("target_price") val targetPrice: Double? = null,
    @SerialName("baseline_price") val baselinePrice: Double? = null,
    @SerialName("last_notified_at") val lastNotifiedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val roundTrip: Boolean get() = !returnDate.isNullOrBlank()

    /** The search this watch is about, so tapping it can re-run it. */
    fun asQuery(): SearchQuery = SearchQuery(
        from = origin,
        to = destination,
        departDate = departDate,
        returnDate = returnDate?.takeIf { it.isNotBlank() },
        adults = adults.coerceAtLeast(1),
        children = children,
        infants = infants,
        cabin = Cabin.entries.firstOrNull { it.wire == cabinClass } ?: Cabin.ECONOMY,
    )
}

/** What the server said when a watch was asked for. */
@Serializable
data class WatchCreated(
    val created: Boolean = false,
    val needsConfirmation: Boolean = true,
)

/**
 * The link that comes in the confirmation email, taken apart.
 *
 * This pair is the only credential in the product. It is stored on the phone
 * so the tracking list has something to ask with, and it is not a login: it
 * grants exactly the alerts belonging to one mailbox and nothing else.
 */
@Serializable
data class ManageKey(val watcherId: Long, val signature: String) {
    companion object {
        /**
         * Pull `w` and `s` out of a manage URL, whatever else is on it.
         *
         * Deliberately tolerant about the rest of the URL and strict about the
         * two values: somebody pasting a link out of a mail client brings
         * along whatever their client did to it.
         */
        fun parse(url: String): ManageKey? {
            val query = url.substringAfter('?', "").substringBefore('#')
            if (query.isEmpty()) return null
            val fields = query.split('&').mapNotNull { pair ->
                val name = pair.substringBefore('=', "")
                val value = pair.substringAfter('=', "")
                if (name.isEmpty() || value.isEmpty()) null else name to value
            }.toMap()
            val id = fields["w"]?.toLongOrNull() ?: return null
            val signature = fields["s"]?.takeIf { it.isNotBlank() } ?: return null
            return ManageKey(id, signature)
        }
    }
}

/**
 * The shape a mail server will accept, checked before we bother it.
 *
 * Deliberately shallow. Anything stricter starts rejecting real addresses,
 * and the only test that actually settles it is whether the confirmation
 * arrives — this exists so the button can stay disabled over an obvious
 * typo, not to adjudicate RFC 5322.
 */
fun looksLikeEmailShape(value: String): Boolean {
    val text = value.trim()
    if (text.length !in 3..254) return false
    if (text.any { it.isWhitespace() }) return false
    val at = text.indexOf('@')
    if (at <= 0 || at != text.lastIndexOf('@')) return false
    val domain = text.substring(at + 1)
    val dot = domain.lastIndexOf('.')
    return dot > 0 && dot < domain.length - 2
}

/** One observed day. A day nobody searched is absent, never zero. */
@Serializable
data class TrendPoint(
    val date: String,
    val cheapest: Double,
    val observations: Int = 0,
)

/**
 * What a route and date has cost, as we saw it.
 *
 * `observedDays` is not decoration. Low, high and average are computed over
 * the days that have a price, so three points across a fortnight produce an
 * average of three days; a screen that does not say so is claiming a
 * fortnight of evidence it does not have.
 */
@Serializable
data class PriceTrend(
    val origin: String = "",
    val destination: String = "",
    val departDate: String = "",
    val currency: String = "DZD",
    val days: Int = 30,
    val points: List<TrendPoint> = emptyList(),
    val low: Double? = null,
    val high: Double? = null,
    val average: Double? = null,
    val latest: Double? = null,
    val observedDays: Int = 0,
) {
    /** Too little to draw. Two points is a line between two dots, not a trend. */
    val worthDrawing: Boolean get() = points.size >= 3

    /**
     * Where a price sits between the cheapest and dearest day we saw, 0..1.
     * Null when every observed day cost the same, because then there is no
     * scale to place it on.
     */
    fun position(price: Double): Float? {
        val bottom = low ?: return null
        val top = high ?: return null
        if (top <= bottom) return null
        return ((price - bottom) / (top - bottom)).toFloat().coerceIn(0f, 1f)
    }
}
