package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.CalendarCell
import pro.qasdatrip.core.FlightCalendar
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow

/**
 * What the route costs on the days either side of the one that was asked
 * about.
 *
 * The point of the screen is the comparison: on this route the same seat can
 * cost half as much three days later, and no amount of searching one date at
 * a time will show that.
 *
 * A day with no cell is a day no site quoted. It is drawn as empty and cannot
 * be tapped - not as zero, and not as "no flights", because we do not know
 * that. The distinction is the whole reason the grid has three visual states
 * rather than two.
 */
@Composable
fun CalendarScreen(
    calendar: FlightCalendar?,
    loading: Boolean,
    chosenDepart: String?,
    chosenReturn: String?,
    onPick: (depart: String, back: String?) -> Unit,
    onBack: () -> Unit,
    origin: String? = null,
    destination: String? = null,
    /** True when the "Graphique" button on the results page opened this. */
    startOnChart: Boolean = false,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    // Two readings of one set of numbers.
    //
    // The grid answers "which day should I fly?" and the chart answers "is
    // this week dear or cheap?", and people ask both within about five
    // seconds of each other. They were two screens' worth of work and the
    // chart had never been built, so the app could only answer the first.
    var chart by rememberSaveable(startOnChart) { mutableStateOf(startOnChart) }

    val route = if (origin != null && destination != null) {
        "${cityName(origin, lang)} ${routeArrow(lang)} ${cityName(destination, lang)}"
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(title = words.priceByDate, onBack = onBack)

        Segmented(
            left = words.calendarTab,
            right = words.priceChart,
            rightOn = chart,
            onPick = { chart = it },
            modifier = Modifier.padding(horizontal = Space.s4),
        )

        // Which route this is about. Somebody who reached it from a search
        // two taps ago should not have to remember.
        route?.let {
            Text(
                text = if (chart) "$it · ${words.chartSub}" else it,
                style = MaterialTheme.typography.labelMedium,
                color = Ink.muted,
                modifier = Modifier.padding(horizontal = Space.s4, vertical = Space.s3),
            )
        }

        when {
            loading -> Centred {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Ink.accentDeep)
                Text(
                    words.calendarLoading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    textAlign = TextAlign.Center,
                )
            }

            calendar == null || calendar.cells.isEmpty() -> Centred {
                Text(words.calendarEmpty, style = MaterialTheme.typography.bodyLarge, color = Ink.muted)
            }

            chart -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.s4, end = Space.s4, bottom = Space.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.s4),
            ) {
                item { PriceChart(calendar.pricedDays, chosenDepart, lang, onPick) }
                item { ChartSummary(calendar.pricedDays, lang) }
                item {
                    Text(
                        words.chartNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.muted,
                    )
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.s4, end = Space.s4, bottom = Space.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.s4),
            ) {
                item {
                    if (calendar.roundTrip) {
                        Matrix(calendar, chosenDepart, chosenReturn, lang, onPick)
                    } else {
                        OneWayStrip(calendar, chosenDepart, lang, onPick)
                    }
                }
                calendar.cheapest?.let { best ->
                    item { CheapestNote(best, lang, onPick) }
                }
                item {
                    Text(
                        words.calendarNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(Space.s6), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) { content() }
    }
}

@Composable
private fun CheapestNote(best: CalendarCell, lang: Lang, onPick: (String, String?) -> Unit) {
    val words = LocalWords.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Text(
            words.cheapestOver,
            style = MaterialTheme.typography.labelMedium,
            color = Ink.muted,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDateLong(best.departDate, lang),
                style = MaterialTheme.typography.titleLarge,
                color = Ink.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                Money.format(best.price ?: 0.0, lang),
                style = MaterialTheme.typography.titleLarge,
                color = Ink.accentDeep,
                maxLines = 1,
            )
        }
        best.returnDate?.let {
            Text(
                formatDate(it, lang),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.inkSoft,
            )
        }
        // The card ends in the thing you came here to do. Reading "the 25th
        // is cheapest" and then having to find the 25th again in the rail
        // above is a step the screen can take for you.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.pill))
                .background(Ink.surface)
                .border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.pill))
                .clickable { onPick(best.departDate, best.returnDate) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                words.seeFlightsOn.replace("{date}", formatDate(best.departDate, lang)),
                style = MaterialTheme.typography.titleSmall,
                color = Ink.ink,
                maxLines = 1,
            )
        }
    }
}

/**
 * One way: a rail of day tiles, as drawn.
 *
 * It was a column of full-width rows, one day per row, which meant five days
 * filled the screen and the sixth was a scroll away. The whole point of this
 * screen is comparing days against each other, and a comparison you have to
 * scroll through is not one you can make. Tiles put a fortnight in view.
 *
 * The month sits above the rail rather than in every tile: thirty tiles
 * carrying "août" thirty times is thirty words that never change.
 */
@Composable
private fun OneWayStrip(
    calendar: FlightCalendar,
    chosen: String?,
    lang: Lang,
    onPick: (String, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        calendar.departDates.firstOrNull()?.let { first ->
            Text(
                monthLabel(first, lang).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            calendar.departDates.forEach { date ->
                val cell = calendar.cell(date)
                DayTile(
                    date = date,
                    cell = cell,
                    selected = date == chosen,
                    lang = lang,
                    onClick = { onPick(date, null) },
                )
            }
        }
    }
}

/**
 * One day: the weekday, the number, the price.
 *
 * Three states with three different jobs. Chosen is ink-filled, because it is
 * where you are. Cheapest is teal-outlined, because it is where you might
 * want to be. A day nobody quoted is pale and inert — not zero, and not "no
 * flights", because we do not know that, and the two must never look alike.
 */
@Composable
private fun DayTile(
    date: String,
    cell: CalendarCell?,
    selected: Boolean,
    lang: Lang,
    onClick: () -> Unit,
) {
    val priced = (cell?.price ?: 0.0) > 0.0
    val best = cell?.rank == FlightCalendar.RANK_CHEAPEST

    Column(
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(
                when {
                    selected -> Ink.solid
                    best -> Ink.accentSoft
                    priced -> Ink.surface
                    else -> Ink.canvas
                },
            )
            .border(
                width = if (best || selected) 2.dp else 1.dp,
                color = when {
                    selected -> Ink.solid
                    best -> Ink.accentUi
                    else -> Ink.line
                },
                shape = RoundedCornerShape(Radius.md),
            )
            // A day nobody quoted is not a choice. Tapping it would run a
            // search we already know returns nothing.
            .then(if (priced) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            weekdayLabel(date, lang),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Ink.onSolid else Ink.muted,
            maxLines = 1,
        )
        Text(
            dayNumber(date, lang),
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) Ink.onSolid else Ink.ink,
            maxLines = 1,
        )
        Text(
            // "No price" and "free" must never look alike, and a dash is the
            // shortest honest way to say the first without saying the second.
            if (priced) Money.amount(cell!!.price!!) else "—",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                selected -> Ink.onSolid
                best -> Ink.accentDeep
                priced -> Ink.inkSoft
                else -> Ink.muted
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Round trip: departures down the side, returns across the top, the way the
 * site draws it. It scrolls sideways rather than shrinking the text - a price
 * nobody can read is not a price.
 */
@Composable
private fun Matrix(
    calendar: FlightCalendar,
    chosenDepart: String?,
    chosenReturn: String?,
    lang: Lang,
    onPick: (String, String?) -> Unit,
) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Column {
            // Corner: keeps the header row aligned with the date column.
            Box(modifier = Modifier.width(DateColumn).size(width = DateColumn, height = HeaderHeight))
            calendar.departDates.forEach { depart ->
                Box(
                    modifier = Modifier.width(DateColumn).size(width = DateColumn, height = CellHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        formatDate(depart, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (depart == chosenDepart) Ink.ink else Ink.inkSoft,
                    )
                }
            }
        }

        calendar.returnDates.forEach { back ->
            Column {
                Box(
                    modifier = Modifier.size(width = CellWidth, height = HeaderHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        formatDate(back, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (back == chosenReturn) Ink.ink else Ink.inkSoft,
                    )
                }
                calendar.departDates.forEach { depart ->
                    MatrixCell(
                        cell = calendar.cell(depart, back),
                        selected = depart == chosenDepart && back == chosenReturn,
                        lang = lang,
                        onClick = { onPick(depart, back) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(cell: CalendarCell?, selected: Boolean, lang: Lang, onClick: () -> Unit) {
    val priced = (cell?.price ?: 0.0) > 0.0
    val best = cell?.rank == FlightCalendar.RANK_CHEAPEST
    Box(
        modifier = Modifier
            .size(width = CellWidth, height = CellHeight)
            .padding(2.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(
                when {
                    selected -> Ink.solid
                    best -> Ink.accentSoft
                    priced -> Ink.surface
                    else -> Ink.canvas
                },
            )
            .border(
                width = if (best || selected) 2.dp else 1.dp,
                color = if (selected) Ink.solid else if (best) Ink.accentUi else Ink.line,
                shape = RoundedCornerShape(Radius.sm),
            )
            .then(if (priced) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (priced) {
            Text(
                Money.amount(cell!!.price!!),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    selected -> Ink.onSolid
                    best -> Ink.accentDeep
                    else -> Ink.ink
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val DateColumn = 76.dp
private val CellWidth = 84.dp
private val CellHeight = 52.dp
private val HeaderHeight = 32.dp

/**
 * Two words, one lit.
 *
 * A pill inside a track rather than two tabs with an underline: an underline
 * belongs to a bar that scrolls content sideways, and these two do not
 * scroll — they redraw. The moving white pill is the same control the home
 * screen uses for one-way against round trip, so the gesture is already
 * learned by the time somebody gets here.
 */
@Composable
private fun Segmented(
    left: String,
    right: String,
    rightOn: Boolean,
    onPick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surfaceSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to left, true to right).forEach { (isRight, label) ->
            val on = isRight == rightOn
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (on) Ink.surface else Ink.surfaceSoft)
                    .clickable { onPick(isRight) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (on) Ink.ink else Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The lowest price on each day, as columns.
 *
 * Colour carries the ranking, not the height alone: the cheapest days are
 * teal, the dearest is red, the chosen day is ink, and everything else is
 * grey. Height on its own is a poor comparison when the spread is narrow —
 * 15 900 against 18 300 is a bar difference of one finger's width — and the
 * question people are asking is "which of these is the good one", which a
 * colour answers instantly and a height does not.
 *
 * There is no prediction here and there will not be. Every column is a price
 * a site actually quoted.
 */
@Composable
private fun PriceChart(
    days: List<CalendarCell>,
    chosen: String?,
    lang: Lang,
    onPick: (depart: String, back: String?) -> Unit,
) {
    val words = LocalWords.current
    if (days.isEmpty()) {
        Text(words.calendarEmpty, style = MaterialTheme.typography.bodyLarge, color = Ink.muted)
        return
    }

    val prices = days.mapNotNull { it.price }
    val low = prices.min()
    val high = prices.max()
    // A flat route would divide by zero. It also deserves full-height bars:
    // every day costs the same, and drawing them all at nothing would say
    // the opposite.
    val span = (high - low).takeIf { it > 0.0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val price = day.price ?: return@forEach
            // A floor of a third, so the cheapest day is still a bar rather
            // than a line somebody has to hunt for.
            val fraction = span?.let { 0.34f + 0.66f * ((price - low) / it).toFloat() } ?: 1f
            val colour = when {
                day.departDate == chosen -> Ink.ink
                price == low -> Ink.accentUi
                price == high -> Ink.alert
                else -> Ink.muted
            }
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    // On a return trip the bar is the cheapest pairing for
                    // that departure, and carries its return date with it.
                    .clickable { onPick(day.departDate, day.returnDate) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    Money.amount(price),
                    style = MetaType,
                    color = colour,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((CHART_HEIGHT * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(colour),
                )
                Text(
                    formatDate(day.departDate, lang),
                    style = MetaType,
                    color = if (day.departDate == chosen) Ink.ink else Ink.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

private const val CHART_HEIGHT = 150f

/** Lowest, average, highest — the three numbers under the columns. */
@Composable
private fun ChartSummary(days: List<CalendarCell>, lang: Lang) {
    val words = LocalWords.current
    val prices = days.mapNotNull { it.price }
    if (prices.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(vertical = Space.s3),
    ) {
        listOf(
            Triple(words.chartLow, prices.min(), Ink.accentDeep),
            Triple(words.chartAvg, prices.average(), Ink.ink),
            Triple(words.chartHigh, prices.max(), Ink.alert),
        ).forEach { (label, value, colour) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
                Text(
                    Money.format(value, lang),
                    style = MaterialTheme.typography.titleSmall,
                    color = colour,
                    maxLines = 1,
                )
            }
        }
    }
}
