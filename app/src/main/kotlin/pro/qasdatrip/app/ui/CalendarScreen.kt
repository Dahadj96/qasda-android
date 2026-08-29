package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import pro.qasdatrip.core.CalendarCell
import pro.qasdatrip.core.FlightCalendar
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money

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
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Ink.surface).padding(Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Ink.ink,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text(words.priceCalendar, style = MaterialTheme.typography.titleMedium)
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

            else -> LazyColumn(
                contentPadding = PaddingValues(Space.s4),
                verticalArrangement = Arrangement.spacedBy(Space.s4),
            ) {
                calendar.cheapest?.let { best ->
                    item { CheapestNote(best, lang) }
                }
                item {
                    if (calendar.roundTrip) {
                        Matrix(calendar, chosenDepart, chosenReturn, lang, onPick)
                    } else {
                        OneWayStrip(calendar, chosenDepart, lang, onPick)
                    }
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
private fun CheapestNote(best: CalendarCell, lang: Lang) {
    val words = LocalWords.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.accentSoft)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        Text(
            words.cheapestDay
                .replace("{date}", formatDate(best.departDate, lang))
                .replace("{price}", Money.format(best.price ?: 0.0, lang)),
            style = MaterialTheme.typography.titleMedium,
            color = Ink.accentDeep,
        )
        best.returnDate?.let {
            Text(
                formatDate(it, lang),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.inkSoft,
            )
        }
    }
}

/** One way: a row per day, because a week of dates reads better than a grid of one column. */
@Composable
private fun OneWayStrip(
    calendar: FlightCalendar,
    chosen: String?,
    lang: Lang,
    onPick: (String, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        calendar.departDates.forEach { date ->
            val cell = calendar.cell(date)
            DayRow(
                date = date,
                cell = cell,
                selected = date == chosen,
                lang = lang,
                onClick = { onPick(date, null) },
            )
        }
    }
}

@Composable
private fun DayRow(
    date: String,
    cell: CalendarCell?,
    selected: Boolean,
    lang: Lang,
    onClick: () -> Unit,
) {
    val words = LocalWords.current
    val priced = (cell?.price ?: 0.0) > 0.0
    val best = cell?.rank == FlightCalendar.RANK_CHEAPEST

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(
                when {
                    selected -> Ink.ink
                    best -> Ink.accentSoft
                    priced -> Ink.surface
                    else -> Ink.canvas
                },
            )
            .border(
                width = if (best || selected) 2.dp else 1.dp,
                color = when {
                    selected -> Ink.ink
                    best -> Ink.accentUi
                    priced -> Ink.line
                    else -> Ink.line
                },
                shape = RoundedCornerShape(Radius.md),
            )
            // A day nobody quoted is not a choice. Tapping it would run a
            // search we already know returns nothing.
            .then(if (priced) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Space.s4),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatDate(date, lang),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Ink.inverse else Ink.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            // "No price" and "free" must never look alike.
            if (priced) Money.format(cell!!.price!!, lang) else words.noPriceThatDay,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                selected -> Ink.inverse
                best -> Ink.accentDeep
                priced -> Ink.ink
                else -> Ink.muted
            },
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
                    selected -> Ink.ink
                    best -> Ink.accentSoft
                    priced -> Ink.surface
                    else -> Ink.canvas
                },
            )
            .border(
                width = if (best || selected) 2.dp else 1.dp,
                color = if (selected) Ink.ink else if (best) Ink.accentUi else Ink.line,
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
                    selected -> Ink.inverse
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
