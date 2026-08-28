package pro.qasdatrip.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pro.qasdatrip.app.BuildConfig
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.QasdaApi
import pro.qasdatrip.core.Words

private const val SEARCH = "search"
private const val RESULTS = "results"
private const val DETAILS = "details"
private const val HELP = "help"
private const val SETTINGS = "settings"

/** The destinations the bar can reach. Results and details are inside the search one. */
private enum class Tab(val route: String, val label: (Words) -> String) {
    HOME(SEARCH, { it.navHome }),
    HELP_TAB(HELP, { it.navHelp }),
    SETTINGS_TAB(SETTINGS, { it.navSettings }),
}

@Composable
private fun TabIcon(tab: Tab) = when (tab) {
    Tab.HOME -> Icon(Icons.Filled.Home, contentDescription = null)
    // Material core ships Info but no question mark, and this tab is help
    // rather than about.
    Tab.HELP_TAB -> Icon(painterResource(R.drawable.ic_help), contentDescription = null)
    Tab.SETTINGS_TAB -> Icon(Icons.Filled.Settings, contentDescription = null)
}

@Composable
fun QasdaNavHost(
    api: QasdaApi,
    lang: Lang,
    chosenLang: Lang?,
    onLang: (Lang?) -> Unit,
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

    val backStack by nav.currentBackStackEntryAsState()
    val here = backStack?.destination
    // The bar belongs to the three places somebody can be, not to the pages
    // they walk into from there. Results with a bar under it invites tapping
    // Home and losing a search that took four sites to produce.
    val onTopLevel = Tab.entries.any { tab -> here?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        containerColor = Ink.canvas,
        bottomBar = {
            if (onTopLevel) {
                NavigationBar(containerColor = Ink.surface) {
                    Tab.entries.forEach { tab ->
                        val selected = here?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { TabIcon(tab) },
                            label = { Text(tab.label(words)) },
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
                SearchScreen(onSearch = { query ->
                    vm.search(query)
                    nav.navigate(RESULTS)
                })
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
                    )
                }
            }
            composable(HELP) {
                HelpScreen(onContact = { openUrl(context, "${BuildConfig.API_BASE}/${lang.tag}/") })
            }
            composable(SETTINGS) {
                SettingsScreen(
                    current = chosenLang,
                    effective = lang,
                    versionName = BuildConfig.VERSION_NAME,
                    onLanguage = onLang,
                    onOpen = { path -> openUrl(context, BuildConfig.API_BASE + path) },
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
