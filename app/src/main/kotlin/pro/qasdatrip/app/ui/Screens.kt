package pro.qasdatrip.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Filters
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.FlightList
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.TimeBand
import pro.qasdatrip.core.Words
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
    onCalendar: () -> Unit = {},
    onPickDate: (depart: String, back: String?) -> Unit = { _, _ -> },
    onTrack: () -> Unit = {},
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
        // A bar with no percentage on it. The server deliberately does not say
        // which sites have answered, so any number here would be invented; what
        // it can honestly report is that the search is still going.
        //
        // Only once there is a list to grow. Before the first offer arrives the
        // whole screen is the wait, and that is drawn as the route instead.
        if (state.running && shown.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Ink.accentDeep,
                trackColor = Ink.line,
            )
        }
        OfflineBanner(modifier = Modifier.padding(horizontal = Space.s4, vertical = Space.s2))

        // The rail stays put while the list scrolls. Filters that scroll away
        // with the results are filters people stop finding, and the count
        // beside them is the one number worth keeping on screen.
        if (state.failed == null && !state.empty) {
            ControlsRow(
                filters = state.filters,
                onOpenFilters = { sheetOpen = true },
                onFilters = onFilters,
                onCalendar = onCalendar,
                onTrack = onTrack,
            )
            // Nothing to count and nothing to order until the first offer is
            // in. Left in, it repeated the loading screen's own caption a
            // finger's width above it.
            if (shown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Space.s3))
                SortRow(shown = shown, running = state.running, sort = state.sort, onSort = onSort)
            }
            Spacer(modifier = Modifier.height(Space.s2))
        }

        when {
            state.failed != null -> Message(
                title = if (state.failed == SearchEvent.Reason.CONNECTION) words.failedTitle else words.failedTitle,
                body = words.failedSub,
                actionLabel = words.retry,
                onAction = onRetry,
            )

            // Nothing on this date is the moment the calendar is worth most:
            // the route may be perfectly well served two days either side,
            // and the search that just failed cannot say so.
            state.empty -> EmptyWithNearby(
                state = state,
                lang = lang,
                onCalendar = onCalendar,
                onPickDate = onPickDate,
            )

            // Still running and nothing through yet. This case used to fall
            // into "no flight matches your filters", which is a lie told for
            // the three seconds before the first site answers.
            state.running && shown.isEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Space.s4, end = Space.s4, bottom = Space.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The plane crossing the route is the only honest progress
                // this screen can show, and it is the difference between
                // "working" and "frozen" on a slow connection.
                item {
                    RouteLoading(
                        origin = cityName(state.query?.from, lang),
                        destination = cityName(state.query?.to, lang),
                        offersSoFar = state.flights.size,
                        modifier = Modifier.padding(vertical = Space.s3),
                    )
                }
                items(SkeletonCount) { SkeletonCard() }
            }

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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Space.s4, end = Space.s4, bottom = Space.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The position is part of the key on purpose. Two sites can hand
                // back the same flight id for the same leg, and a LazyColumn throws
                // if a key repeats — a duplicate upstream must never crash the list.
                itemsIndexed(
                    shown,
                    key = { index, flight -> "$index:${flight.id ?: flight.hashCode()}" },
                ) { index, flight ->
                    FlightCard(
                        flight = flight,
                        onOpen = { onOpen(flight) },
                        onBook = { onBook(flight) },
                        // Only when the list is ordered by price. Top of a
                        // list sorted by departure time is the earliest
                        // flight, and calling that the best price would be
                        // a lie the border tells loudly.
                        best = index == 0 && state.sort == SortBy.PRICE && !state.running,
                    )
                }
                // The list keeps its length honest while it grows: two grey
                // cards at the bottom say more is coming without pretending
                // to know what it will be.
                if (state.running) {
                    items(2) { SkeletonCard() }
                }
            }
        }
    }
}

private const val SkeletonCount = 4

/**
 * A card-shaped absence.
 *
 * It carries no numbers on purpose. A skeleton that shows a plausible price
 * teaches people to read the shape before the data arrives, and the first
 * real price then looks like a change rather than an answer.
 */
@Composable
private fun SkeletonCard() {
    val shimmer = rememberInfiniteTransition(label = "skeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        // The circle is the airline mark's circle, at the same 36dp, so the
        // row does not jump sideways when the real card replaces this one.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Ink.line.copy(alpha = alpha)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Bone(widthFraction = 0.6f, alpha = alpha)
                Bone(widthFraction = 0.4f, alpha = alpha, height = 10.dp)
            }
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 18.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Ink.line.copy(alpha = alpha)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 22.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Ink.line.copy(alpha = alpha)),
            )
            Box(modifier = Modifier.weight(1f).height(2.dp).background(Ink.line.copy(alpha = alpha)))
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 22.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Ink.line.copy(alpha = alpha)),
            )
        }
        Bone(widthFraction = 0.5f, alpha = alpha, height = 20.dp)
    }
}

@Composable
private fun Bone(widthFraction: Float, alpha: Float, height: androidx.compose.ui.unit.Dp = 14.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(Radius.sm))
            .background(Ink.line.copy(alpha = alpha)),
    )
}

/**
 * The chip rail under the app bar: everything that changes what the list
 * shows, in one horizontal scroll.
 *
 * The two most-wanted narrowings — direct flights, and a morning departure —
 * are chips of their own rather than rules buried in the sheet, because they
 * are the two people reach for and neither deserves a modal. The sheet is
 * still there for everything else, and its chip carries the count: a list
 * that has been narrowed looks exactly like a list that is short.
 *
 * Ordering is not here. Sorting does not remove anything and narrowing does,
 * and a row that mixes them invites reading "Price" as "cheap flights only".
 */
@Composable
private fun ControlsRow(
    filters: Filters,
    onOpenFilters: () -> Unit,
    onFilters: (Filters) -> Unit,
    onCalendar: () -> Unit = {},
    onTrack: () -> Unit = {},
) {
    val words = LocalWords.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        // Two equal controls, side by side and full width, exactly as the
        // mobile site has them. They were chips in a scrolling rail before,
        // which put the price calendar — the thing that answers "is this a
        // bad day to fly?" — off the right-hand edge of the screen where
        // nobody found it.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            ToolButton(
                label = if (filters.isEmpty) words.filters
                else "${words.filters} · ${Money.isolate(filters.active.toString())}",
                icon = R.drawable.ic_sliders,
                on = !filters.isEmpty,
                onClick = onOpenFilters,
                modifier = Modifier.weight(1f),
            )
            ToolButton(
                label = words.priceCalendar,
                icon = R.drawable.ic_calendar,
                on = false,
                onClick = onCalendar,
                modifier = Modifier.weight(1f),
            )
        }
        // Underneath, the two narrowings people actually reach for, and the
        // one thing you can do with a route rather than to a list.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chip(
                label = words.direct,
                on = filters.maxStops == 0,
                onClick = { onFilters(filters.copy(maxStops = if (filters.maxStops == 0) null else 0)) },
            )
            Chip(
                label = words.morning,
                on = filters.departBands == setOf(TimeBand.MORNING),
                onClick = {
                    val already = filters.departBands == setOf(TimeBand.MORNING)
                    onFilters(filters.copy(departBands = if (already) emptySet() else setOf(TimeBand.MORNING)))
                },
            )
            Chip(label = words.trackRoute, on = false, onClick = onTrack)
        }
    }
}

/** Full width, 44 tall, icon then label — the pair above the results list. */
@Composable
private fun ToolButton(
    label: String,
    @androidx.annotation.DrawableRes icon: Int,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (on) Ink.accentSoft else Ink.surface)
            .border(1.dp, if (on) Ink.accentUi else Ink.lineStrong, RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s2),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (on) Ink.accentDeep else Ink.ink,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (on) Ink.accentDeep else Ink.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 32dp tall, 8dp corners, 13sp medium — the chip from the design system. */
@Composable
private fun Chip(
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    @androidx.annotation.DrawableRes icon: Int? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(if (on) Ink.accentSoft else Ink.surface)
            .border(1.dp, if (on) Ink.accentUi else Ink.lineStrong, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = if (on) Ink.accentDeep else Ink.ink,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (on) Ink.accentDeep else Ink.ink,
            maxLines = 1,
        )
    }
}

/**
 * How many offers, from how many airlines, and what the list is ordered by.
 *
 * The airline count is the honest measure of a comparison: forty offers from
 * two carriers is a thinner answer than twelve from six, and only one of
 * those numbers says so.
 */
@Composable
private fun SortRow(
    shown: List<Flight>,
    running: Boolean,
    sort: SortBy,
    onSort: (SortBy) -> Unit,
) {
    val words = LocalWords.current
    var open by remember { mutableStateOf(false) }

    val airlines = shown.mapNotNull { flight ->
        (flight.outbound?.operatingAirline ?: flight.airline)?.takeIf { it.isNotBlank() }
    }.toSet().size

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildString {
                append(
                    if (running) words.comparing
                    else words.offersSoFar.replace("{n}", Money.isolate(shown.size.toString()))
                )
                if (!running && airlines > 0) {
                    append(" · ")
                    append(
                        if (airlines == 1) words.airlineOne
                        else words.airlinesMany.replace("{n}", Money.isolate(airlines.toString()))
                    )
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = Ink.muted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable { open = true }
                    .padding(horizontal = Space.s2, vertical = Space.s1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    tint = Ink.accentDeep,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    labelFor(sort, words),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink.accentDeep,
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SortBy.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                labelFor(option, words),
                                color = if (option == sort) Ink.accentDeep else Ink.ink,
                            )
                        },
                        onClick = { onSort(option); open = false },
                    )
                }
            }
        }
    }
}

private fun labelFor(sort: SortBy, words: Words): String = when (sort) {
    SortBy.PRICE -> words.sortPrice
    SortBy.DEPARTURE -> words.sortDeparture
    SortBy.DURATION -> words.sortDuration
}

/**
 * The app bar the design draws: back, the route with the trip under it in
 * one grey line, and "Edit" as a text link.
 *
 * It sits on the canvas rather than on white. A white bar over a white list
 * needs a rule to separate them and then reads as two surfaces; on the paper
 * ground the cards are the only white on screen, which is what makes them
 * look like cards.
 */
@Composable
private fun SearchSummaryBar(state: SearchViewModel.State, onEdit: () -> Unit) {
    val lang = LocalLang.current
    val words = LocalWords.current
    val q = state.query ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Ink.ink,
            modifier = Modifier.size(22.dp).clickable(onClick = onEdit),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${cityName(q.from, lang)} ${routeArrow(lang)} ${cityName(q.to, lang)}",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Dates, travellers and cabin on one grey line: three answers
            // somebody already gave, worth confirming and not worth a row
            // each.
            Text(
                tripLine(q, lang, words),
                style = MaterialTheme.typography.labelMedium,
                color = Ink.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // "Search" on this control meant "go back and change the search",
        // which is not what the word says.
        Text(
            words.edit,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Ink.accentDeep,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.sm))
                .clickable(onClick = onEdit)
                .padding(horizontal = Space.s2, vertical = Space.s1),
        )
    }
}

private fun tripLine(q: SearchQuery, lang: pro.qasdatrip.core.Lang, words: Words): String {
    val dates = listOfNotNull(q.departDate, q.returnDate).joinToString(" – ") { formatDate(it, lang) }
    val travellers = if (q.travellers == 1) {
        words.travellerOne
    } else {
        words.travellersMany.replace("{n}", Money.isolate(q.travellers.toString()))
    }
    val cabin = when (q.cabin) {
        Cabin.ECONOMY -> words.economy
        Cabin.PREMIUM -> words.premium
        Cabin.BUSINESS -> words.business
        Cabin.FIRST -> words.first
    }
    return listOf(dates, travellers, cabin).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * No flights on the chosen date - and, when the calendar has been fetched,
 * the days either side that do have them.
 *
 * This is the screen where the calendar earns its keep. "No flights ALG to
 * CDG" is true of the date and says nothing about the route, and somebody
 * reading it has no way to tell which they are looking at.
 *
 * The calendar is not fetched automatically: it costs the server the better
 * part of a minute against four sites, and doing it behind every empty
 * search would spend that on people who are about to change the route
 * instead. The button asks.
 */
@Composable
private fun EmptyWithNearby(
    state: SearchViewModel.State,
    lang: pro.qasdatrip.core.Lang,
    onCalendar: () -> Unit,
    onPickDate: (String, String?) -> Unit,
) {
    val words = LocalWords.current
    val nearby = state.calendar?.pricedDays.orEmpty()
        .filter { it.departDate != state.query?.departDate }

    Column(
        modifier = Modifier.fillMaxSize().padding(Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            words.noResultsTitle
                .replace("{from}", cityName(state.query?.from, lang))
                .replace("{to}", cityName(state.query?.to, lang)),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            words.noResultsSub,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.muted,
            textAlign = TextAlign.Center,
        )

        when {
            state.calendarLoading -> {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Ink.accentDeep)
                Text(
                    words.calendarLoading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    textAlign = TextAlign.Center,
                )
            }

            nearby.isNotEmpty() -> {
                Text(
                    words.nearbyDates,
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                    modifier = Modifier.fillMaxWidth(),
                )
                nearby.take(4).forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Ink.surface)
                            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                            .clickable { onPickDate(day.departDate, day.returnDate) }
                            .padding(Space.s4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDate(day.departDate, lang), style = MaterialTheme.typography.titleMedium)
                        Text(
                            Money.format(day.price ?: 0.0, lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink.accentDeep,
                        )
                    }
                }
            }

            else -> Button(
                onClick = onCalendar,
                shape = RoundedCornerShape(Radius.sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                ),
            ) { Text(words.priceCalendar) }
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
