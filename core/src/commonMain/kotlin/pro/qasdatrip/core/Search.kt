package pro.qasdatrip.core

/** Everything a search needs, and nothing it does not. */
data class SearchQuery(
    val from: String,
    val to: String,
    val departDate: String,           // YYYY-MM-DD
    val returnDate: String? = null,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val cabin: Cabin = Cabin.ECONOMY,
) {
    val roundTrip: Boolean get() = !returnDate.isNullOrBlank()
    val travellers: Int get() = adults + children + infants
}

enum class Cabin(val wire: String) {
    ECONOMY("economy"), PREMIUM("premium"), BUSINESS("business"), FIRST("first");
}

/**
 * What a search reports as it goes. The server answers after each site, so
 * this is a stream and not a result: a list that grows, then either finishes
 * or says why it stopped.
 */
sealed interface SearchEvent {
    data class Results(val flights: List<Flight>) : SearchEvent
    data object Done : SearchEvent
    data class Failed(val reason: Reason) : SearchEvent

    enum class Reason {
        /** The connection went away, or stayed open and said nothing. */
        CONNECTION,
        /** The server answered, and the answer was an error. */
        SERVER,
    }
}
