package pro.qasdatrip.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Filters
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
    /**
     * A return trip, unless somebody says otherwise.
     *
     * It defaulted to one-way, and testers kept arriving at the calendar
     * having quietly been put in a mode they did not choose - then finding
     * no way to change it from there. Most people flying out of Algeria are
     * coming back; the default should be the common trip, and the choice now
     * also lives on the date screen where it is actually needed.
     */
    val roundTrip: Boolean = true,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val cabin: Cabin = Cabin.ECONOMY,
    /**
     * The two filters people reach for before they have seen a single
     * price. They are the same filters as on the results - not a second
     * system - so a search started with "direct" lands on a list that
     * already says Direct is on, and turning it off there is one tap.
     */
    val directOnly: Boolean = false,
    val bagOnly: Boolean = false,
) {
    val travellers: Int get() = adults + children + infants

    /** What the form's quick filters mean, in the results' own terms. */
    fun toFilters(): Filters = Filters(
        maxStops = if (directOnly) 0 else null,
        bagOnly = bagOnly,
    )

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

    fun directOnly(on: Boolean) = set { it.copy(directOnly = on) }

    fun bagOnly(on: Boolean) = set { it.copy(bagOnly = on) }

    fun travellers(adults: Int, children: Int, infants: Int, cabin: Cabin) = set {
        it.copy(adults = adults, children = children, infants = infants, cabin = cabin)
    }

    /**
     * The outbound, a day either side, on its own.
     *
     * The first version carried the return along so the trip kept its
     * length. Testers found that surprising - they pressed one arrow and
     * watched two dates move - so each end now moves by itself, and the only
     * rule is that a return can never sit before its outbound: if the
     * outbound is pushed past it, the return is pushed to the same day.
     */
    fun shiftDepart(days: Long) = set { d ->
        val depart = d.depart?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@set d
        val moved = depart.plusDays(days)
        // Never into the past: a search for yesterday has no answer.
        if (moved.isBefore(LocalDate.now())) return@set d
        val back = d.back?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val keptBack = back?.let { if (it.isBefore(moved)) moved else it }
        d.copy(depart = moved.toString(), back = keptBack?.toString())
    }

    /** The return only, which changes the length of the trip. */
    fun shiftReturn(days: Long) = set { d ->
        val back = d.back?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@set d
        val depart = d.depart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val moved = back.plusDays(days)
        // A return before the outbound is not a trip; stop at the outbound.
        if (depart != null && moved.isBefore(depart)) return@set d
        d.copy(back = moved.toString())
    }

    fun load(query: SearchQuery, keepDates: Boolean) = set { current ->
        // A past search fills the route and the passengers; the quick
        // filters are a preference, not part of the trip, and stay as set.
        SearchDraft.of(query, keepDates).copy(directOnly = current.directOnly, bagOnly = current.bagOnly)
    }
}
