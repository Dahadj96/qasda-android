package pro.qasdatrip.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.SearchQuery

/**
 * The question, while it is still being asked.
 *
 * The search used to live in `remember` inside the search screen, which was
 * fine while every answer was given in a dialog on top of that screen. It
 * stops being fine the moment each answer is its own page: navigating away
 * would throw the half-finished question on the floor, and coming back would
 * start it again from Alger.
 *
 * So the draft is a ViewModel. It outlives the four pages that fill it in,
 * it survives a rotation, and it is the single place that knows whether the
 * question is complete enough to send.
 */
data class SearchDraft(
    val from: String = "ALG",
    val to: String = "CDG",
    val depart: String? = null,
    val back: String? = null,
    val roundTrip: Boolean = false,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val cabin: Cabin = Cabin.ECONOMY,
) {
    val travellers: Int get() = adults + children + infants

    /**
     * A round trip without a return is half a question: the server would
     * answer it as a one-way and quote the wrong thing. A route that starts
     * where it ends is not a journey.
     */
    val complete: Boolean
        get() = !depart.isNullOrBlank() && (!roundTrip || !back.isNullOrBlank()) && from != to

    fun toQuery(): SearchQuery = SearchQuery(
        from = from,
        to = to,
        departDate = depart.orEmpty(),
        returnDate = back.takeIf { roundTrip },
        adults = adults,
        children = children,
        infants = infants,
        cabin = cabin,
    )

    companion object {
        fun of(query: SearchQuery, keepDates: Boolean): SearchDraft = SearchDraft(
            from = query.from,
            to = query.to,
            depart = query.departDate.takeIf { keepDates },
            back = query.returnDate?.takeIf { keepDates },
            roundTrip = query.roundTrip,
            adults = query.adults,
            children = query.children,
            infants = query.infants,
            cabin = query.cabin,
        )
    }
}

class SearchFormViewModel : ViewModel() {
    private val _draft = MutableStateFlow(SearchDraft())
    val draft: StateFlow<SearchDraft> = _draft.asStateFlow()

    fun set(block: (SearchDraft) -> SearchDraft) { _draft.update(block) }

    fun from(iata: String) = set { it.copy(from = iata) }

    fun to(iata: String) = set { it.copy(to = iata) }

    /** Departure and destination change places, and so do the two names. */
    fun swap() = set { it.copy(from = it.to, to = it.from) }

    fun dates(depart: String?, back: String?) = set { it.copy(depart = depart, back = back) }

    fun roundTrip(on: Boolean) = set {
        // Switching to one way drops a return that is no longer part of the
        // question being asked.
        it.copy(roundTrip = on, back = if (on) it.back else null)
    }

    fun travellers(adults: Int, children: Int, infants: Int, cabin: Cabin) = set {
        it.copy(adults = adults, children = children, infants = infants, cabin = cabin)
    }

    fun load(query: SearchQuery, keepDates: Boolean) {
        _draft.value = SearchDraft.of(query, keepDates)
    }
}
