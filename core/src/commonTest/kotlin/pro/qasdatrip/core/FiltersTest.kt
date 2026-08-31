package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClockTest {

    @Test
    fun `a bare clock time reads as minutes past midnight`() {
        assertEquals(7 * 60 + 30, FlightList.clockMinutes("07:30"))
        assertEquals(0, FlightList.clockMinutes("00:00"))
        assertEquals(23 * 60 + 59, FlightList.clockMinutes("23:59"))
    }

    @Test
    fun `a whole timestamp gives up its clock time`() {
        // Not every site sends the same shape, so the reader looks for the
        // first HH:MM rather than trusting a format none of them agreed on.
        assertEquals(16 * 60 + 40, FlightList.clockMinutes("2026-09-20T16:40:00"))
    }

    @Test
    fun `something that is not a time is not guessed at`() {
        assertNull(FlightList.clockMinutes(null))
        assertNull(FlightList.clockMinutes(""))
        assertNull(FlightList.clockMinutes("bientôt"))
        assertNull(FlightList.clockMinutes("99:99"))
    }
}

class DurationTest {

    @Test
    fun `the shapes the sites actually send all read the same`() {
        assertEquals(195, FlightList.durationMinutes("3h15"))
        assertEquals(195, FlightList.durationMinutes("3 h 15"))
        assertEquals(195, FlightList.durationMinutes("PT3H15M"))
        assertEquals(60, FlightList.durationMinutes("1h"))
        assertEquals(45, FlightList.durationMinutes("PT45M"))
        assertEquals(195, FlightList.durationMinutes("195"))
    }

    @Test
    fun `an unreadable duration stays unknown rather than becoming zero`() {
        // Zero would sort to the top and read as the fastest flight on the
        // list, which is the opposite of what not knowing means.
        assertNull(FlightList.durationMinutes(null))
        assertNull(FlightList.durationMinutes("direct"))
    }
}

class FilterTest {

    private fun flight(
        id: String,
        price: Double?,
        stops: Int? = 0,
        bag: Boolean = true,
        departure: String? = "08:00",
        duration: String? = "2h00",
    ) = Flight(
        id = id,
        hasLuggage = bag,
        prices = mapOf("volz" to price),
        outbound = Leg(departure = departure, stops = stops, duration = duration),
    )

    private fun roundTrip(
        id: String,
        outStops: Int?,
        backStops: Int?,
        price: Double = 10_000.0,
    ) = Flight(
        id = id,
        isRoundTrip = true,
        hasLuggage = true,
        prices = mapOf("volz" to price),
        outbound = Leg(departure = "08:00", stops = outStops, duration = "2h00"),
        inbound = Leg(departure = "19:00", stops = backStops, duration = "2h00"),
    )

    @Test
    fun `direct on a return trip means direct in both directions`() {
        // The bug this replaces: only the outbound was read, so a fare that
        // flew straight out and changed planes on the way home passed a
        // "direct" filter. Somebody filtering for direct flights was shown a
        // connection in the half of the trip they were not looking at.
        val bothDirect = roundTrip("both", outStops = 0, backStops = 0)
        val homeChanges = roundTrip("home-changes", outStops = 0, backStops = 1)
        val outChanges = roundTrip("out-changes", outStops = 1, backStops = 0)

        val kept = FlightList.apply(
            listOf(bothDirect, homeChanges, outChanges),
            Filters(maxStops = 0),
            SortBy.PRICE,
        ).map { it.id }

        assertEquals(listOf("both"), kept)
    }

    @Test
    fun `a return leg with no stop count survives a direct filter`() {
        // Same rule as everywhere else in this file: a filter removes a
        // flight when the data says to, never because the data was missing.
        val unknownReturn = roundTrip("unknown-return", outStops = 0, backStops = null)
        val kept = FlightList.apply(
            listOf(unknownReturn),
            Filters(maxStops = 0),
            SortBy.PRICE,
        ).map { it.id }
        assertEquals(listOf("unknown-return"), kept)
    }

    @Test
    fun `a flight with no departure time survives a time filter`() {
        // The rule this file exists to protect: a filter removes a flight when
        // the data says to, never because the data was missing. Dropping it
        // would show a shorter list and call it "morning flights".
        val unknown = flight("no-time", 10_000.0, departure = null)
        val evening = flight("evening", 12_000.0, departure = "19:30")
        val result = FlightList.apply(
            listOf(unknown, evening),
            Filters(departBands = setOf(TimeBand.MORNING)),
            SortBy.PRICE,
        )
        assertEquals(listOf("no-time"), result.map { it.id })
    }

    @Test
    fun `a flight with no stop count survives a direct-only filter`() {
        val unknown = flight("no-stops", 10_000.0, stops = null)
        val oneStop = flight("one-stop", 9_000.0, stops = 1)
        val result = FlightList.apply(listOf(unknown, oneStop), Filters(maxStops = 0), SortBy.PRICE)
        assertEquals(listOf("no-stops"), result.map { it.id })
    }

    @Test
    fun `the price ceiling keeps what is at the ceiling`() {
        val at = flight("at", 20_000.0)
        val over = flight("over", 20_001.0)
        val result = FlightList.apply(listOf(at, over), Filters(maxPrice = 20_000.0), SortBy.PRICE)
        assertEquals(listOf("at"), result.map { it.id })
    }

    @Test
    fun `night wraps around midnight`() {
        assertTrue(TimeBand.NIGHT.contains(23 * 60 + 30))
        assertTrue(TimeBand.NIGHT.contains(2 * 60))
        assertTrue(!TimeBand.NIGHT.contains(9 * 60))
    }

    @Test
    fun `an unknown value sorts last instead of first`() {
        val known = flight("known", 10_000.0, duration = "5h00")
        val unknown = flight("unknown", 9_000.0, duration = null)
        val result = FlightList.apply(listOf(unknown, known), Filters(), SortBy.DURATION)
        assertEquals(listOf("known", "unknown"), result.map { it.id })
    }
}
