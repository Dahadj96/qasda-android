package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManageKeyTest {

    @Test
    fun `takes the pair out of a manage link`() {
        val key = ManageKey.parse("https://dev.qasdatrip.pro/alerts?w=42&s=abc123")
        assertEquals(ManageKey(42, "abc123"), key)
    }

    @Test
    fun `survives what a mail client adds on the way`() {
        // Tracking parameters appended by the client, and a fragment.
        val key = ManageKey.parse("https://qasdatrip.pro/alerts?utm_source=mail&w=7&s=sig&x=1#top")
        assertEquals(ManageKey(7, "sig"), key)
    }

    @Test
    fun `refuses anything that is not one`() {
        // Half a link is not a link: without both halves the server would
        // reject it anyway, and storing it would leave the screen showing
        // "that link is not valid" forever.
        assertNull(ManageKey.parse("https://qasdatrip.pro/alerts?w=42"))
        assertNull(ManageKey.parse("https://qasdatrip.pro/alerts?s=abc"))
        assertNull(ManageKey.parse("https://qasdatrip.pro/alerts?w=notanumber&s=abc"))
        assertNull(ManageKey.parse("https://qasdatrip.pro/alerts"))
        assertNull(ManageKey.parse(""))
        assertNull(ManageKey.parse("not a url at all"))
    }
}

class PriceTrendTest {

    private fun trend(vararg prices: Double) = PriceTrend(
        points = prices.mapIndexed { i, p -> TrendPoint(date = "2026-08-${10 + i}", cheapest = p) },
        low = prices.minOrNull(),
        high = prices.maxOrNull(),
        observedDays = prices.size,
    )

    @Test
    fun `two readings is not a trend`() {
        assertFalse(trend(40000.0).worthDrawing)
        assertFalse(trend(40000.0, 50000.0).worthDrawing)
        assertTrue(trend(40000.0, 50000.0, 45000.0).worthDrawing)
    }

    @Test
    fun `places a price between the cheapest and dearest day seen`() {
        val t = trend(40000.0, 60000.0, 50000.0)
        assertEquals(0f, t.position(40000.0))
        assertEquals(1f, t.position(60000.0))
        assertEquals(0.5f, t.position(50000.0))
    }

    @Test
    fun `a flat fortnight has no scale to place anything on`() {
        // Every day the same price is not "in the middle" — there is no
        // middle. A bar chart that invents one implies variation that the
        // observations do not show.
        assertNull(trend(40000.0, 40000.0, 40000.0).position(40000.0))
        assertNull(PriceTrend().position(40000.0))
    }

    @Test
    fun `a price outside the observed range still lands on the chart`() {
        val t = trend(40000.0, 60000.0, 50000.0)
        assertEquals(0f, t.position(10000.0))
        assertEquals(1f, t.position(99000.0))
    }
}

class EmailShapeTest {

    @Test
    fun `accepts the shape a mail server would`() {
        assertTrue(looksLikeEmailShape("nom@exemple.com"))
        assertTrue(looksLikeEmailShape("a.b+tag@sub.domain.dz"))
    }

    @Test
    fun `refuses what would only bounce`() {
        assertFalse(looksLikeEmailShape(""))
        assertFalse(looksLikeEmailShape("nom"))
        assertFalse(looksLikeEmailShape("@exemple.com"))
        assertFalse(looksLikeEmailShape("nom@exemple"))
        assertFalse(looksLikeEmailShape("nom@@exemple.com"))
        assertFalse(looksLikeEmailShape("nom @exemple.com"))
        assertFalse(looksLikeEmailShape("nom@exemple.c"))
    }
}
