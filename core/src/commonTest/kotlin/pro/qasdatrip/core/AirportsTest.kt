package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The rules that made the website's airport search usable, pinned here so the
 * app cannot quietly lose them.
 */
class FoldTest {

    @Test
    fun `an alif with and without its hamza are the same word`() {
        assertEquals(fold("أدرار"), fold("ادرار"))
        assertEquals(fold("إسطنبول"), fold("اسطنبول"))
    }

    @Test
    fun `a ta marbuta and a ha are the same ending`() {
        assertEquals(fold("قسنطينة"), fold("قسنطينه"))
    }

    @Test
    fun `a French keyboard is not a different language`() {
        assertEquals(fold("Séville"), fold("seville"))
        assertEquals(fold("Genève"), fold("geneve"))
    }

    @Test
    fun `Arabic-Indic digits read as digits`() {
        assertEquals("2026", fold("٢٠٢٦"))
    }
}

class AirportSearchTest {

    private val sample = """
        [{"i":"AZR","c":{"en":"Adrar","fr":"Adrar","ar":"أدرار"},"n":"Touat Airport","a":["Touat"]},
         {"i":"ORN","c":{"en":"Oran","fr":"Oran","ar":"وهران"},"n":"Ahmed Ben Bella","a":["Es Senia"]},
         {"i":"ORA","c":{"en":"Embarcacion","fr":"Embarcacion","ar":"إمباركاسيون"},"n":"Embarcacion","a":[]}]
    """.trimIndent()

    private fun load() = Airports.load(sample)

    @Test
    fun `typing an Arabic city without its hamza finds it`() {
        load()
        assertEquals("AZR", Airports.search("ادرار").first().iata)
    }

    @Test
    fun `a curated city beats an airstrip that happens to share three letters`() {
        load()
        // "ora" is a code in Argentina and the start of Oran. Here, Oran wins.
        assertEquals("ORN", Airports.search("ora").first().iata)
    }

    @Test
    fun `an airport answers to the commune it is filed under`() {
        load()
        assertTrue(Airports.search("es senia").any { it.iata == "ORN" })
    }
}

class MoneyTest {

    @Test
    fun `the currency sits on the side the language reads from`() {
        assertTrue(Money.format(52400.0, Lang.EN).endsWith("DZD"))
        assertTrue(Money.format(52400.0, Lang.AR).endsWith("دج"))
    }

    @Test
    fun `digits are isolated so Arabic cannot reorder them`() {
        val out = Money.format(52400.0, Lang.AR)
        assertTrue(out.contains('⁦') && out.contains('⁩'))
    }
}

class SeatsTest {

    @Test
    fun `nine means at least nine and is never printed`() {
        assertEquals(null, Seats.left(9))
        assertEquals(null, Seats.left(5))
        assertEquals(2, Seats.left(2))
        assertEquals(null, Seats.left(null))
    }
}
