package pro.qasdatrip.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.qasdatrip.core.Alert
import pro.qasdatrip.core.DeviceKey
import pro.qasdatrip.core.ManageKey
import pro.qasdatrip.core.PriceTrend
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Watch

/**
 * Watching routes, without an account and now without an address either.
 *
 * The old model was a mailbox: type an address, wait for a confirmation
 * mail, follow the link. On a phone every one of those steps is a place to
 * give up, and none of them buys anything — the app is already a channel
 * that can reach this person. So the install registers itself, keeps the key
 * it gets back, and that is the whole credential.
 *
 * The manage key is still read and still honoured. Somebody who set alerts
 * up on the website and opened one of those links here has watches, and
 * taking them away to simplify a ViewModel would be a poor trade.
 */
class TrackingViewModel(
    private val api: QasdaApi,
    private val readKey: () -> ManageKey?,
    private val writeKey: (ManageKey?) -> Unit,
    private val readDevice: () -> DeviceKey? = { null },
    private val writeDevice: (DeviceKey?) -> Unit = {},
    private val locale: () -> String = { "fr" },
    private val signedIn: () -> Boolean = { false },
) : ViewModel() {

    /** Where the create form is in its short life. */
    enum class Stage { EDITING, SENDING, SENT, FAILED }

    data class State(
        val key: ManageKey? = null,
        val device: DeviceKey? = null,
        /** Nothing can be watched until the install has an identity. */
        val registering: Boolean = false,
        val watches: List<Watch> = emptyList(),
        val history: List<Watch> = emptyList(),
        val hasMore: Boolean = false,
        val historyHasMore: Boolean = false,
        val loading: Boolean = false,
        /** The link was refused: it is stale, tampered with, or the secret rotated. */
        val keyRejected: Boolean = false,
        val stage: Stage = Stage.EDITING,
        val sentTo: String? = null,
        val needsConfirmation: Boolean = true,
        val alerts: List<Alert> = emptyList(),
        val alertsLoading: Boolean = false,
        val trend: PriceTrend? = null,
        val trendLoading: Boolean = false,
        val openWatch: Watch? = null,
    )

    private val _state = MutableStateFlow(State(key = readKey(), device = readDevice()))
    val state: StateFlow<State> = _state.asStateFlow()

    private var listJob: Job? = null
    private var trendJob: Job? = null
    private var alertsJob: Job? = null
    private var registerJob: Job? = null

    init {
        register()
        if (_state.value.key != null || _state.value.device != null) refresh()
    }

    /**
     * Make sure this install has an identity, and refresh what the server
     * knows about it.
     *
     * Runs on every launch rather than only the first: the push token
     * rotates, and the cheapest way to keep it current is to send it
     * alongside a registration that is idempotent anyway. When the server
     * cannot be reached this quietly does nothing — the app still works, and
     * only tracking is unavailable until it can.
     */
    fun register(pushToken: String? = null) {
        if (_state.value.registering) return
        registerJob?.cancel()
        _state.update { it.copy(registering = true) }
        registerJob = viewModelScope.launch {
            val key = api.registerDevice(
                existing = _state.value.device,
                pushToken = pushToken,
                locale = locale(),
            )
            if (key != null) {
                writeDevice(key)
                _state.update { it.copy(device = key, registering = false) }
                refresh()
            } else {
                _state.update { it.copy(registering = false) }
            }
        }
    }

    /**
     * Re-read the list from the server every time the screen opens.
     *
     * Nothing is cached: an alert cancelled from a laptop must not still be
     * listed here, and this is a handful of rows behind a signed link.
     */
    fun refresh() {
        val key = _state.value.device?.asManageKey() ?: _state.value.key
        if (!signedIn() && key == null) return
        listJob?.cancel()
        _state.update { it.copy(loading = true) }
        listJob = viewModelScope.launch {
            val rows = if (signedIn()) api.accountWatches("active") else key?.let { api.watches(it) }
            val history = if (signedIn()) api.accountWatches("history") else emptyList()
            _state.update {
                it.copy(
                    loading = false,
                    watches = rows.orEmpty().filter { it.active },
                    history = history.orEmpty(),
                    hasMore = signedIn() && rows?.size == 50,
                    historyHasMore = signedIn() && history?.size == 50,
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

    /**
     * Watch this route and date.
     *
     * `seenPrice` is the number that was on screen when the button was
     * pressed, and it is the whole rule: cheaper than that, and the phone
     * says so. A null price is not a missing answer — it means the search
     * came back empty, and the watch becomes the other kind: tell me when a
     * seat appears.
     *
     * Nothing is typed and nothing is confirmed, so there is no SENT state
     * that means "we have emailed you". It either exists now or it does not.
     */
    fun track(query: SearchQuery, seenPrice: Double?) {
        if (!signedIn()) { _state.update { it.copy(stage = Stage.FAILED) }; return }
        val device = _state.value.device
        if (device == null) {
            // No identity yet: try once more, and let the screen stay on its
            // failed state rather than pretending the watch was made.
            register()
            _state.update { it.copy(stage = Stage.FAILED) }
            return
        }
        _state.update { it.copy(stage = Stage.SENDING) }
        viewModelScope.launch {
            val created = api.trackAccountRoute(query, seenPrice)
            if (created) {
                _state.update { it.copy(stage = Stage.SENT, needsConfirmation = false) }
                refresh()
            } else {
                _state.update { it.copy(stage = Stage.FAILED) }
            }
        }
    }

    /** Back to a blank form, so the screen can be opened again from anywhere. */
    fun resetForm() {
        _state.update { it.copy(stage = Stage.EDITING, sentTo = null) }
    }

    fun stop(watchId: Long) {
        val key = _state.value.device?.asManageKey() ?: _state.value.key
        if (!signedIn() && key == null) return
        viewModelScope.launch {
            if (if (signedIn()) api.cancelAccountWatch(watchId) else key?.let { api.cancelWatch(it, watchId) } == true) {
                // Drop it locally rather than re-listing: the server has
                // already said it is gone, and a round trip here is a list
                // that flickers.
                _state.update { s -> s.copy(watches = s.watches.filterNot { it.id == watchId }) }
            }
        }
    }

    /**
     * Stop every watch on this device.
     *
     * One call per watch rather than a bulk endpoint, because there is no
     * bulk endpoint and inventing one for a button pressed once in a
     * lifetime is not worth a migration. Each is dropped locally as the
     * server confirms it, so a list that half-succeeds on a bad connection
     * shows exactly what actually stopped.
     */
    fun stopAll() {
        val key = _state.value.device?.asManageKey() ?: _state.value.key
        if (!signedIn() && key == null) return
        val ids = _state.value.watches.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            for (id in ids) {
                if (if (signedIn()) api.cancelAccountWatch(id) else key?.let { api.cancelWatch(it, id) } == true) {
                    _state.update { s -> s.copy(watches = s.watches.filterNot { it.id == id }) }
                }
            }
        }
    }

    /**
     * What we have already told this device.
     *
     * Re-read every time the screen opens, like the watch list: a phone that
     * was offline when a price moved should see the message the first time it
     * can, not the first time it happens to be launched afterwards.
     */
    fun loadAlerts() {
        val key = _state.value.device?.asManageKey() ?: _state.value.key
        if (!signedIn() && key == null) return
        alertsJob?.cancel()
        _state.update { it.copy(alertsLoading = true) }
        alertsJob = viewModelScope.launch {
            val rows = if (signedIn()) api.accountAlerts() else key?.let { api.alerts(it) }
            _state.update { it.copy(alerts = rows.orEmpty(), alertsLoading = false) }
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

    fun accountChanged() {
        listJob?.cancel(); alertsJob?.cancel(); trendJob?.cancel()
        _state.update { it.copy(watches = emptyList(), history = emptyList(), hasMore = false, historyHasMore = false, alerts = emptyList(), openWatch = null, trend = null, stage = Stage.EDITING) }
        refresh()
    }
    fun restart(watchId: Long) { viewModelScope.launch { if (api.restartAccountWatch(watchId)) refresh() } }
    fun loadMore(history: Boolean = false) {
        if (!signedIn() || _state.value.loading) return
        _state.update { it.copy(loading = true) }
        listJob = viewModelScope.launch {
            val old = if (history) _state.value.history else _state.value.watches
            val rows = api.accountWatches(if (history) "history" else "active", old.size)
            _state.update { if (history) it.copy(history = (old + rows.orEmpty()).distinctBy { watch -> watch.id }, historyHasMore = rows?.size == 50, loading = false)
                else it.copy(watches = (old + rows.orEmpty()).distinctBy { watch -> watch.id }, hasMore = rows?.size == 50, loading = false) }
        }
    }
}
