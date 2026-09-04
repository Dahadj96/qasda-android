package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarTest {

    private fun cell(depart: String, back: String? = null, price: Double?) =
        CalendarCell(departDate = depart, returnDate = back, price = price)

    @Test
    fun `a one-way calendar charts each priced day`() {
        val cal = FlightCalendar(
            departDates = listOf("2026-09-08", "2026-09-09", "2026-09-10"),
            cells = listOf(
                cell("2026-09-09", price = 200.0),
                cell("2026-09-08", price = 100.0),
                cell("2026-09-10", price = null),
            ),
        )
        assertEquals(listOf("2026-09-08", "2026-09-09"), cal.pricedDays.map { it.departDate })
    }

    @Test
    fun `a return calendar charts the cheapest of each departure row`() {
        // The chart used to look only for cells without a return date, find
        // none on a return trip, and say there were no prices while the grid
        // beside it was full of them.
        val cal = FlightCalendar(
            departDates = listOf("2026-09-08", "2026-09-09"),
            returnDates = listOf("2026-09-15", "2026-09-16"),
            cells = listOf(
                cell("2026-09-08", "2026-09-15", 300.0),
                cell("2026-09-08", "2026-09-16", 250.0),
                cell("2026-09-09", "2026-09-15", 400.0),
                cell("2026-09-09", "2026-09-16", null),
            ),
        )
        val days = cal.pricedDays
        assertEquals(listOf("2026-09-08", "2026-09-09"), days.map { it.departDate })
        assertEquals(listOf(250.0, 400.0), days.map { it.price })
    }
}
