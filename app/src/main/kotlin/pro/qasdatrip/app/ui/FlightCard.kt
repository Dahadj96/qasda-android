package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import pro.qasdatrip.core.routeArrow
import pro.qasdatrip.core.stopCount

/**
 * One offer, as the design draws it.
 *
 * Four lines and no ornament: who flies it and what it costs, when it leaves
 * and lands, and the small print. There is no airline logo here on purpose —
 * a wide wordmark crushed into a 40dp circle is decoration that costs a
 * network request and reads as noise beside the name it is already next to.
 *
 * The card is the whole target: tapping it opens the detail, where every
 * site's price is listed and the booking actually starts. A "Book" button
 * here would send somebody to one site before they had seen the others,
 * which is the opposite of what this product is for.
 */
@Composable
fun FlightCard(
    flight: Flight,
    onOpen: () -> Unit,
    onBook: () -> Unit,
    best: Boolean = false,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val cheapest = flight.cheapest
    val leg = flight.outbound ?: flight.outboundOrSelf

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(
                width = if (best) 2.dp else 1.dp,
                color = if (best) Ink.accentUi else Ink.line,
                shape = RoundedCornerShape(Radius.md),
            )
            .clickable(onClick = onOpen)
            .padding(CardPad),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        if (best) {
            Tag(words.bestPrice, fg = Ink.accentDeep, bg = Ink.accentSoft)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Airlines.name(leg?.operatingAirline ?: flight.airline),
                style = MaterialTheme.typography.titleMedium,
                color = Ink.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 18sp, not the 22 of a screen headline: on a list of a dozen
            // cards the price is the thing being compared, not the thing
            // being announced, and at 22 every row shouts over the next.
            Text(
                text = Money.format(cheapest?.second ?: 0.0, lang),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                color = Ink.accentDeep,
            )
        }

        leg?.let { TimesRow(it) }
        flight.inbound?.let { TimesRow(it, label = words.inbound) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            cheapest?.let { Tag(Sites.name(it.first), fg = Ink.inkSoft, bg = Ink.surfaceSoft) }
            // A fare with no checked bag is not the same fare at a lower
            // price, and the list sorts on price. The design does not draw
            // this chip; leaving it out would let the cheapest row on screen
            // be cheapest only because something was taken out of it.
            if (!flight.hasLuggage) {
                Tag(words.bagNone, fg = Ink.alert, bg = Ink.alertSoft)
            }
            Seats.left(flight.seatsAvailable)?.let { n ->
                Tag(seatsLabel(n, words), fg = Ink.alert, bg = Ink.alertSoft)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = words.seeOffer,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
                color = Ink.accentDeep,
                maxLines = 1,
            )
        }
    }
}

private val CardPad = 14.dp

/**
 * `07:30 → 10:45` on the left, `Direct · 3h15` on the right.
 *
 * The stop count and the duration share a line because they answer one
 * question together: two hours longer for one stop is a trade somebody makes
 * in a glance, and splitting them across the card makes them argue.
 */
@Composable
private fun TimesRow(leg: Leg, label: String? = null) {
    val words = LocalWords.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = Ink.muted)
        }
        Text(
            Money.isolate(leg.departure.orEmpty()),
            style = MaterialTheme.typography.titleLarge,
        )
        // The row is mirrored in Arabic, so the departure sits on the right;
        // the glyph has to turn around with it or it points back at where the
        // journey started.
        Text(routeArrow(LocalLang.current), style = MaterialTheme.typography.bodyLarge, color = Ink.muted)
        Row(verticalAlignment = Alignment.Top) {
            Text(
                Money.isolate(leg.arrival.orEmpty()),
                style = MaterialTheme.typography.titleLarge,
            )
            // A 14:25 departure arriving "17:35" reads as an afternoon hop.
            // When the duration says that 17:35 is tomorrow, the +1 is the
            // difference between a three-hour flight and a twenty-six-hour one.
            val days = arrivalDayOffset(leg.departure, leg.arrival, leg.duration) ?: 0
            if (days > 0) {
                Text(
                    Money.isolate("+$days"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.alert,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = journeyLine(leg, words),
            style = MaterialTheme.typography.labelMedium,
            color = if (leg.stopCount == 0) Ink.accentDeep else Ink.muted,
            maxLines = 1,
        )
    }
}

/**
 * "Direct · 3h15", or just "3h15" when the itinerary does not say how many
 * stops it has. "Direct" is a claim, and some sites send no stop count.
 */
private fun journeyLine(leg: Leg, words: Words): String {
    val stops = stopsLabel(leg.stopCount, words.direct, words.stopsOne, words.stopsMany)
    val duration = leg.duration?.takeIf { it.isNotBlank() }?.let { Money.isolate(it) }
    return listOfNotNull(stops, duration).joinToString(" · ")
}

private fun stopsLabel(stops: Int?, direct: String, one: String, many: String): String? = when (stops) {
    null -> null
    0 -> direct
    1 -> one
    else -> many.replace("{n}", Money.isolate(stops.toString()))
}

/** "1 seats" is the sort of thing that makes an app look machine-written. */
fun seatsLabel(n: Int, words: Words): String =
    if (n == 1) words.seatOne else words.seatsShort.replace("{n}", Money.isolate(n.toString()))

/**
 * The small rounded label the design uses inside cards: 10sp extra-bold on a
 * tinted ground, 6dp corners.
 */
@Composable
fun Tag(text: String, fg: Color, bg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.tag))
            .background(bg)
            .padding(horizontal = Space.s2, vertical = 3.dp),
    )
}

/** Kept for the detail screen, which still lists a fare's facts as chips. */
@Composable
fun Chip(text: String, fg: Color, bg: Color) = Tag(text, fg, bg)

/**
 * The arrival with its day offset, for screens that show a leg on its own.
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
