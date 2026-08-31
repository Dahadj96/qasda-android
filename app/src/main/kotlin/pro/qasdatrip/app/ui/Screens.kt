package pro.qasdatrip.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Filters
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.FlightList
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow
import pro.qasdatrip.core.SearchQuery
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
    onOpenFilters: () -> Unit = {},
    onPickDate: (depart: String, back: String?) -> Unit = { _, _ -> },
    onTrack: () -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    // What the list is showing, after the narrowing and the ordering. The
    // unfiltered list stays in state so the sheet can count what a change
    // would leave behind without touching what is on screen.
    val shown = FlightList.apply(state.flights, state.filters, state.sort)

    // How much of the header is showing.
    //
    // The block above the list is four things stacked - the route, three
    // controls, the carriers, and the count - and together they ate a third
    // of the screen on every scroll. It now folds as soon as the list starts
    // moving down and comes back the moment it moves up, which is the
    // gesture people already make when they want the controls again.
    //
    // Driven by the scroll delta rather than by position, so it answers the
    // direction of the finger instead of where the list happens to be.
    var expanded by remember { mutableStateOf(true) }
    val folding = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A dead band, so a thumb resting on the glass does not
                // flutter the header open and shut.
                if (available.y < -6f) expanded = false
                if (available.y > 6f) expanded = true
                return Offset.Zero
            }
        }
    }

    // How many of the answers are on screen. A search can come back with
    // several hundred, and a list that long is not a list, it is a scroll
    // with no end - people give up rather than reach the bottom.
    var showing by remember(state.query, state.filters, state.sort) { mutableStateOf(PageSize) }
    val page = shown.take(showing)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.canvas)
            .nestedScroll(folding),
    ) {
        SearchSummaryBar(state, onEdit, expanded = expanded)
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
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(Motion.sizeDefault) + fadeIn(Motion.effectsFast),
                exit = shrinkVertically(Motion.sizeDefault) + fadeOut(Motion.effectsFast),
            ) {
                Column {
                    ControlsRow(
                        filters = state.filters,
                        onOpenFilters = onOpenFilters,
                        onFilters = onFilters,
                        onCalendar = onCalendar,
                    )
                    // The carriers, above the count, computed from everything
                    // the sites sent rather than from what is currently on
                    // screen — see FlightList.airlinesOn for why that matters.
                    if (shown.isNotEmpty() || !state.running) {
                        Spacer(modifier = Modifier.height(Space.s3))
                        AirlineRail(
                            airlines = FlightList.airlinesOn(state.flights) { Airlines.name(it) },
                            selected = state.filters.airlines,
                            onSelect = { picked -> onFilters(state.filters.copy(airlines = picked)) },
                        )
                    }
                }
            }
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
                title = words.failedTitle,
                body = words.failedSub,
                actionLabel = words.retry,
                onAction = onRetry,
                icon = R.drawable.ic_plane_off,
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
                    page,
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

                // The rest, on request. The count is on the button because
                // "more" alone gives no sense of how much is left, and a
                // list that ends without saying so reads as a list that
                // failed to load the rest.
                val remaining = shown.size - page.size
                if (remaining > 0 && !state.running) {
                    item {
                        MoreButton(remaining = remaining, onClick = { showing += PageSize })
                    }
                }
            }
        }
    }
}

private const val SkeletonCount = 4

/**
 * How many offers arrive at once.
 *
 * Ten is roughly two screens: enough that scrolling feels worthwhile, few
 * enough that the end is reachable. Every press adds another ten rather than
 * everything at once, so a search that came back with three hundred never
 * builds three hundred cards.
 */
private const val PageSize = 10

@Composable
private fun MoreButton(remaining: Int, onClick: () -> Unit) {
    val words = LocalWords.current
    val haptics = LocalHaptics.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.s2)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surface)
            .border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.pill))
            .clickable {
                haptics.play(Feedback.Selection)
                onClick()
            }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (remaining == 1) words.showMoreOne
            else words.showMore.replace("{n}", Money.isolate(remaining.toString())),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Ink.accentDeep,
        )
    }
}

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
    onFilters: (Filters) -> Unit = {},
    onCalendar: () -> Unit = {},
) {
    val words = LocalWords.current
    val haptics = LocalHaptics.current
    // Three equal controls, side by side and full width, exactly as drawn.
    //
    // There used to be a second row of loose chips under this one — Direct,
    // Matin, and a "track this route" that had no business sitting among
    // filters. They were a shortcut to two of the choices the filters page
    // already offers, drawn in a different shape, so the same question had
    // two answers on one screen and neither showed the other's state. The
    // page is one tap away and shows all of them at once.
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
            label = words.calendarTab,
            icon = R.drawable.ic_calendar,
            on = false,
            onClick = onCalendar,
            modifier = Modifier.weight(1f),
        )
        // Direct, in the third slot, because it was the one filter people
        // reach for on every search and it was two taps deep.
        //
        // What used to be here was a second door to the same room: the chart
        // and the calendar are two views of one page, and that page already
        // has a control to switch between them. Two buttons that open the
        // same screen is a menu that has not been read back.
        val direct = filters.maxStops == 0
        ToolButton(
            label = words.direct,
            icon = R.drawable.ic_plane_right,
            on = direct,
            onClick = {
                haptics.play(if (direct) Feedback.ToggleOff else Feedback.ToggleOn)
                onFilters(filters.copy(maxStops = if (direct) null else 0))
            },
            modifier = Modifier.weight(1f),
        )
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
            // A bordered pill, as drawn. Bare text with an icon read as a
            // label rather than a control, and the one complaint people had
            // about this row was not knowing the ordering could be changed.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Ink.surface)
                    .border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.pill))
                    .clickable { open = true }
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "${words.sortBy} : ${labelFor(sort, words).lowercase()}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink.ink,
                    maxLines = 1,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = Ink.inkSoft,
                    modifier = Modifier.size(14.dp),
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
private fun SearchSummaryBar(
    state: SearchViewModel.State,
    onEdit: () -> Unit,
    expanded: Boolean = true,
) {
    val lang = LocalLang.current
    val words = LocalWords.current
    val q = state.query ?: return
    // The shared bar, not a hand-rolled one. This screen used to draw its own
    // 22dp arrow on the canvas while every other screen wore the 40dp circle,
    // so the most-used back button in the app was the smallest and the least
    // like the rest of it.
    QasdaAppBar(
        title = "${cityName(q.from, lang)} ${routeArrow(lang)} ${cityName(q.to, lang)}",
        // Dates, travellers and cabin on one grey line: three answers
        // somebody already gave, worth confirming and not worth a row each.
        // The grey line folds away with the rest of the header once the
        // list is moving: by then somebody is reading prices, not checking
        // the dates they just chose.
        subtitle = tripLine(q, lang, words).takeIf { expanded },
        onBack = onEdit,
        // A pencil, not the word. See QasdaAppBar's actionIcon.
        actionLabel = words.edit,
        actionIcon = R.drawable.ic_edit,
        onAction = onEdit,
    )
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
                    containerColor = Ink.solid,
                    contentColor = Ink.onSolid,
                ),
            ) { Text(words.priceCalendar) }
        }
    }
}

@Composable
private fun Message(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    @androidx.annotation.DrawableRes icon: Int? = null,
) {
    Box(modifier = Modifier.fillMaxSize().padding(Space.s6), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            // A tinted disc above the words, as drawn. A wall of centred text
            // on an otherwise empty screen reads as an error the app has not
            // finished writing; a mark at the top of it reads as a state the
            // app meant to show.
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Ink.alertSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = Ink.alert,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.muted, textAlign = TextAlign.Center)
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.solid, contentColor = Ink.onSolid),
                ) { Text(actionLabel, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

fun cityName(iata: String?, lang: pro.qasdatrip.core.Lang): String =
    iata?.let { Airports.byIata(it)?.cityIn(lang) ?: it }.orEmpty()
