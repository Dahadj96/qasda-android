package pro.qasdatrip.core

import kotlin.math.abs

/**
 * How long the passenger is on the ground, and where.
 *
 * The payload never says this. It gives the segments — ALG 20:30 → TUN
 * 21:40, then TUN 17:30 → CDG 20:50 — and a total duration for the leg, and
 * leaves the gap between them to be worked out. The app was not working it
 * out, so a flight that spends nineteen hours in Tunis and a flight that
 * connects in ninety minutes both read as "1 escale", and the cheaper of the
 * two looked like the better deal.
 *
 * The arithmetic is easier than it looks in one respect and harder in
 * another. Easier: the arrival and the next departure are both local times
 * at the *same* airport, so no time zone enters into it and the wait is
 * simply the clock difference. Harder: the clock wraps. A landing at 21:40
 * and a departure at 17:30 is nineteen hours and fifty minutes, not minus
 * four — and if the wait runs past twenty-four hours the clock has nothing
 * left to say about it at all.
 *
 * So the wrap is resolved against the one number that does carry the whole
 * span: the leg's own duration. Time on the ground plus time in the air is
 * the duration, and time in the air has to be a number an aircraft could
 * plausibly produce. When a naive reading leaves twenty hours of flying for
 * two short hops, a wait was understated by a day and the day is added back.
 * When no reading survives that test, this returns nothing rather than a
 * guess — a wrong layover is worse on this screen than no layover, because
 * somebody books around it.
 */
data class Layover(
    /** The IATA code of the airport waited in, when the payload names it. */
    val airport: String?,
    val minutes: Int,
    /** Four hours or more: long enough to change what the trip costs a person. */
    val long: Boolean,
    /** The wait runs through local midnight, or past it into the next day. */
    val overnight: Boolean,
)

/** Below this, two segments are one connection and not a stopover. */
private const val MIN_CONNECTION = 20

/** No single hop between two airports on these routes takes longer. */
private const val MAX_HOP_MINUTES = 16 * 60

private const val LONG_LAYOVER = 4 * 60
private const val DAY = 24 * 60

/**
 * One entry per stop, in order. Empty when the leg is direct, when it has no
 * segments, or when the numbers do not agree well enough to be trusted.
 */
fun Leg.layovers(): List<Layover> {
    val legs = segments
    if (legs.size < 2) return emptyList()

    val gaps = ArrayList<Int>(legs.size - 1)
    for (i in 0 until legs.size - 1) {
        val landed = FlightList.clockMinutes(legs[i].arrival) ?: return emptyList()
        val leaves = FlightList.clockMinutes(legs[i + 1].departure) ?: return emptyList()
        gaps += ((leaves - landed) % DAY + DAY) % DAY
    }

    val total = FlightList.durationMinutes(duration)
    val resolved = resolve(gaps, total, legs.size) ?: return emptyList()

    return resolved.mapIndexed { i, minutes ->
        val landed = FlightList.clockMinutes(legs[i].arrival) ?: 0
        Layover(
            airport = legs[i].destination?.trim()?.takeIf { it.isNotEmpty() }
                ?: legs[i + 1].origin?.trim()?.takeIf { it.isNotEmpty() },
            minutes = minutes,
            long = minutes >= LONG_LAYOVER,
            // Past midnight, or so late that the wait is a night whatever the
            // clock says about it.
            overnight = landed + minutes >= DAY || minutes >= DAY,
        )
    }
}

/**
 * Pick the reading of the gaps that leaves a believable amount of flying.
 *
 * With one stop there are two candidates and with two stops four, so this
 * enumerates rather than reasons. Adding a whole day to a gap is only ever
 * done to make the flying time plausible, never to make a layover look
 * dramatic.
 */
private fun resolve(gaps: List<Int>, total: Int?, segments: Int): List<Int>? {
    // Without a duration there is nothing to check the wrap against. The
    // naive reading is still right for every connection under 24 hours,
    // which is all but a handful — but a gap that looks impossibly short is
    // the signature of a wrap, and that one we will not guess at.
    if (total == null) return gaps.takeIf { it.none { g -> g < MIN_CONNECTION } }

    val best = (0 until (1 shl gaps.size))
        .map { mask -> gaps.mapIndexed { i, g -> if (mask shr i and 1 == 1) g + DAY else g } }
        .filter { candidate ->
            val flying = total - candidate.sum()
            flying > 0 &&
                flying <= segments * MAX_HOP_MINUTES &&
                candidate.none { it < MIN_CONNECTION }
        }
        // Of the readings that survive, the one whose implied flying time is
        // closest to what these segments would actually take.
        .minByOrNull { candidate -> abs((total - candidate.sum()) - segments * TYPICAL_HOP) }

    return best
}

/** Algiers to anywhere these sites sell is roughly two hours a hop. */
private const val TYPICAL_HOP = 130

/** "6 h 05", the way a wait is written. Never "6.08 h" and never "365 min". */
fun formatMinutes(total: Int): String {
    val hours = total / 60
    val minutes = total % 60
    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h " + minutes.toString().padStart(2, '0')
    }
}
