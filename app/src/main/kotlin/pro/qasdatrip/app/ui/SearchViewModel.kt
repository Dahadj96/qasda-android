package pro.qasdatrip.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchEvent
import pro.qasdatrip.core.SearchQuery

/**
 * One search at a time, and what it has produced so far.
 *
 * A flight with no quoted price never reaches the list: every flight here is
 * on the list because a site returned it with a price, and a computed figure
 * is not a price we are willing to stand behind.
 */
class SearchViewModel(private val api: QasdaApi) : ViewModel() {

    data class State(
        val query: SearchQuery? = null,
        val flights: List<Flight> = emptyList(),
        val running: Boolean = false,
        val failed: SearchEvent.Reason? = null,
        val selected: Flight? = null,
    ) {
        val empty: Boolean get() = !running && failed == null && query != null && flights.isEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    fun search(query: SearchQuery) {
        job?.cancel()
        _state.value = State(query = query, running = true)
        job = viewModelScope.launch {
            api.search(query).collect { event ->
                when (event) {
                    is SearchEvent.Results -> {
                        Airlines.learn(event.flights)
                        val priced = event.flights.filter { it.cheapest != null }
                            .sortedBy { it.cheapest?.second ?: Double.MAX_VALUE }
                        _state.update { current ->
                            // The list is re-sorted on every update, so a
                            // position is not an identity. Re-find the open
                            // flight by its id instead, and its details page
                            // follows the price down as later sites answer.
                            val stillOpen = current.selected?.let { open ->
                                priced.firstOrNull { it.id != null && it.id == open.id } ?: open
                            }
                            current.copy(flights = priced, selected = stillOpen)
                        }
                    }
                    SearchEvent.Done -> _state.update { it.copy(running = false) }
                    is SearchEvent.Failed -> _state.update {
                        // Something already arrived: show it rather than an error.
                        if (it.flights.isNotEmpty()) it.copy(running = false)
                        else it.copy(running = false, failed = event.reason)
                    }
                }
            }
        }
    }

    fun retry() { state.value.query?.let(::search) }

    /** Which flight the details screen is looking at. */
    fun open(flight: Flight?) { _state.update { it.copy(selected = flight) } }

    suspend fun bookingUrl(flight: Flight, site: String): String? {
        val q = state.value.query ?: return null
        return api.bookingUrl(q, site)
    }
}
