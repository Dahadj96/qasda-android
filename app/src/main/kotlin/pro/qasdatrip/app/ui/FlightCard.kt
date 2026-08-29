package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Leg
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.Seats
import pro.qasdatrip.core.Sites
import pro.qasdatrip.core.Words
import pro.qasdatrip.core.arrivalDayOffset
import pro.qasdatrip.core.stopCount

/**
 * The card, as the site draws it: the two facts worth a chip, a leg you can
 * read across, and one price with the name of the site quoting it. The site
 * name belongs here and on the details page, nowhere else — that line is the
 * product.
 */
@Composable
fun FlightCard(flight: Flight, onOpen: () -> Unit, onBook: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val cheapest = flight.cheapest

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable(onClick = onOpen)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            Chip(
                text = if (flight.hasLuggage) words.bagIncluded else words.bagNone,
                fg = if (flight.hasLuggage) Ink.accentDeep else Ink.alert,
                bg = if (flight.hasLuggage) Ink.accentSoft else Ink.alertSoft,
            )
            Seats.left(flight.seatsAvailable)?.let { n ->
                Chip(text = seatsLabel(n, words), fg = Ink.notice, bg = Ink.noticeSoft)
            }
        }

        flight.outbound?.let { LegRow(it, words.outbound) }
        flight.inbound?.let { LegRow(it, words.inbound) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = Money.format(cheapest?.second ?: 0.0, lang),
                    style = MaterialTheme.typography.headlineSmall,
                )
                cheapest?.let {
                    Text(
                        text = words.cheapestOn.replace("{site}", Sites.name(it.first)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.accentDeep,
                    )
                }
            }
            Button(
                onClick = onBook,
                shape = RoundedCornerShape(Radius.sm),
                colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
            ) { Text(words.book, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun LegRow(leg: Leg, label: String) {
    val words = LocalWords.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            Text(Money.isolate(leg.departure.orEmpty()), style = MaterialTheme.typography.titleMedium)
            Text(leg.origin.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = Airlines.logoUrl(leg.operatingAirline),
                contentDescription = Airlines.name(leg.operatingAirline),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Ink.surfaceSoft),
            )
            Box(
                modifier = Modifier
                    .padding(top = Space.s2)
                    .fillMaxWidth(0.7f)
                    .height(1.dp)
                    .background(Ink.line),
            )
            // Nothing at all when the itinerary does not say. "Direct" is a
            // claim, and the server sends no stop count on this route.
            stopsLabel(leg.stopCount, words.direct, words.stopsOne, words.stopsMany)?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (leg.stopCount == 0) Ink.accentDeep else Ink.alert,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(Money.isolate(leg.duration.orEmpty()), style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            ArrivalTime(leg)
            Text(leg.destination.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
    }
}

private fun stopsLabel(stops: Int?, direct: String, one: String, many: String): String? = when (stops) {
    null -> null
    0 -> direct
    1 -> one
    else -> many.replace("{n}", Money.isolate(stops.toString()))
}

/**
 * The arrival, with the day it actually happens on.
 *
 * A 14:25 departure arriving "17:35" reads as an afternoon hop. When the
 * duration says that 17:35 is tomorrow, the +1 is the difference between a
 * three-hour flight and a twenty-six-hour one.
 */
@Composable
fun ArrivalTime(leg: Leg) {
    val days = arrivalDayOffset(leg.departure, leg.arrival, leg.duration) ?: 0
    Row(verticalAlignment = Alignment.Top) {
        Text(Money.isolate(leg.arrival.orEmpty()), style = MaterialTheme.typography.titleMedium)
        if (days > 0) {
            Text(
                Money.isolate("+$days"),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.alert,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/** "1 seats" is the sort of thing that makes an app look machine-written. */
fun seatsLabel(n: Int, words: Words): String =
    if (n == 1) words.seatOne else words.seatsShort.replace("{n}", Money.isolate(n.toString()))

@Composable
fun Chip(text: String, fg: Color, bg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(bg)
            .padding(horizontal = Space.s2, vertical = Space.s1),
    )
}
