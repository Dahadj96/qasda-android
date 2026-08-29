package pro.qasdatrip.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.ManageKey
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.Words

private const val SEARCH = "search"
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

/**
 * The hosts a manage link can arrive from. dev is here because that is where
 * the app points today; the other two are here so a link still works after
 * the apex domain goes live and an old email is opened a month later.
 */
private val ManageHosts = listOf("dev.qasdatrip.pro", "qasdatrip.pro", "www.qasdatrip.pro")

/** The destinations the bar can reach. Results and details are inside the search one. */
private enum class Tab(val route: String, val label: (Words) -> String) {
    HOME(SEARCH, { it.navHome }),
    TRACKING_TAB(TRACKING, { it.navTracking }),
    HELP_TAB(HELP, { it.navHelp }),
    // Compte, not Réglages. Nobody opens an app to visit its settings; they
    // open it to see what is theirs. Réglages is a row inside this one.
    ACCOUNT_TAB(ACCOUNT, { it.navAccount }),
}

@Composable
private fun TabIcon(tab: Tab) = when (tab) {
    Tab.HOME -> Icon(Icons.Filled.Home, contentDescription = null)
    Tab.TRACKING_TAB -> Icon(Icons.Filled.Notifications, contentDescription = null)
    // Material core ships Info but no question mark, and this tab is help
    // rather than about.
    Tab.HELP_TAB -> Icon(painterResource(R.drawable.ic_help), contentDescription = null)
    Tab.ACCOUNT_TAB -> Icon(Icons.Filled.Person, contentDescription = null)
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
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val words = LocalWords.current

    val vm: SearchViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(api) as T
    })
    val state by vm.state.collectAsStateWithLifecycle()

    val tracking: TrackingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackingViewModel(api, readKey = { manageKey }, writeKey = onManageKey) as T
    })
    val trackState by tracking.state.collectAsStateWithLifecycle()


    // Choosing a site leaves the app: we do not sell tickets, and the booking
    // and the payment happen on the site somebody chose.
    val openBooking: (Flight, String?) -> Unit = { flight, chosen ->
        val site = chosen ?: flight.cheapest?.first
        if (site != null) {
            scope.launch {
                vm.bookingUrl(flight, site)?.let { url -> openUrl(context, url) }
            }
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
    val withTabBar = setOf(SEARCH, RESULTS, CALENDAR, TRACKING, HELP, ACCOUNT)
    val onTopLevel = route in withTabBar


    Scaffold(
        containerColor = Ink.canvas,
        bottomBar = {
            if (onTopLevel) {
                NavigationBar(
                    containerColor = Ink.surface,
                    tonalElevation = 0.dp,
                    // 74dp with a hairline on top, as drawn. Material's
                    // default is 80 and shadowed, which floats the bar off
                    // a flat paper design.
                    modifier = Modifier
                        .drawBehind {
                            drawRect(
                                color = Ink.line,
                                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                            )
                        },
                ) {
                    Tab.entries.forEach { tab ->
                        // Results and the price calendar are the search tab's
                        // own pages: standing on them, Accueil is where you
                        // are, not somewhere else to go.
                        val selected = when (tab) {
                            Tab.HOME -> route == SEARCH || route == RESULTS || route == CALENDAR
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
                                if (!selected || route != tab.route) {
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
                                indicatorColor = Ink.accentSoft,
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
            modifier = Modifier.padding(padding),
        ) {
            composable(SEARCH) {
                SearchScreen(
                    recent = recent,
                    onSearch = { query ->
                        onRemember(query)
                        vm.search(query)
                        nav.navigate(RESULTS)
                    },
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
                    onBook = { openBooking(it, null) },
                    onRetry = { vm.retry() },
                    onEdit = { nav.popBackStack() },
                    onFilters = { vm.filter(it) },
                    onSort = { vm.sortBy(it) },
                    onCalendar = {
                        vm.loadCalendar()
                        nav.navigate(CALENDAR)
                    },
                    onPickDate = { depart, back -> searchDate(depart, back) },
                    onTrack = {
                        tracking.resetForm()
                        nav.navigate(TRACK_NEW)
                    },
                )
            }
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
                        onBook = { site -> openBooking(flight, site) },
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
                    onOpen = { watch ->
                        tracking.open(watch)
                        nav.navigate(TRACK_HISTORY)
                    },
                    onStop = { tracking.stop(it) },
                    onLink = { url -> tracking.useLink(url) },
                    onNew = {
                        tracking.resetForm()
                        nav.navigate(TRACK_NEW)
                    },
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
                        currentCheapest = state.flights.mapNotNull { it.cheapest?.second }.minOrNull()
                            ?.takeIf { state.query == query },
                        state = trackState,
                        onCreate = { email, target ->
                            tracking.create(query, email, lang.tag, target)
                        },
                        onDone = {
                            tracking.resetForm()
                            nav.popBackStack()
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
                    onSettings = { nav.navigate(SETTINGS) },
                    onAbout = { nav.navigate(ABOUT) },
                    onOpen = { path -> openUrl(context, BuildConfig.API_BASE + path) },
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
