package pro.qasdatrip.core

/**
 * Two things a leg knows that the server does not say outright.
 *
 * Both exist because of the same discovery, made on a real phone against the
 * live API: for ALG to CDG on 31 August, every single flight came back with
 * `stops` empty. The app read that as zero and labelled a 26-hour itinerary
 * through Tunis "Direct", showing 14:25 to 17:35 as though it were an
 * afternoon hop. It is neither direct nor same-day.
 */

/**
 * How many stops the leg really has.
 *
 * The server's own count when it gives one. Otherwise the segments, which are
 * the itinerary itself: ALG>TUN, TUN>CDG is two flights and therefore one
 * stop, whatever the missing field says.
 *
 * Null means genuinely unknown - no count and no segments - and callers must
 * say nothing rather than guess. "Direct" is a claim, and a claim needs
 * evidence.
 */
val Leg.stopCount: Int?
    get() = stops ?: segments.size.takeIf { it > 0 }?.let { it - 1 }

/**
 * Which day the leg lands on, counted from the day it left. 0 is same day,
 * 1 is the following morning.
 *
 * Nothing in the payload says this: `arrivalDate` comes back empty and the
 * two clock times are local to two different places. But the elapsed duration
 * does say it, once you notice that only one answer is physically possible.
 *
 * Departure + duration lands at some instant; the arrival clock reads that
 * instant in the destination's zone. So
 *
 *     departure + duration + offset = arrival + 1440 * days
 *
 * where `offset` is the difference between the two time zones. Time zones run
 * from about -12h to +14h, so exactly one whole number of days puts `offset`
 * inside that range - and rounding picks it.
 *
 * Worked on the real flight above: 14:25 + 26h10 = 2435 minutes, arrival
 * reads 17:35 = 1055, so (2435 - 1055) / 1440 = 0.96, rounds to 1 day, and
 * the implied offset is +1h - which is exactly Algiers to Paris in summer.
 */
fun arrivalDayOffset(departure: String?, arrival: String?, duration: String?): Int? {
    val dep = FlightList.clockMinutes(departure) ?: return null
    val arr = FlightList.clockMinutes(arrival) ?: return null
    val mins = FlightList.durationMinutes(duration) ?: return null
    if (mins <= 0) return null

    val days = ((dep + mins - arr).toDouble() / MINUTES_PER_DAY)
    val rounded = kotlin.math.round(days).toInt()
    if (rounded < 0) return null

    // Sanity: the implied zone offset has to be a real one. If it is not,
    // one of the three numbers is wrong and saying nothing beats saying "+1".
    val offset = (arr + rounded * MINUTES_PER_DAY) - (dep + mins)
    if (offset < -12 * 60 || offset > 14 * 60) return null

    return rounded
}

private const val MINUTES_PER_DAY = 1440
