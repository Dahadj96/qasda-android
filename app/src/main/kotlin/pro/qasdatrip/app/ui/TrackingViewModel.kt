package pro.qasdatrip.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.qasdatrip.core.ManageKey
import pro.qasdatrip.core.PriceTrend
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Watch

/**
 * Watching routes, without an account.
 *
 * The whole model is a mailbox: an address is given, a confirmation mail goes
 * out, and the link in it carries a signed pair that grants exactly that
 * mailbox's alerts. This holds that pair once it has arrived, and nothing
 * else about the person.
 */
class TrackingViewModel(
    private val api: QasdaApi,
    private val readKey: () -> ManageKey?,
    private val writeKey: (ManageKey?) -> Unit,
) : ViewModel() {

    /** Where the create form is in its short life. */
    enum class Stage { EDITING, SENDING, SENT, FAILED }

    data class State(
        val key: ManageKey? = null,
        val watches: List<Watch> = emptyList(),
        val loading: Boolean = false,
        /** The link was refused: it is stale, tampered with, or the secret rotated. */
        val keyRejected: Boolean = false,
        val stage: Stage = Stage.EDITING,
        val sentTo: String? = null,
        val needsConfirmation: Boolean = true,
        val trend: PriceTrend? = null,
        val trendLoading: Boolean = false,
        val openWatch: Watch? = null,
    )

    private val _state = MutableStateFlow(State(key = readKey()))
    val state: StateFlow<State> = _state.asStateFlow()

    private var listJob: Job? = null
    private var trendJob: Job? = null

    init {
        if (_state.value.key != null) refresh()
    }

    /**
     * Re-read the list from the server every time the screen opens.
     *
     * Nothing is cached: an alert cancelled from a laptop must not still be
     * listed here, and this is a handful of rows behind a signed link.
     */
    fun refresh() {
        val key = _state.value.key ?: return
        listJob?.cancel()
        _state.update { it.copy(loading = true) }
        listJob = viewModelScope.launch {
            val rows = api.watches(key)
            _state.update {
                it.copy(
                    loading = false,
                    watches = rows.orEmpty(),
                    // Null is the server refusing the link, which is different
                    // from an empty list. Only the first should offer to take
                    // a new one.
                    keyRejected = rows == null,
                )
            }
        }
    }

    /**
     * Accept a manage link, from a tapped email link or a pasted one.
     * Returns false without storing anything if it is not one.
     */
    fun useLink(url: String): Boolean {
        val key = ManageKey.parse(url) ?: return false
        writeKey(key)
        _state.update { it.copy(key = key, keyRejected = false) }
        refresh()
        return true
    }

    fun forgetLink() {
        writeKey(null)
        _state.update { it.copy(key = null, watches = emptyList(), keyRejected = false) }
    }

    fun create(query: SearchQuery, email: String, locale: String, targetPrice: Double?) {
        _state.update { it.copy(stage = Stage.SENDING) }
        viewModelScope.launch {
            val result = api.createWatch(query, email.trim(), locale, targetPrice)
            _state.update {
                if (result?.created == true) {
                    it.copy(stage = Stage.SENT, sentTo = email.trim(), needsConfirmation = result.needsConfirmation)
                } else {
                    it.copy(stage = Stage.FAILED)
                }
            }
        }
    }

    /** Back to a blank form, so the screen can be opened again from anywhere. */
    fun resetForm() {
        _state.update { it.copy(stage = Stage.EDITING, sentTo = null) }
    }

    fun stop(watchId: Long) {
        val key = _state.value.key ?: return
        viewModelScope.launch {
            if (api.cancelWatch(key, watchId)) {
                // Drop it locally rather than re-listing: the server has
                // already said it is gone, and a round trip here is a list
                // that flickers.
                _state.update { s -> s.copy(watches = s.watches.filterNot { it.id == watchId }) }
            }
        }
    }

    /**
     * Open one watch and fetch what it has cost.
     *
     * A null trend is a server that has nothing to say — an older build with
     * no history endpoint, or a route nobody has searched. The screen shows
     * the watch either way; the chart is the extra.
     */
    fun open(watch: Watch) {
        trendJob?.cancel()
        _state.update { it.copy(openWatch = watch, trend = null, trendLoading = true) }
        trendJob = viewModelScope.launch {
            val trend = api.priceHistory(watch.asQuery())
            _state.update { it.copy(trend = trend, trendLoading = false) }
        }
    }

    fun closeWatch() {
        trendJob?.cancel()
        _state.update { it.copy(openWatch = null, trend = null, trendLoading = false) }
    }
}
