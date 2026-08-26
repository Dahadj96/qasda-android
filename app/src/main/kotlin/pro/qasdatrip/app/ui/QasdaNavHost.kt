package pro.qasdatrip.app.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.QasdaApi

private const val SEARCH = "search"
private const val RESULTS = "results"

@Composable
fun QasdaNavHost(api: QasdaApi, lang: Lang, onLang: (Lang) -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vm: SearchViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(api) as T
    })
    val state by vm.state.collectAsStateWithLifecycle()

    // Choosing a site leaves the app: we do not sell tickets, and the booking
    // and the payment happen on the site somebody chose.
    val openBooking: (Flight) -> Unit = { flight ->
        val site = flight.cheapest?.first
        if (site != null) {
            scope.launch {
                vm.bookingUrl(flight, site)?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            }
        }
    }

    NavHost(navController = nav, startDestination = SEARCH) {
        composable(SEARCH) {
            SearchScreen(onSearch = { query ->
                vm.search(query)
                nav.navigate(RESULTS)
            })
        }
        composable(RESULTS) {
            ResultsScreen(
                state = state,
                onOpen = { openBooking(it) },     // the details screen is the next one to build
                onBook = { openBooking(it) },
                onRetry = { vm.retry() },
                onEdit = { nav.popBackStack() },
            )
        }
    }
}
