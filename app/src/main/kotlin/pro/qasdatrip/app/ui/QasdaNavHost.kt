package pro.qasdatrip.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pro.qasdatrip.app.BuildConfig
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.ThemeMode
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.DeviceKey
import pro.qasdatrip.core.ManageKey
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Words

private const val SEARCH = "search"
private const val PICK_FROM = "pick-from"
private const val PICK_TO = "pick-to"
private const val PICK_DATES = "pick-dates"
private const val PICK_TRAVELLERS = "pick-travellers"
private const val REVIEW = "review"
private const val RESULTS = "results"
private const val DETAILS = "details"
private const val CALENDAR = "calendar"
private const val HELP = "help"
private const val ACCOUNT = "account"
private const val SETTINGS = "settings"
private const val ABOUT = "about"
private const val TRACKING = "tracking"
private const val TRACK_NEW = "track-new"
private const val TRACK_HISTORY = "track-history"
private const val NOTIFICATIONS = "notifications"
private const val FILTERS = "filters"
private const val CHART = "chart"
private const val PICK_HOME = "pick-home"
private const val HANDOVER = "handover"

/**
 * The hosts a manage link can arrive from. dev is here because that is where
 * the app points today; the other two are here so a link still works after
 * the apex domain goes live and an old email is opened a month later.
 */
private val ManageHosts = listOf("dev.qasdatrip.pro", "qasdatrip.pro", "www.qasdatrip.pro")

/**
 * Which alerts this phone is willing to be interrupted by.
 *
 * Three flags rather than three parameters because they travel together
 * everywhere and are always changed one at a time from the same screen.
 */
data class AlertPrefs(
    val drops: Boolean = true,
    val seats: Boolean = true,
    val ended: Boolean = true,
)

/** The destinations the bar can reach. Results and details are inside the search one. */
private enum class Tab(val route: String, val label: (Words) -> String) {
    HOME(SEARCH, { it.navHome }),
    TRACKING_TAB(TRACKING, { it.navTracking }),
    HELP_TAB(HELP, { it.navHelp }),
    // Compte, not Réglages. Nobody opens an app to visit its settings; they
    // open it to see what is theirs. Réglages is a row inside this one.
    ACCOUNT_TAB(ACCOUNT, { it.navAccount }),
}

/**
 * The bar's glyphs, outlined in every state.
 *
 * They were the filled set, which is Material's default pairing — outline
 * when idle, solid when chosen. The drawn bar does not do that: all four
 * stay line drawings and the colour alone says which one you are on. That is
 * the lighter, more current look, and it is what the design says, so it wins
 * over the framework's habit.
 */
@Composable
private fun TabIcon(tab: Tab) = when (tab) {
    Tab.HOME -> Icon(Icons.Outlined.Home, contentDescription = null)
    Tab.TRACKING_TAB -> Icon(Icons.Outlined.Notifications, contentDescription = null)
    // Material core ships Info but no question mark, and this tab is help
    // rather than about.
    Tab.HELP_TAB -> Icon(painterResource(R.drawable.ic_help), contentDescription = null)
    Tab.ACCOUNT_TAB -> Icon(Icons.Outlined.Person, contentDescription = null)
}

@Composable
fun QasdaNavHost(
    api: QasdaApi,
    lang: Lang,
    chosenLang: Lang?,
    onLang: (Lang?) -> Unit,
    recent: List<SearchQuery> = emptyList(),
    onRemember: (SearchQuery) -> Unit = {},
    manageKey: ManageKey? = null,
    onManageKey: (ManageKey?) -> Unit = {},
    readDevice: () -> DeviceKey? = { null },
    onDeviceKey: (DeviceKey?) -> Unit = {},
    homeAirport: String? = null,
    onHomeAirport: (String) -> Unit = {},
    alertsSeenAt: Long = 0L,
    onAlertsSeen: () -> Unit = {},
    alertPrefs: AlertPrefs = AlertPrefs(),
    onAlertPrefs: (AlertPrefs) -> Unit = {},
    onClearRecent: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeMode: (ThemeMode) -> Unit = {},
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val words = LocalWords.current

    val haptics = LocalHaptics.current

    val vm: SearchViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(api) as T
    })
    val state by vm.state.collectAsStateWithLifecycle()

    // What the search turned into, felt once.
    //
    // Keyed on the running -> settled edge rather than on the state itself,
    // which matters: a screen composed with results already in hand - coming
    // back from a flight's detail page, or a rotation - is not a search
    // finishing, and would otherwise buzz for something that happened
    // minutes ago. The commit is felt on the button, on the screen that owns
    // it; this is the answer arriving, and it lands later.
    //
    // "No flights" is a warning rather than an error on purpose: it is a true
    // answer to the question asked, and buzzing as though the app broke would
    // be a lie about somebody's trip.
    var wasRunning by remember { mutableStateOf(false) }
    LaunchedEffect(state.running) {
        if (wasRunning && !state.running) {
            haptics.play(
                when {
                    state.failed != null -> Feedback.Error
                    state.empty -> Feedback.Warning
                    else -> Feedback.Success
                },
            )
        }
        wasRunning = state.running
    }

    // The half-finished question, owned above the five pages that fill it in
    // so walking between them cannot lose it.
    val form: SearchFormViewModel = viewModel()
    val draft by form.draft.collectAsStateWithLifecycle()

    // Somebody who told us where they live should not be handed Alger every
    // time the app opens. Keyed on the setting, so it seeds the draft once
    // and does not fight with a route they are in the middle of changing.
    LaunchedEffect(homeAirport) {
        homeAirport?.let { form.from(it) }
    }

    val tracking: TrackingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackingViewModel(
                api,
                readKey = { manageKey },
                writeKey = onManageKey,
                readDevice = readDevice,
                writeDevice = onDeviceKey,
                locale = { lang.tag },
            ) as T
    })
    val trackState by tracking.state.collectAsStateWithLifecycle()


    // Which offer, on which site, is being handed over. Held here rather
    // than passed as route arguments because a Flight is not a string and
    // serialising one into a URL to read it back two lines later is work
    // that buys nothing.
    var handover by remember { mutableStateOf<Pair<Flight, String>?>(null) }
    var opening by remember { mutableStateOf(false) }

    // Choosing a site leaves the app: we do not sell tickets, and the booking
    // and the payment happen on the site somebody chose. That jump gets a
    // page of its own — see HandoverScreen — because swapping the app for a
    // stranger's checkout mid-tap is how people end up believing they bought
    // the ticket from us.
    val goToBooking: (Flight, String?) -> Unit = { flight, chosen ->
        val site = chosen ?: flight.cheapest?.first
        if (site != null) {
            handover = flight to site
            opening = false
            nav.navigate(HANDOVER)
        }
    }

    val openBooking: (Flight, String) -> Unit = { flight, site ->
        opening = true
        scope.launch {
            val url = vm.bookingUrl(flight, site)
            opening = false
            // A URL we could not build is a site we cannot send anybody to.
            // Staying put with the button live again is better than opening
            // a browser on nothing.
            if (url != null) openUrl(context, url)
        }
    }

    // Re-run the same search on a different date. Everything else about the
    // question - route, travellers, cabin - is what somebody already chose,
    // and only the date is being changed.
    val searchDate: (String, String?) -> Unit = { depart, back ->
        state.query?.let { q ->
            // A one-way calendar hands back no return date, and the query it
            // came from has none either. Keeping the existing one is what
            // makes a round-trip pick change only the half that was picked.
            val next = q.copy(departDate = depart, returnDate = back ?: q.returnDate)
            onRemember(next)
            vm.search(next)
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val here = backStack?.destination
    val route = here?.route?.substringBefore('?')
    // Where the bar belongs.
    //
    // It used to be the four tab roots only, on the argument that a bar under
    // the results invites tapping Home and losing a search that took four
    // sites to produce. Testing on a phone said the opposite: results is
    // where people spend the most time, and a screen with no bar under it
    // reads as a modal somebody is trapped in — they hunt for a way out
    // instead of using the one on screen.
    //
    // So the rule is now about the kind of screen, not the tab. Anywhere you
    // can stand and look around keeps the bar. Anywhere you walked into to
    // do one thing and leave — a filter sheet, a fare's detail, a step of the
    // search, a handover to a booking site — does not, because the bar there
    // competes with the one action the screen exists for.
    val withTabBar = setOf(SEARCH, RESULTS, CALENDAR, CHART, TRACKING, NOTIFICATIONS, HELP, ACCOUNT)
    val onTopLevel = route in withTabBar


    Scaffold(
        containerColor = Ink.canvas,
        // Zero, deliberately. Every screen but home wears QasdaAppBar, which
        // takes the status bar inset itself; home draws its photograph under
        // the clock. The bottom inset still arrives through `padding` below,
        // via the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (onTopLevel) {
                // Lifted out of the draw scope below: `Ink` is a composable
                // read, and `drawBehind` runs outside composition.
                val hairline = Ink.line
                NavigationBar(
                    containerColor = Ink.surface,
                    tonalElevation = 0.dp,
                    // 74dp with a hairline on top, as drawn. Material's
                    // default is 80 and shadowed, which floats the bar off
                    // a flat paper design.
                    modifier = Modifier
                        .drawBehind {
                            drawRect(
                                color = hairline,
                                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                            )
                        },
                ) {
                    Tab.entries.forEach { tab ->
                        // Results and the price calendar are the search tab's
                        // own pages: standing on them, Accueil is where you
                        // are, not somewhere else to go.
                        val selected = when (tab) {
                            Tab.HOME -> route == SEARCH || route == RESULTS || route == CALENDAR || route == CHART
                            else -> here?.hierarchy?.any {
                                it.route?.substringBefore('?') == tab.route
                            } == true
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // Tapping the tab you are already on is not a
                                // no-op when you are two pages inside it: from
                                // the results, Accueil means "take me back to
                                // the search", which is exactly what somebody
                                // reaches for when they want to change route
                                // rather than date.
                                // Only on a real change. Re-tapping the tab
                                // you are standing on scrolls to the top, and
                                // a tick for that would be feedback for
                                // nothing happening.
                                if (!selected || route != tab.route) {
                                    haptics.play(Feedback.Selection)
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { TabIcon(tab) },
                            label = {
                                Text(
                                    tab.label(words),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Ink.accentDeep,
                                selectedTextColor = Ink.accentDeep,
                                // No pill. Material draws a filled capsule
                                // behind the chosen glyph; the design does
                                // not, and on a bar this short the capsule
                                // reads as a button somebody left pressed.
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Ink.muted,
                                unselectedTextColor = Ink.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = SEARCH,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            // Set once, here, so every page gets the same motion and no
            // screen can quietly ship with the framework default.
            enterTransition = Motion.enter,
            exitTransition = Motion.exit,
            popEnterTransition = Motion.popEnter,
            popExitTransition = Motion.popExit,
        ) {
            // The staged search: home, then one page per answer, then a look
            // at all four before four sites are asked.
            val runSearch: () -> Unit = {
                val query = draft.toQuery()
                onRemember(query)
                vm.search(query)
                nav.navigate(RESULTS) { popUpTo(SEARCH) }
            }
            composable(SEARCH) {
                SearchScreen(
                    draft = draft,
                    recent = recent,
                    onRoundTrip = { form.roundTrip(it) },
                    onSwap = { form.swap() },
                    onPickFrom = { nav.navigate(PICK_FROM) },
                    onPickTo = { nav.navigate(PICK_TO) },
                    onPickDates = { nav.navigate(PICK_DATES) },
                    onPickTravellers = { nav.navigate(PICK_TRAVELLERS) },
                    onSearch = runSearch,
                    onLanguage = { nav.navigate(SETTINGS) },
                    onRecent = { past ->
                        // A trip whose date has gone is still a useful
                        // shortcut — the route and the passengers are right —
                        // so it fills the form and asks for a new date rather
                        // than searching a day that has passed and coming back
                        // with nothing.
                        val stillAhead = past.departDate >= java.time.LocalDate.now().toString()
                        form.load(past, keepDates = stillAhead)
                        if (stillAhead) {
                            onRemember(past)
                            vm.search(past)
                            nav.navigate(RESULTS)
                        } else {
                            nav.navigate(PICK_DATES)
                        }
                    },
                )
            }
            composable(PICK_FROM) {
                AirportStepScreen(
                    originSide = true,
                    step = 1,
                    subtitle = null,
                    onPick = { airport ->
                        form.from(airport.iata)
                        // Forward, not back: the point of a staged flow is
                        // that answering one question offers the next.
                        nav.navigate(PICK_TO) { popUpTo(SEARCH) }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(PICK_TO) {
                AirportStepScreen(
                    originSide = false,
                    step = 2,
                    subtitle = cityName(draft.from, lang),
                    onPick = { airport ->
                        form.to(airport.iata)
                        nav.navigate(PICK_DATES) { popUpTo(SEARCH) }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(PICK_DATES) {
                DatesScreen(
                    depart = draft.depart,
                    back = draft.back,
                    roundTrip = draft.roundTrip,
                    routeSubtitle = routeLine(draft, lang),
                    onPick = { d, b -> form.dates(d, b) },
                    onConfirm = { nav.navigate(PICK_TRAVELLERS) { popUpTo(SEARCH) } },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(PICK_TRAVELLERS) {
                TravellersStepScreen(
                    draft = draft,
                    subtitle = routeLine(draft, lang),
                    onApply = { a, c, i, cabin ->
                        form.travellers(a, c, i, cabin)
                        nav.navigate(REVIEW) { popUpTo(SEARCH) }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(REVIEW) {
                ReviewScreen(
                    draft = draft,
                    onEditFrom = { nav.navigate(PICK_FROM) },
                    onEditTo = { nav.navigate(PICK_TO) },
                    onEditDates = { nav.navigate(PICK_DATES) },
                    onEditTravellers = { nav.navigate(PICK_TRAVELLERS) },
                    onSearch = runSearch,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(RESULTS) {
                ResultsScreen(
                    state = state,
                    onOpen = { flight ->
                        vm.open(flight)
                        nav.navigate(DETAILS)
                    },
                    // The card's own button books the cheapest, which is the
                    // price the card is showing. Choosing a different site is
                    // what the details screen is for.
                    onBook = { goToBooking(it, null) },
                    onRetry = { vm.retry() },
                    onEdit = {
                        state.query?.let { form.load(it, keepDates = true) }
                        nav.navigate(SEARCH) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onFilters = { vm.filter(it) },
                    onSort = { vm.sortBy(it) },
                    onOpenFilters = { nav.navigate(FILTERS) },
                    onCalendar = {
                        vm.loadCalendar()
                        nav.navigate(CALENDAR)
                    },
                    onChart = {
                        vm.loadCalendar()
                        nav.navigate(CHART)
                    },
                    onPickDate = { depart, back -> searchDate(depart, back) },
                    onTrack = {
                        tracking.resetForm()
                        nav.navigate(TRACK_NEW)
                    },
                )
            }
            // The filters are a page now, not a sheet — see FiltersScreen for
            // why. It reads the unfiltered list so the counts it shows are
            // about everything the sites sent, not about what survived the
            // filters already applied.
            composable(FILTERS) {
                FiltersScreen(
                    current = state.filters,
                    flights = state.flights,
                    sort = state.sort,
                    onApply = {
                        vm.filter(it)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            // One screen, two readings. Both routes land on the same page and
            // differ only in which half of the segmented control is lit, so
            // switching between them costs nothing and neither is a dead end.
            composable(CALENDAR) {
                CalendarScreen(
                    calendar = state.calendar,
                    loading = state.calendarLoading,
                    chosenDepart = state.query?.departDate,
                    chosenReturn = state.query?.returnDate,
                    origin = state.query?.from,
                    destination = state.query?.to,
                    onPick = { depart, back ->
                        searchDate(depart, back)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(CHART) {
                CalendarScreen(
                    calendar = state.calendar,
                    loading = state.calendarLoading,
                    chosenDepart = state.query?.departDate,
                    chosenReturn = state.query?.returnDate,
                    origin = state.query?.from,
                    destination = state.query?.to,
                    onPick = { depart, back ->
                        searchDate(depart, back)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                    startOnChart = true,
                )
            }
            // The page between the app and somebody else's checkout.
            composable(HANDOVER) {
                val chosen = handover
                if (chosen == null) {
                    // Restored onto this screen with nothing behind it.
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    HandoverScreen(
                        flight = chosen.first,
                        site = chosen.second,
                        query = state.query,
                        opening = opening,
                        onOpen = { openBooking(chosen.first, chosen.second) },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            composable(DETAILS) {
                val flight = state.selected
                if (flight == null) {
                    // The selection lives in the ViewModel, so this is the
                    // process having been killed and restored onto this screen
                    // with nothing behind it. Go back rather than show an
                    // empty page.
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    DetailsScreen(
                        flight = flight,
                        onBook = { site -> goToBooking(flight, site) },
                        onBack = { nav.popBackStack() },
                        onTrack = {
                            tracking.resetForm()
                            nav.navigate(TRACK_NEW)
                        },
                    )
                }
            }
            composable(
                route = TRACKING,
                // The "manage my alerts" link out of a confirmation email
                // lands here. Declared on the destination rather than driven
                // from the activity's intent: NavHost matches it while it is
                // building its graph, so there is no window in which the tab
                // bar has moved and the screen has not.
                deepLinks = ManageHosts.map { host ->
                    navDeepLink { uriPattern = "https://$host/alerts?w={w}&s={s}" }
                },
            ) { entry ->
                // Re-read on every visit: a watch cancelled from a laptop must
                // not still be listed here.
                LaunchedEffect(Unit) {
                    val id = entry.arguments?.getString("w")
                    val signature = entry.arguments?.getString("s")
                    if (id != null && signature != null) {
                        tracking.useLink("?w=$id&s=$signature")
                    }
                    tracking.refresh()
                }
                TrackingScreen(
                    state = trackState,
                    onNotifications = { nav.navigate(NOTIFICATIONS) },
                    onOpen = { watch ->
                        tracking.open(watch)
                        nav.navigate(TRACK_HISTORY)
                    },
                    onStop = { tracking.stop(it) },
                    onNew = {
                        tracking.resetForm()
                        nav.navigate(TRACK_NEW)
                    },
                )
            }
            composable(NOTIFICATIONS) {
                NotificationsScreen(
                    state = trackState,
                    onLoad = { tracking.loadAlerts() },
                    onOpen = { alert ->
                        // Every message links back to a live search rather
                        // than restating its own number: the price it carries
                        // was true when it was observed, and re-running the
                        // question is the only thing that can say what the
                        // route costs now.
                        val query = trackState.watches.firstOrNull { it.id == alert.watchId }?.asQuery()
                        if (query != null) {
                            onRemember(query)
                            vm.search(query)
                            nav.navigate(RESULTS)
                        }
                    },
                    onBack = { nav.popBackStack() },
                    seenAt = alertsSeenAt,
                    onMarkAllRead = onAlertsSeen,
                )
            }
            composable(TRACK_NEW) {
                // A watch is about a route and a date, so it needs one. The
                // last search is the question somebody just asked; with none
                // at all there is nothing to watch and the search screen is
                // the honest place to send them.
                val query = state.query ?: recent.firstOrNull()
                if (query == null) {
                    LaunchedEffect(Unit) {
                        nav.popBackStack()
                        nav.navigate(SEARCH) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                } else {
                    TrackScreen(
                        query = query,
                        // The number that was on screen. Null when the search
                        // came back empty, which is not a missing answer — it
                        // is the other kind of watch.
                        seenPrice = state.flights.mapNotNull { it.cheapest?.second }.minOrNull()
                            ?.takeIf { state.query == query },
                        state = trackState,
                        onTrack = { seen -> tracking.track(query, seen) },
                        onDone = {
                            tracking.resetForm()
                            nav.popBackStack()
                            nav.navigate(TRACKING) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            composable(TRACK_HISTORY) {
                val watch = trackState.openWatch
                if (watch == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    PriceHistoryScreen(
                        watch = watch,
                        trend = trackState.trend,
                        loading = trackState.trendLoading,
                        onSearch = {
                            val query = watch.asQuery()
                            onRemember(query)
                            vm.search(query)
                            tracking.closeWatch()
                            nav.navigate(RESULTS)
                        },
                        onStop = {
                            tracking.stop(watch.id)
                            tracking.closeWatch()
                            nav.popBackStack()
                        },
                        onBack = {
                            tracking.closeWatch()
                            nav.popBackStack()
                        },
                    )
                }
            }
            composable(HELP) {
                HelpScreen(onContact = { openUrl(context, "${BuildConfig.API_BASE}/${lang.tag}/") })
            }
            composable(ACCOUNT) {
                AccountScreen(
                    lang = lang,
                    chosenLang = chosenLang,
                    versionName = BuildConfig.VERSION_NAME,
                    activeWatches = trackState.watches.size,
                    notifications = trackState.alerts.size,
                    searches = recent.size,
                    homeAirport = homeAirport,
                    onSettings = { nav.navigate(SETTINGS) },
                    onHomeAirport = { nav.navigate(PICK_HOME) },
                    onAbout = { nav.navigate(ABOUT) },
                    onOpen = { path -> openUrl(context, BuildConfig.API_BASE + path) },
                )
            }
            // The same picker the search uses, answering a different
            // question. Rebuilding it here would be a second list of Algerian
            // airports to keep in step with the first.
            composable(PICK_HOME) {
                AirportStepScreen(
                    originSide = true,
                    step = 0,
                    subtitle = null,
                    onPick = { airport ->
                        onHomeAirport(airport.iata)
                        form.from(airport.iata)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(SETTINGS) {
                SettingsScreen(
                    current = chosenLang,
                    effective = lang,
                    versionName = BuildConfig.VERSION_NAME,
                    onLanguage = onLang,
                    onOpen = { path -> openUrl(context, BuildConfig.API_BASE + path) },
                    onAbout = { nav.navigate(ABOUT) },
                    onBack = { nav.popBackStack() },
                    alertDrops = alertPrefs.drops,
                    alertSeats = alertPrefs.seats,
                    alertEnded = alertPrefs.ended,
                    onAlertDrops = { onAlertPrefs(alertPrefs.copy(drops = it)) },
                    onAlertSeats = { onAlertPrefs(alertPrefs.copy(seats = it)) },
                    onAlertEnded = { onAlertPrefs(alertPrefs.copy(ended = it)) },
                    recentCount = recent.size,
                    watchCount = trackState.watches.size,
                    onClearRecent = onClearRecent,
                    onStopAllWatches = { tracking.stopAll() },
                    themeMode = themeMode,
                    onThemeMode = onThemeMode,
                )
            }
            composable(ABOUT) {
                AboutScreen(
                    lang = lang,
                    versionName = BuildConfig.VERSION_NAME,
                    onOpen = { path -> openUrl(context, BuildConfig.API_BASE + path) },
                    onFaq = {
                        nav.navigate(HELP) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
