package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import pro.qasdatrip.app.R
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
/**
 * The scale of the card's small print.
 *
 * Airport codes, stop counts, leg captions and tag labels are all one thing
 * in the drawing — eleven point, semi-bold, slightly tracked — and they were
 * all reaching for `labelSmall`, which is ten point extra-bold with wider
 * tracking because it is the style for a SECTION HEADING IN SMALL CAPS.
 * Borrowing it made every code and tag on this card a size too small and a
 * weight too heavy. This is the drawn value, kept here rather than added to
 * the shared scale because it is the card's, not the app's.
 */
internal val MetaType = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.9.sp,
)

/** The price, at the size the design gives it. */
private val PriceType = TextStyle(
    fontSize = 24.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp,
)

/** A one-way card's clock times. The round trip's are smaller — see LegRow. */
private val TimeType = TextStyle(
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.4).sp,
)

private val LegTimeType = TextStyle(
    fontSize = 17.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.5).sp,
)

@Composable
fun FlightCard(
    flight: Flight,
    onOpen: () -> Unit,
    onBook: () -> Unit,
    best: Boolean = false,
) {
    // Nobody quoted it. The card used to render that as "0 DA", which is a
    // price, and a wrong one. It is a flight that exists and cannot be
    // bought, which the design has a state for.
    val soldOut = flight.cheapest == null || flight.seatsAvailable == 0

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
        if (flight.inbound != null) {
            RoundTripBody(flight, soldOut)
        } else {
            OneWayBody(flight, soldOut)
        }

        CardTags(flight, soldOut)

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))

        Footer(flight = flight, best = best, soldOut = soldOut, onOpen = onOpen)
    }
}

/**
 * One way: the carrier and the price across the top, the journey beneath.
 */
@Composable
private fun OneWayBody(flight: Flight, soldOut: Boolean) {
    val lang = LocalLang.current
    val words = LocalWords.current
    val leg = flight.outbound ?: flight.outboundOrSelf
    val carrier = leg?.operatingAirline ?: flight.airline
    val cheapest = flight.cheapest

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
        PriceBlock(
            price = cheapest?.second,
            note = cheapest?.let { words.onlyOnSite.replace("{site}", Sites.name(it.first)) },
            soldOut = soldOut,
            lang = lang,
            align = Alignment.End,
        )
    }

    leg?.let { TimesRow(it) }
}

/**
 * Round trip: a block per leg with its own date and carrier, a rule between
 * them, and one price underneath for the pair.
 *
 * It used to be the one-way card with a second row of times stapled on and
 * the word "Retour" over it — one logo, one carrier name, and a price in the
 * header that silently covered both journeys. That is wrong twice over: the
 * two legs are routinely different airlines, and a price at the top of a
 * card reads as the price of the thing directly under it. Here the total sits
 * below both legs, where it plainly belongs to both, and says so.
 */
@Composable
private fun RoundTripBody(flight: Flight, soldOut: Boolean) {
    val words = LocalWords.current
    flight.outbound?.let { LegRow(it, words.outbound) }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
    flight.inbound?.let { LegRow(it, words.inbound) }
}

/** One leg of a round trip: caption, logo, times. */
@Composable
private fun LegRow(leg: Leg, direction: String) {
    val lang = LocalLang.current
    val words = LocalWords.current
    val carrier = leg.operatingAirline

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(direction.uppercase(), style = MetaType, color = Ink.muted, maxLines = 1)
            leg.departureDate?.takeIf { it.isNotBlank() }?.let {
                Text("· ${formatDate(it, lang)}", style = MetaType, color = Ink.inkSoft, maxLines = 1)
            }
            carrier?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "· ${Airlines.name(it)}",
                    style = MetaType,
                    color = Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // A logo per leg, because the two halves of a round trip are
            // routinely flown by different airlines and one mark at the top
            // of the card would be a claim about both.
            AirlineLogo(code = carrier, size = 28.dp)
            Endpoints(leg, timeStyle = LegTimeType, words = words)
        }
    }
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
private fun TimesRow(leg: Leg) {
    val words = LocalWords.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Endpoints(leg, timeStyle = TimeType, words = words)
    }
}

/** Depart · rail · arrive. Shared by both card shapes; only the type differs. */
@Composable
private fun RowScope.Endpoints(leg: Leg, timeStyle: TextStyle, words: Words) {
    Column {
        Text(Money.isolate(leg.departure.orEmpty()), style = timeStyle, color = Ink.ink, maxLines = 1)
        Text(leg.origin.orEmpty(), style = MetaType, color = Ink.muted, maxLines = 1)
    }
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        leg.duration?.takeIf { it.isNotBlank() }?.let {
            Text(
                Money.isolate(it),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.inkSoft,
                maxLines = 1,
            )
        }
        Rail()
        stopsLabel(leg.stopCount, words.direct, words.stopsOne, words.stopsMany)?.let {
            // Grey, not green. The drawing says the same word twice — once
            // here as a fact about the journey and once above as a tag — and
            // colouring both of them made "Direct" the loudest thing on a
            // card whose subject is a price.
            Text(it, style = MetaType, color = Ink.muted, maxLines = 1)
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Top) {
            Text(Money.isolate(leg.arrival.orEmpty()), style = timeStyle, color = Ink.ink, maxLines = 1)
            // A 14:25 departure arriving "17:35" reads as an afternoon hop.
            // When the duration says 17:35 is tomorrow, the +1 is the
            // difference between a three-hour flight and a twenty-six-hour
            // one.
            val days = arrivalDayOffset(leg.departure, leg.arrival, leg.duration) ?: 0
            if (days > 0) {
                Text(
                    Money.isolate("+$days"),
                    style = MetaType,
                    color = Ink.alert,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Text(leg.destination.orEmpty(), style = MetaType, color = Ink.muted, maxLines = 1)
    }
}

/**
 * The line between the two times, with an aircraft on it.
 *
 * The aircraft is the thing this card was missing. It was a four-pixel dot,
 * which is a full stop: it says the line has a middle and nothing else. The
 * mark says which way the journey runs, and on a round-trip card where the
 * return leg reads right to left underneath the outbound, that is not
 * decoration — it is the only thing on the row that distinguishes them at a
 * glance. It is the same drawing as the loading screen's, and it mirrors in
 * Arabic for the same reason.
 */
@Composable
private fun Rail() {
    val rtl = LocalLang.current.rtl
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Ink.rail))
        Icon(
            painter = painterResource(R.drawable.ic_plane_right),
            contentDescription = null,
            tint = Ink.muted,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer { scaleX = if (rtl) -1f else 1f },
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Ink.rail))
    }
}

/** The price and the site under it, at the size the design gives them. */
@Composable
private fun PriceBlock(
    price: Double?,
    note: String?,
    soldOut: Boolean,
    lang: pro.qasdatrip.core.Lang,
    align: Alignment.Horizontal,
) {
    Column(horizontalAlignment = align) {
        Text(
            text = if (price != null) Money.format(price, lang) else "—",
            style = PriceType,
            // Greyed when nothing can be bought at it. A green price on a
            // sold-out fare is an offer we cannot honour.
            color = if (soldOut) Ink.muted else Ink.accentDeep,
            maxLines = 1,
        )
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
                maxLines = 1,
                textAlign = if (align == Alignment.End) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

/**
 * What the fare is and what it carries.
 *
 * Three slots, in the order the design puts them: what the journey is, what
 * the ticket includes, and what is running out. The middle one is grey when
 * a bag is included, because that is the ordinary case and an ordinary case
 * in green is a badge; it turns red only when a bag is missing, which is the
 * fact that makes a cheap fare cheap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardTags(flight: Flight, soldOut: Boolean) {
    val words = LocalWords.current
    val leg = flight.outbound ?: flight.outboundOrSelf

    val tags = buildList {
        if (soldOut) {
            add(Triple(words.soldOut, Ink.alert, Ink.alertSoft))
            add(Triple(words.noSeats, Ink.notice, Ink.noticeSoft))
        } else {
            stopsLabel(leg?.stopCount, words.direct, words.stopsOne, words.stopsMany)?.let {
                add(
                    if (leg?.stopCount == 0) Triple(it, Ink.accentDeep, Ink.accentSoft)
                    else Triple(it, Ink.inkSoft, Ink.surfaceSoft),
                )
            }
            if (flight.hasLuggage) add(Triple(words.bagIncluded, Ink.inkSoft, Ink.surfaceSoft))
            else add(Triple(words.bagNone, Ink.alert, Ink.alertSoft))

            Seats.left(flight.seatsAvailable)?.let { n ->
                add(Triple(seatsLabel(n, words), Ink.notice, Ink.noticeSoft))
            }
        }
    }
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { (text, fg, bg) -> Tag(text, fg = fg, bg = bg) }
    }
}

/** How many sites quoted, and the way in. */
@Composable
private fun Footer(flight: Flight, best: Boolean, soldOut: Boolean, onOpen: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        // A round trip carries its price down here, under both legs, rather
        // than in a header over the first of them.
        if (flight.inbound != null) {
            PriceBlock(
                price = flight.cheapest?.second,
                note = flight.cheapest?.let {
                    "${words.roundTrip.lowercase()}, ${words.onlyOnSite.replace("{site}", Sites.name(it.first))}"
                },
                soldOut = soldOut,
                lang = lang,
                align = Alignment.Start,
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // "Le moins cher sur 4 sites", not "MEILLEUR PRIX". The shouted
            // version was a badge on a card that already wears a green
            // border; the sentence says the same thing and says how it is
            // known, which is the claim this product actually makes.
            val sites = Money.isolate(flight.quotingSites.toString())
            Text(
                text = when {
                    soldOut -> ""
                    best && flight.quotingSites > 1 -> words.cheapestOnSites.replace("{n}", sites)
                    best -> words.bestPrice
                    flight.quotingSites > 1 -> words.sitesQuoting.replace("{n}", sites)
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (best) Ink.accentDeep else Ink.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        // Nothing to look at on a fare nobody is selling; what somebody wants
        // there is to be told when that changes.
        CardAction(label = if (soldOut) words.notifyMe else words.seeDetails, onClick = onOpen)
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
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Ink.ink,
            maxLines = 1,
        )
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
 * The small rounded label the design uses inside cards: 24 tall, 8dp corners,
 * eleven point semi-bold on a tinted ground.
 */
@Composable
fun Tag(text: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = Space.s2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MetaType, color = fg, maxLines = 1)
    }
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
