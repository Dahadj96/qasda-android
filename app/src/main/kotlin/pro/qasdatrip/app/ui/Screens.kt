package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Filters
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.FlightList
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchEvent
import pro.qasdatrip.core.SortBy

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
    onFilters: (Filters) -> Unit = {},
    onSort: (SortBy) -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    var sheetOpen by remember { mutableStateOf(false) }

    // What the list is showing, after the narrowing and the ordering. The
    // unfiltered list stays in state so the sheet can count what a change
    // would leave behind without touching what is on screen.
    val shown = FlightList.apply(state.flights, state.filters, state.sort)

    if (sheetOpen) {
        FilterSheet(
            current = state.filters,
            priceCeiling = state.flights.mapNotNull { it.cheapest?.second }.maxOrNull(),
            matchCount = { candidate -> FlightList.apply(state.flights, candidate, state.sort).size },
            onDismiss = { sheetOpen = false },
            onApply = { chosen ->
                onFilters(chosen)
                sheetOpen = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        SearchSummaryBar(state, onEdit)
        OfflineBanner(modifier = Modifier.padding(horizontal = Space.s4, vertical = Space.s2))

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

            // Filters that match nothing is not the same answer as a route
            // with no flights: the flights are there, the question was too
            // narrow. Saying "no flights on this route" here would be false.
            shown.isEmpty() -> Message(
                title = words.noMatchTitle,
                body = words.noMatchSub,
                actionLabel = words.reset,
                onAction = { onFilters(Filters()) },
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
                            words.resultsCount.replace("{n}", Money.isolate(shown.size.toString())),
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
                item {
                    ControlsRow(
                        filters = state.filters,
                        sort = state.sort,
                        onOpenFilters = { sheetOpen = true },
                        onSort = onSort,
                    )
                }
                items(shown, key = { it.id ?: it.hashCode().toString() }) { flight ->
                    FlightCard(flight, onOpen = { onOpen(flight) }, onBook = { onBook(flight) })
                }
            }
        }
    }
}

/**
 * Filters on the left, ordering on the right. The filter button carries the
 * number of rules in force, because a list that has been narrowed looks
 * exactly like a list that is short.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsRow(
    filters: Filters,
    sort: SortBy,
    onOpenFilters: () -> Unit,
    onSort: (SortBy) -> Unit,
) {
    val words = LocalWords.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Pill(
            label = if (filters.isEmpty) words.filters else "${words.filters} · ${filters.active}",
            on = !filters.isEmpty,
            onClick = onOpenFilters,
        )
        listOf(
            SortBy.PRICE to words.sortPrice,
            SortBy.DEPARTURE to words.sortDeparture,
            SortBy.DURATION to words.sortDuration,
        ).forEach { (option, label) ->
            Pill(label = label, on = option == sort) { onSort(option) }
        }
    }
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (on) Ink.accentDeep else Ink.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (on) Ink.accentSoft else Ink.surface)
            .border(1.dp, if (on) Ink.accentUi else Ink.lineStrong, RoundedCornerShape(Radius.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s3, vertical = Space.s2),
    )
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
