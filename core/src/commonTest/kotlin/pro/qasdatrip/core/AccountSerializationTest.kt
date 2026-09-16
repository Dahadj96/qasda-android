package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json

class AccountSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun exactTrackerRetainsItsParametersAndCompletionState() {
        val watch = json.decodeFromString<Watch>("""{"id":"123","origin":"ALG","destination":"CDG","depart_date":"2026-10-15","return_date":"2026-10-25","cabin_class":"business","adults":2,"children":1,"infants":1,"kind":"seat","active":false,"tracker_state":"completed","last_checked_at":"2026-09-15T10:00:00Z"}""")
        assertFalse(watch.active)
        assertEquals("completed", watch.trackerState)
        assertEquals(2, watch.asQuery().adults)
        assertEquals(1, watch.asQuery().children)
        assertEquals("2026-10-25", watch.asQuery().returnDate)
        assertEquals(Cabin.BUSINESS, watch.asQuery().cabin)
    }
    @Test fun inboxAcceptanceDoesNotPretendThePhoneDisplayedIt() {
        val alert = json.decodeFromString<Alert>("""{"id":"42","watchId":"123","kind":"seat","origin":"ALG","destination":"CDG","departDate":"2026-10-15","price":50000,"delivered":false}""")
        assertEquals(123L, alert.watchId)
        assertFalse(alert.delivered)
    }
}
