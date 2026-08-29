package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Both cases here are real payloads from the live API for ALG to CDG. They
 * are the reason this code exists: on the card they are both "1 escale", and
 * one of them parks the passenger in Tunis from one evening to the next.
 */
class LayoversTest {

    private fun leg(duration: String, vararg hops: Triple<String, String, String>) = Leg(
        duration = duration,
        stops = hops.size - 1,
        segments = hops.mapIndexed { i, (airports, dep, arr) ->
            val (from, to) = airports.split(">")
            Segment(
                flightNo = "XX${100 + i}", origin = from, destination = to,
                departure = dep, arrival = arr,
            )
        },
    )

    @Test
    fun `a night in Tunis is nineteen hours and fifty minutes`() {
        val tunis = leg(
            "23h 20m",
            Triple("ALG>TUN", "20:30", "21:40"),
            Triple("TUN>CDG", "17:30", "20:50"),
        )
        val stops = tunis.layovers()
        assertEquals(1, stops.size)
        assertEquals(19 * 60 + 50, stops[0].minutes)
        assertEquals("TUN", stops[0].airport)
        assertTrue(stops[0].long)
        assertTrue(stops[0].overnight, "landing at 21:40 and leaving at 17:30 is a night on the ground")
    }

    @Test
    fun `a morning connection through Annaba is six hours and five minutes`() {
        val annaba = leg(
            "9h 40m",
            Triple("ALG>AAE", "06:30", "07:45"),
            Triple("AAE>CDG", "13:50", "17:10"),
        )
        val stops = annaba.layovers()
        assertEquals(1, stops.size)
        assertEquals(6 * 60 + 5, stops[0].minutes)
        assertTrue(stops[0].long)
        assertTrue(!stops[0].overnight, "07:45 to 13:50 is the same morning")
    }

    @Test
    fun `a direct leg has nothing on the ground`() {
        assertEquals(emptyList(), leg("2h 35m", Triple("ALG>CDG", "03:25", "07:00")).layovers())
    }

    @Test
    fun `a wait longer than a day is recovered from the duration`() {
        // Lands 10:00, leaves 11:00 the following day. The clock alone says
        // one hour; the duration says the aircraft would have to fly for
        // twenty-six of the twenty-eight hours, which no pair of hops does.
        val long = leg(
            "28h 00m",
            Triple("ALG>IST", "07:00", "10:00"),
            Triple("IST>CDG", "11:00", "14:00"),
        )
        val stops = long.layovers()
        assertEquals(1, stops.size)
        assertEquals(25 * 60, stops[0].minutes)
        assertTrue(stops[0].overnight)
    }

    @Test
    fun `numbers that cannot be reconciled produce nothing at all`() {
        // Three hours in total, six on the ground. One of the two is wrong
        // and there is no way to tell which, so the screen says nothing.
        val nonsense = leg(
            "3h 00m",
            Triple("ALG>TUN", "08:00", "09:00"),
            Triple("TUN>CDG", "15:00", "17:00"),
        )
        assertEquals(emptyList(), nonsense.layovers())
    }

    @Test
    fun `a wait is written the way a person writes one`() {
        assertEquals("6h 05", formatMinutes(365))
        assertEquals("19h 50", formatMinutes(1190))
        assertEquals("2h", formatMinutes(120))
        assertEquals("45m", formatMinutes(45))
    }
}
