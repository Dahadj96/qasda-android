package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import pro.qasdatrip.core.stopCount

/**
 * One offer, as the design draws it.
 *
 * Read top to bottom it answers the four questions in the order people ask
 * them: who flies it and what it costs, when it leaves and lands and how
 * long that takes, what the fare does and does not include, and how to see
 * more.
 *
 * The last of those is the part that used to be missing. The card is still
 * tappable everywhere, but a card with no visible control does not tell a
 * first-time user that there is anything behind it — so the row ends in a
 * button that names what happens: it opens the detail. It does not book.
 * Booking belongs on the detail page, after every site's price has been
 * seen, because sending somebody to one site before they have compared is
 * the opposite of what this product is for.
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
    val carrier = leg?.operatingAirline ?: flight.airline

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            AirlineLogo(code = carrier, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Airlines.name(carrier),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitleOf(leg)?.let {
                    Text(
                        text = Money.isolate(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // 18sp, not the 22 of a screen headline: on a list of a dozen
                // cards the price is the thing being compared, not the thing
                // being announced, and at 22 every row shouts over the next.
                Text(
                    text = Money.format(cheapest?.second ?: 0.0, lang),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                    color = Ink.accentDeep,
                    maxLines = 1,
                )
                cheapest?.let {
                    Text(
                        text = words.onlyOnSite.replace("{site}", Sites.name(it.first)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.muted,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        leg?.let { TimesRow(it) }
        flight.inbound?.let { TimesRow(it, label = words.inbound) }

        val tags = buildList {
            if (flight.hasLuggage) add(Triple(words.bagIncluded, Ink.accentDeep, Ink.accentSoft))
            // A fare with no checked bag is not the same fare at a lower
            // price, and the list sorts on price. Leaving this out would let
            // the cheapest row on screen be cheapest only because something
            // was taken out of it.
            else add(Triple(words.bagNone, Ink.alert, Ink.alertSoft))
            Seats.left(flight.seatsAvailable)?.let { n ->
                add(Triple(seatsLabel(n, words), Ink.notice, Ink.noticeSoft))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            tags.forEach { (text, fg, bg) -> Tag(text, fg = fg, bg = bg) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.line),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Text(
                text = when {
                    best -> words.bestPrice
                    flight.quotingSites > 1 ->
                        words.sitesQuoting.replace("{n}", Money.isolate(flight.quotingSites.toString()))
                    else -> ""
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (best) Ink.accentDeep else Ink.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CardAction(label = words.seeDetails, onClick = onOpen)
        }
    }
}

private val CardPad = 14.dp

/** "AH 1006 · A330", or as much of it as the payload actually knows. */
private fun subtitleOf(leg: Leg?): String? {
    if (leg == null) return null
    val number = leg.flightNo?.trim()?.takeIf { it.isNotEmpty() }
    val aircraft = leg.aircraft.firstOrNull()?.code?.trim()?.takeIf { it.isNotEmpty() }
    val parts = listOfNotNull(number, aircraft)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/**
 * The card's own control. Outlined rather than filled: the filled button on
 * this screen is the one at the bottom of the detail that leaves the app,
 * and two solid black buttons on one screen make the wrong one look like
 * the safe one.
 */
@Composable
private fun CardAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Ink.ink,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s3, vertical = 10.dp),
    )
}

/**
 * The two clock times with their airport codes beneath them, and between
 * them the rail: elapsed time above, stop count below.
 *
 * The stop count and the duration belong together because they answer one
 * question together — two hours longer for one stop is a trade somebody
 * makes in a glance, and splitting them across the card makes them argue.
 */
@Composable
private fun TimesRow(leg: Leg, label: String? = null) {
    val words = LocalWords.current
    Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Column {
                Text(
                    Money.isolate(leg.departure.orEmpty()),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink.ink,
                )
                Text(
                    leg.origin.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                leg.duration?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        Money.isolate(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.inkSoft,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s1),
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Ink.line))
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(Ink.lineStrong))
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Ink.line))
                }
                stopsLabel(leg.stopCount, words.direct, words.stopsOne, words.stopsMany)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (leg.stopCount == 0) Ink.accentDeep else Ink.muted,
                        maxLines = 1,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        Money.isolate(leg.arrival.orEmpty()),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink.ink,
                    )
                    // A 14:25 departure arriving "17:35" reads as an afternoon
                    // hop. When the duration says 17:35 is tomorrow, the +1 is
                    // the difference between a three-hour flight and a
                    // twenty-six-hour one.
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
                Text(
                    leg.destination.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                )
            }
        }
    }
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
