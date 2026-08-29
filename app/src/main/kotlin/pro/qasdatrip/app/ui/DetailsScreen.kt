package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.BagAllowance
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Leg
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.Seats
import pro.qasdatrip.core.Segment
import pro.qasdatrip.core.Sites
import pro.qasdatrip.core.Words
import pro.qasdatrip.core.arrivalDayOffset
import pro.qasdatrip.core.stopCount

/**
 * One flight, in full: what it actually flies, what it lets you carry, what
 * it costs to change your mind, and what each site is asking for it.
 *
 * The price table is the reason this screen exists. A card can only carry one
 * number; the product is that four sites quoted four different numbers for the
 * same seat, and here they sit next to each other.
 *
 * A site that has not answered, or answered with nothing, is simply absent. It
 * never appears as a failed row: how our scrapers are getting on is our
 * business, not something to tell somebody looking for a flight.
 */
@Composable
fun DetailsScreen(flight: Flight, onBook: (site: String) -> Unit, onBack: () -> Unit) {
    val words = LocalWords.current
    val cheapest = flight.cheapest

    // Only sites that actually quoted a number, cheapest first.
    val quotes = flight.prices
        .mapNotNull { (site, value) -> value?.takeIf { it > 0 }?.let { site to it } }
        .sortedBy { it.second }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        DetailsBar(flight, onBack)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            flight.outbound?.let { leg -> item { LegCard(leg, words.outbound) } }
            flight.inbound?.let { leg -> item { LegCard(leg, words.inbound) } }

            item { BaggageCard(flight) }

            flight.fareRules?.let { rules ->
                item { ConditionsCard(rules.refundable, rules.changeable, rules.refundFee, rules.changeFee) }
            }

            if (quotes.isNotEmpty()) {
                item {
                    Text(words.priceBySite, style = MaterialTheme.typography.titleMedium)
                }
                items(quotes.size) { index ->
                    val (site, amount) = quotes[index]
                    SiteRow(
                        site = site,
                        amount = amount,
                        seats = flight.seats[site],
                        best = site == cheapest?.first,
                        onBook = { onBook(site) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsBar(flight: Flight, onBack: () -> Unit) {
    val leg = flight.outboundOrSelf
    val airline = leg?.operatingAirline ?: flight.airline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.surface)
            .padding(Space.s4),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Auto-mirrored: in Arabic the arrow has to point the other way, and
        // a typed "←" would not turn around.
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Ink.ink,
            modifier = Modifier.clickable(onClick = onBack),
        )
        AsyncImage(
            model = Airlines.logoUrl(airline),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Ink.surfaceSoft),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(Airlines.name(airline), style = MaterialTheme.typography.titleMedium)
            val subtitle = listOfNotNull(
                leg?.flightNo?.takeIf { it.isNotBlank() },
                leg?.aircraft?.firstOrNull()?.name?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(Money.isolate(subtitle), style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
            }
        }
    }
}

/**
 * A leg read top to bottom: leave here, fly this long, arrive there. The
 * segments are listed when there is more than one, because an itinerary with
 * a stop is two flights and the second may not be the same airline.
 */
@Composable
private fun LegCard(leg: Leg, label: String) {
    val words = LocalWords.current
    Card {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)

        Endpoint(time = leg.departure, place = leg.origin)
        Row(
            modifier = Modifier.padding(vertical = Space.s2),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(Ink.line),
            )
            Text(
                listOfNotNull(leg.duration?.let(Money::isolate), stopsText(leg.stopCount, words))
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
        }
        Endpoint(time = leg.arrival, place = leg.destination, leg = leg)

        if (leg.segments.size > 1) {
            leg.segments.forEach { segment -> SegmentLine(segment) }
        }
    }
}

@Composable
private fun Endpoint(time: String?, place: String?, leg: Leg? = null) {
    val days = leg?.let { arrivalDayOffset(it.departure, it.arrival, it.duration) } ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.width(64.dp)) {
            Text(Money.isolate(time.orEmpty()), style = MaterialTheme.typography.titleMedium)
            if (days > 0) {
                Text(
                    Money.isolate("+$days"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.alert,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Text(place.orEmpty(), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A segment, and the codeshare rule the site already follows: the airline
 * flying the aeroplane is the one worth naming. A ticket sold by one carrier
 * and flown by another is not the experience the brand on the ticket implies.
 */
@Composable
private fun SegmentLine(segment: Segment) {
    val words = LocalWords.current
    val operating = segment.operatingAirline ?: segment.marketingAirline
    val parts = listOfNotNull(
        segment.flightNo?.takeIf { it.isNotBlank() },
        segment.origin?.let { o -> segment.destination?.let { d -> "$o → $d" } },
    ).joinToString(" · ")

    Column(modifier = Modifier.padding(top = Space.s2)) {
        if (parts.isNotEmpty()) {
            Text(Money.isolate(parts), style = MaterialTheme.typography.bodyMedium, color = Ink.inkSoft)
        }
        if (segment.marketingAirline != null && operating != segment.marketingAirline) {
            Text(
                words.operatedBy.replace("{airline}", Airlines.name(operating)),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.notice,
            )
        }
    }
}

@Composable
private fun BaggageCard(flight: Flight) {
    val words = LocalWords.current
    val leg = flight.outbound ?: flight.inbound
    Card {
        BagLine(words.cabinBag, leg?.cabinBags, words)
        BagLine(words.checkedBag, leg?.checkedBags, words)
    }
}

@Composable
private fun BagLine(label: String, bag: BagAllowance?, words: Words) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s1),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            bagText(bag, words),
            style = MaterialTheme.typography.bodyLarge,
            // "Nobody said" is not "none", and it should not read like a
            // promise in either direction.
            color = if (bag == null) Ink.muted else Ink.ink,
        )
    }
}

private fun bagText(bag: BagAllowance?, words: Words): String {
    if (bag == null) return words.bagUnknown
    // Zero of something is not a quantity of it, it is the absence.
    if (bag.value <= 0) return words.bagNone
    val n = Money.isolate(bag.value.toString())
    // PIECE and KG are different promises; the unit travels with the number.
    return if (bag.unit.equals("KG", ignoreCase = true)) {
        words.kilos.replace("{n}", n)
    } else {
        words.pieces.replace("{n}", n)
    }
}

@Composable
private fun ConditionsCard(refundable: Boolean?, changeable: Boolean?, refundFee: Double?, changeFee: Double?) {
    val words = LocalWords.current
    val lang = LocalLang.current
    Card {
        Text(words.conditions, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
        // A null is a rule nobody stated. Printing "non-refundable" for it
        // would be inventing a term of somebody's ticket.
        refundable?.let { yes ->
            RuleLine(
                text = if (yes) words.refundable else words.nonRefundable,
                fee = refundFee.takeIf { yes },
                ok = yes,
                lang = lang,
                words = words,
            )
        }
        changeable?.let { yes ->
            RuleLine(
                text = if (yes) words.changeable else words.nonChangeable,
                fee = changeFee.takeIf { yes },
                ok = yes,
                lang = lang,
                words = words,
            )
        }
        if (refundable == null && changeable == null) {
            Text(words.bagUnknown, style = MaterialTheme.typography.bodyLarge, color = Ink.muted)
        }
    }
}

@Composable
private fun RuleLine(text: String, fee: Double?, ok: Boolean, lang: Lang, words: Words) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s1),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (ok) Ink.ink else Ink.muted)
        if (fee != null && fee > 0) {
            Text(
                words.feeApplies.replace("{amount}", Money.format(fee, lang)),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.notice,
            )
        }
    }
}

/**
 * One site, one price, and the door to it. Every number here was quoted by the
 * site sitting next to it — nothing on this screen is computed.
 */
@Composable
private fun SiteRow(site: String, amount: Double, seats: Int?, best: Boolean, onBook: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(if (best) Ink.accentSoft else Ink.surface)
            .border(1.dp, if (best) Ink.accentUi else Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                Sites.name(site),
                style = MaterialTheme.typography.titleMedium,
                color = if (best) Ink.accentDeep else Ink.ink,
            )
            val note = listOfNotNull(
                words.cheapest.takeIf { best },
                Seats.left(seats)?.let { n -> seatsLabel(n, words) },
            ).joinToString(" · ")
            if (note.isNotEmpty()) {
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (best) Ink.accentDeep else Ink.notice,
                )
            }
        }
        Text(Money.format(amount, lang), style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onBook,
            shape = RoundedCornerShape(Radius.sm),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (best) Ink.ink else Ink.surfaceSoft,
                contentColor = if (best) Ink.inverse else Ink.ink,
            ),
        ) { Text(words.book, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s1),
        content = content,
    )
}

/** Null when the itinerary does not say. "Direct" is a claim, not a default. */
private fun stopsText(stops: Int?, words: Words): String? = when (stops) {
    null -> null
    0 -> words.direct
    1 -> words.stopsOne
    else -> words.stopsMany.replace("{n}", Money.isolate(stops.toString()))
}
