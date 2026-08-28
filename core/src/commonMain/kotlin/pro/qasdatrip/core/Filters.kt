package pro.qasdatrip.core

/**
 * Narrowing a list of flights, and ordering what is left.
 *
 * This lives in the shared module because it is not a screen: it is the rule
 * about which flights answer the question somebody asked, and iOS will need
 * the same rule to give the same answer.
 *
 * One principle runs through all of it, and it is the reason for the nulls:
 *
 *   **A filter removes a flight only when the data says to. It never removes
 *   one because the data was missing.**
 *
 * The sites do not all send the same fields. If a flight arrives with no
 * departure time we cannot tell whether it leaves in the morning - and
 * quietly dropping it would mean showing somebody a shorter list and calling
 * it "flights in the morning", which is a claim we cannot support. So an
 * unknown always survives the filter. It sorts to the end, where being last
 * is visible, rather than disappearing, where being absent is not.
 */

enum class SortBy { PRICE, DEPARTURE, DURATION }

/** Rough parts of the day, in local clock time as the sites report it. */
enum class TimeBand(val fromHour: Int, val untilHour: Int) {
    MORNING(5, 12),
    AFTERNOON(12, 18),
    EVENING(18, 23),
    /** Wraps midnight, and is the only band that does. */
    NIGHT(23, 5);

    fun contains(minutes: Int): Boolean {
        val hour = minutes / 60
        return if (fromHour <= untilHour) hour in fromHour until untilHour
        else hour >= fromHour || hour < untilHour
    }
}

data class Filters(
    /** null means any number of stops. 0 is direct only. */
    val maxStops: Int? = null,
    val bagOnly: Boolean = false,
    val departBands: Set<TimeBand> = emptySet(),
    val maxPrice: Double? = null,
) {
    val active: Int
        get() = listOf(
            maxStops != null,
            bagOnly,
            departBands.isNotEmpty(),
            maxPrice != null,
        ).count { it }

    val isEmpty: Boolean get() = active == 0
}

object FlightList {

    fun apply(flights: List<Flight>, filters: Filters, sort: SortBy): List<Flight> =
        flights.filter { keep(it, filters) }.sortedWith(order(sort))

    private fun keep(flight: Flight, f: Filters): Boolean {
        if (f.bagOnly && !flight.hasLuggage) return false

        val price = flight.cheapest?.second
        if (f.maxPrice != null && price != null && price > f.maxPrice) return false

        val stops = flight.outbound?.stops ?: flight.stops
        if (f.maxStops != null && stops != null && stops > f.maxStops) return false

        if (f.departBands.isNotEmpty()) {
            val minutes = clockMinutes(flight.outboundOrSelf?.departure)
            // Unknown time: keep it. See the note at the top of this file.
            if (minutes != null && f.departBands.none { it.contains(minutes) }) return false
        }
        return true
    }

    private fun order(sort: SortBy): Comparator<Flight> = when (sort) {
        SortBy.PRICE -> Comparator { a, b -> unknownLast(a.cheapest?.second, b.cheapest?.second) }
        SortBy.DEPARTURE -> Comparator { a, b ->
            unknownLast(clockMinutes(a.outboundOrSelf?.departure), clockMinutes(b.outboundOrSelf?.departure))
        }
        SortBy.DURATION -> Comparator { a, b ->
            unknownLast(durationMinutes(a.outboundOrSelf?.duration), durationMinutes(b.outboundOrSelf?.duration))
        }
    }

    /** What we could not read goes last, where it is still visible. */
    private fun <T : Comparable<T>> unknownLast(a: T?, b: T?): Int = when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        else -> a.compareTo(b)
    }

    /**
     * The first clock time in a string, as minutes past midnight.
     *
     * The sites send this in more than one shape - a bare "07:30", sometimes a
     * whole timestamp - so this looks for the first HH:MM rather than trusting
     * a format none of them agreed on.
     */
    fun clockMinutes(text: String?): Int? {
        val s = text ?: return null
        var i = 0
        while (i < s.length) {
            if (s[i].isDigit()) {
                var j = i
                while (j < s.length && s[j].isDigit()) j++
                val hourDigits = j - i
                if (hourDigits in 1..2 && j < s.length && s[j] == ':') {
                    val hour = s.substring(i, j).toInt()
                    var k = j + 1
                    while (k < s.length && s[k].isDigit()) k++
                    if (k - (j + 1) == 2) {
                        val minute = s.substring(j + 1, k).toInt()
                        if (hour in 0..23 && minute in 0..59) return hour * 60 + minute
                    }
                }
                i = j
            } else {
                i++
            }
        }
        return null
    }

    /**
     * A duration in minutes, from any of the shapes the sites use: "3h15",
     * "3 h 15", "PT3H15M", "1h", or a plain number of minutes.
     */
    fun durationMinutes(text: String?): Int? {
        val s = text?.trim()?.uppercase() ?: return null
        if (s.isEmpty()) return null

        s.toIntOrNull()?.let { return it.takeIf { m -> m >= 0 } }

        var hours: Int? = null
        var minutes: Int? = null
        var number = StringBuilder()
        for (c in s) {
            when {
                c.isDigit() -> number.append(c)
                c == 'H' && number.isNotEmpty() -> { hours = number.toString().toIntOrNull(); number = StringBuilder() }
                c == 'M' && number.isNotEmpty() -> { minutes = number.toString().toIntOrNull(); number = StringBuilder() }
                else -> Unit
            }
        }
        // A trailing number with no unit is the minutes half of "3h15".
        if (number.isNotEmpty() && minutes == null && hours != null) minutes = number.toString().toIntOrNull()

        if (hours == null && minutes == null) return null
        return (hours ?: 0) * 60 + (minutes ?: 0)
    }
}
