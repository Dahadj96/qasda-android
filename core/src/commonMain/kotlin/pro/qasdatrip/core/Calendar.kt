package pro.qasdatrip.core

import kotlinx.serialization.Serializable

/**
 * What a route costs on the days around the one somebody asked about.
 *
 * The server already builds this - it is the same merge of four sites the
 * search does, run across a week instead of a day. The app's job is only to
 * show it, and to be careful about one thing: every number here is a price a
 * site quoted for that date. A day with no cell is a day nobody quoted, not a
 * day with no flights, and the two must not look the same.
 */
@Serializable
data class CalendarCell(
    val departDate: String,
    val returnDate: String? = null,
    val price: Double? = null,
    val site: String? = null,
    val airline: String? = null,
    val prices: Map<String, Double?> = emptyMap(),
    /** "cheapest" or "dearest" when the server ranked it, otherwise null. */
    val rank: String? = null,
    val selected: Boolean = false,
)

@Serializable
data class FlightCalendar(
    val sites: List<String> = emptyList(),
    val departDates: List<String> = emptyList(),
    val returnDates: List<String> = emptyList(),
    val cells: List<CalendarCell> = emptyList(),
) {
    val roundTrip: Boolean get() = returnDates.isNotEmpty()

    /** The cell for a day, or null when no site quoted that day. */
    fun cell(depart: String, back: String? = null): CalendarCell? =
        cells.firstOrNull { it.departDate == depart && it.returnDate == back }

    /**
     * The cheapest cell that actually carries a price.
     *
     * The server's own "cheapest" rank when it gave one - it saw the whole
     * grid - and otherwise the lowest price present. Days it could not price
     * are skipped rather than counted as free.
     */
    val cheapest: CalendarCell?
        get() = cells.firstOrNull { it.rank == RANK_CHEAPEST && it.price != null }
            ?: cells.filter { (it.price ?: 0.0) > 0.0 }.minByOrNull { it.price!! }

    /** Days with a price, in date order - what a one-way strip shows. */
    val pricedDays: List<CalendarCell>
        get() = cells.filter { it.returnDate == null && (it.price ?: 0.0) > 0.0 }
            .sortedBy { it.departDate }

    companion object {
        const val RANK_CHEAPEST = "cheapest"
        const val RANK_DEAREST = "dearest"
    }
}
