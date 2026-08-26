package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchEvent

/**
 * Results, as they arrive.
 *
 * A site that has not answered yet is not a site with no flights, so the list
 * grows while the search runs rather than waiting for the slowest of four.
 */
@Composable
fun ResultsScreen(
    state: SearchViewModel.State,
    onOpen: (Flight) -> Unit,
    onBook: (Flight) -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        SearchSummaryBar(state, onEdit)

        when {
            state.failed != null -> Message(
                title = if (state.failed == SearchEvent.Reason.CONNECTION) words.failedTitle else words.failedTitle,
                body = words.failedSub,
                actionLabel = words.retry,
                onAction = onRetry,
            )

            state.empty -> Message(
                title = words.noResultsTitle
                    .replace("{from}", cityName(state.query?.from, lang))
                    .replace("{to}", cityName(state.query?.to, lang)),
                body = words.noResultsSub,
                actionLabel = null,
                onAction = {},
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Space.s4),
                verticalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            words.resultsCount.replace("{n}", Money.isolate(state.flights.size.toString())),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.running) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(start = Space.s3),
                                strokeWidth = 2.dp,
                                color = Ink.accentDeep,
                            )
                        }
                    }
                }
                items(state.flights, key = { it.id ?: it.hashCode().toString() }) { flight ->
                    FlightCard(flight, onOpen = { onOpen(flight) }, onBook = { onBook(flight) })
                }
            }
        }
    }
}

@Composable
private fun SearchSummaryBar(state: SearchViewModel.State, onEdit: () -> Unit) {
    val lang = LocalLang.current
    val q = state.query ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.surface)
            .padding(Space.s4),
    ) {
        Text(
            "${cityName(q.from, lang)} → ${cityName(q.to, lang)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Money.isolate(listOfNotNull(q.departDate, q.returnDate).joinToString(" – ")),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(Radius.sm)) {
                Text(LocalWords.current.search, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Message(title: String, body: String, actionLabel: String?, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(Space.s6), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.muted, textAlign = TextAlign.Center)
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(Radius.sm),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
                ) { Text(actionLabel) }
            }
        }
    }
}

fun cityName(iata: String?, lang: pro.qasdatrip.core.Lang): String =
    iata?.let { Airports.byIata(it)?.cityIn(lang) ?: it }.orEmpty()
