package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The numbers here are a real answer from the live API for ALG to CDG on
 * 31 August 2026, kept because they are what caught the bug.
 */
class StopCountTest {

    @Test
    fun `two segments is one stop, whatever the missing field says`() {
        // Every flight in that response came back with stops empty. Reading
        // that as zero put "Direct" on an itinerary through Tunis.
        val viaTunis = Leg(
            departure = "14:25", arrival = "17:35", duration = "26h 10m",
            segments = listOf(
                Segment(flightNo = "BJ 131", origin = "ALG", destination = "TUN"),
                Segment(flightNo = "BJ 730", origin = "TUN", destination = "CDG"),
            ),
        )
        assertEquals(1, viaTunis.stopCount)
    }

    @Test
    fun `one segment is direct`() {
        val nonstop = Leg(
            departure = "17:35", arrival = "21:05", duration = "2h 30m",
            segments = listOf(Segment(origin = "ALG", destination = "CDG")),
        )
        assertEquals(0, nonstop.stopCount)
    }

    @Test
    fun `the server's own count wins when it gives one`() {
        val stated = Leg(stops = 2, segments = listOf(Segment(), Segment()))
        assertEquals(2, stated.stopCount)
    }

    @Test
    fun `no count and no segments is unknown, not direct`() {
        // The whole point: unknown has to stay unknown so the screen can say
        // nothing. "Direct" is a claim and a claim needs evidence.
        assertNull(Leg(departure = "08:00").stopCount)
    }
}

class ArrivalDayTest {

    @Test
    fun `an overnight through Tunis lands the next day`() {
        // 14:25 + 26h10 = 2435 minutes; the arrival clock reads 17:35.
        // Only one whole number of days leaves a believable zone offset.
        assertEquals(1, arrivalDayOffset("14:25", "17:35", "26h 10m"))
    }

    @Test
    fun `an afternoon hop lands the same day`() {
        assertEquals(0, arrivalDayOffset("17:35", "21:05", "2h 30m"))
        assertEquals(0, arrivalDayOffset("03:25", "07:00", "2h 35m"))
        assertEquals(0, arrivalDayOffset("12:20", "22:25", "9h 5m"))
    }

    @Test
    fun `a red-eye that crosses midnight lands the next day`() {
        // Leaves 23:30, flies four hours, arrives 03:30 - and the offset that
        // implies is zero, which is a real one.
        assertEquals(1, arrivalDayOffset("23:30", "03:30", "4h"))
    }

    @Test
    fun `a long flight is not assumed to be overnight`() {
        // Twenty hours arriving at 10:20 implies a +4h20 zone offset, which
        // is an ordinary one - Algiers to much of Asia. The rule is about
        // what the numbers support, not about how long the flight feels.
        assertEquals(1, arrivalDayOffset("10:00", "10:20", "20h"))
    }

    @Test
    fun `an arrival that cannot follow its departure says nothing`() {
        // One hour in the air cannot leave at midnight and land at 23:00:
        // no whole number of days reconciles it with a real time zone, so
        // the screen gets no marker rather than a wrong one.
        assertNull(arrivalDayOffset("00:00", "23:00", "1h"))
    }

    @Test
    fun `anything missing is unknown`() {
        assertNull(arrivalDayOffset(null, "17:35", "3h"))
        assertNull(arrivalDayOffset("14:25", null, "3h"))
        assertNull(arrivalDayOffset("14:25", "17:35", null))
    }
}
