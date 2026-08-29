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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airlines
import pro.qasdatrip.core.Airports
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
import pro.qasdatrip.core.formatMinutes
import pro.qasdatrip.core.layovers
import pro.qasdatrip.core.stopCount

/**
 * One flight, in full: what it actually flies, what it lets you carry, what
 * it costs to change your mind, and what each site is asking for it.
 *
 * The price table is the reason this screen exists. A card can only carry one
 * number; the product is that four sites quoted four different numbers for
 * the same seat, and here they sit next to each other in one list.
 *
 * One booking button, not one per row. A button beside every price makes the
 * table a set of doors and the comparison an afterthought; a single button
 * at the foot, naming the site it will open, keeps the table the point and
 * the choice explicit.
 *
 * A site that has not answered, or answered with nothing, is simply absent.
 * It never appears as a failed row: how our scrapers are getting on is our
 * business, not something to tell somebody looking for a flight.
 */
@Composable
fun DetailsScreen(
    flight: Flight,
    onBook: (site: String) -> Unit,
    onBack: () -> Unit,
    onTrack: () -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    // Only sites that actually quoted a number, cheapest first.
    val quotes = flight.prices
        .mapNotNull { (site, value) -> value?.takeIf { it > 0 }?.let { site to it } }
        .sortedBy { it.second }
    val best = quotes.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        DetailsBar(flight, onBack)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            flight.outbound?.let { leg -> item { LegCard(leg, if (flight.inbound != null) words.outbound else null) } }
            flight.inbound?.let { leg -> item { LegCard(leg, words.inbound) } }

            if (quotes.isNotEmpty()) {
                item {
                    Text(
                        words.compareSites.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.muted,
                    )
                }
                item {
                    // One card, hairlines between the rows. Separate cards
                    // per site read as four offers; this is four prices for
                    // one seat, and the shape should say so.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Ink.surface)
                            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
                    ) {
                        quotes.forEachIndexed { index, (site, amount) ->
                            if (index > 0) Hairline()
                            SiteRow(
                                site = site,
                                amount = amount,
                                seats = flight.seats[site],
                                best = index == 0,
                            )
                        }
                    }
                }
            }

            item { BaggageCard(flight) }

            flight.fareRules?.let { rules ->
                item { ConditionsCard(rules.refundable, rules.changeable, rules.refundFee, rules.changeFee) }
            }
        }

        // The two things somebody does from here, in the order they do them.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.s4, vertical = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            best?.let { (site, _) ->
                Button(
                    onClick = { onBook(site) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
                ) {
                    // Naming the site is the warning: this leaves the app,
                    // and the ticket is theirs to sell, not ours.
                    Text(
                        words.bookOn.replace("{site}", Sites.name(site)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            OutlinedButton(
                onClick = onTrack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Radius.pill),
            ) {
                Text(words.trackRoute, style = MaterialTheme.typography.titleMedium, color = Ink.ink)
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
            .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Auto-mirrored: in Arabic the arrow has to point the other way, and
        // a typed "←" would not turn around. In a circle because it is the
        // most-pressed control on the screen.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Ink.surface)
                .border(1.dp, Ink.line, RoundedCornerShape(Radius.pill))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Ink.ink,
                modifier = Modifier.size(20.dp),
            )
        }
        AirlineLogo(code = airline, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                Airlines.name(airline),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                leg?.flightNo?.takeIf { it.isNotBlank() },
                leg?.aircraft?.firstOrNull()?.name?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    Money.isolate(subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A leg read top to bottom, stop by stop.
 *
 * This is the screen you asked for, and the reason is one real itinerary:
 * Nouvelair ALG to CDG, "25h 20m · 1 escale" on the card, and nothing
 * anywhere in the app saying that nineteen of those hours are spent sitting
 * in Tunis. The card cannot say it — there is no room, and a card is a
 * summary. The detail page can, and until now it did not either: it listed
 * "BJ 131 · ALG → TUN" and "BJ 794 · TUN → CDG" and left the gap between
 * them for the reader to work out.
 *
 * So the leg is drawn as what it is: a sequence of places, with the time on
 * the ground written between them. A wait over four hours is tinted amber
 * and a wait that runs through the night says so, because those two change
 * what a trip costs a person in a way the price never mentions.
 *
 * When the numbers do not agree — a duration that cannot be reconciled with
 * the clock times — nothing is drawn rather than a guess. Somebody books
 * around a layover, and a wrong one is worse than none.
 */
@Composable
private fun LegCard(leg: Leg, label: String?) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val stops = leg.layovers()
    val hops = leg.segments

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
    ) {
        label?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
                modifier = Modifier.padding(start = Space.s4, top = Space.s3),
            )
        }

        if (hops.size > 1) {
            Endpoint(time = hops.first().departure, iata = hops.first().origin, lang = lang)
            hops.forEachIndexed { index, hop ->
                SegmentBand(hop)
                if (index < hops.size - 1) {
                    Endpoint(time = hop.arrival, iata = hop.destination, lang = lang, hollow = true)
                    stops.getOrNull(index)?.let { stop ->
                        LayoverRow(
                            minutes = stop.minutes,
                            airport = stop.airport,
                            long = stop.long,
                            overnight = stop.overnight,
                            departsAt = hops[index + 1].departure,
                            lang = lang,
                        )
                    }
                }
            }
            Endpoint(
                time = hops.last().arrival,
                iata = hops.last().destination,
                lang = lang,
                leg = leg,
            )
        } else {
            Endpoint(time = leg.departure, iata = leg.origin, lang = lang)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink.surfaceSoft)
                    .padding(horizontal = Space.s4, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    listOfNotNull(leg.duration?.let(Money::isolate), stopsText(leg.stopCount, words))
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.muted,
                    modifier = Modifier.padding(start = 72.dp + Space.s3 - Space.s4 + 8.dp),
                )
            }
            Endpoint(time = leg.arrival, iata = leg.destination, lang = lang, leg = leg)
        }

        Hairline()
        Row(
            modifier = Modifier.fillMaxWidth().padding(Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Text(
                listOfNotNull(leg.duration?.let(Money::isolate), stopsText(leg.stopCount, words))
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = Ink.inkSoft,
            )
        }
    }
}

/**
 * The flight between two of the stops: which aircraft, on whose metal.
 *
 * Indented to sit under the timeline's dots rather than beside them, so the
 * column of times stays a column and the eye can run down it.
 */
@Composable
private fun SegmentBand(segment: Segment) {
    val words = LocalWords.current
    val operating = segment.operatingAirline ?: segment.marketingAirline
    val line = listOfNotNull(
        segment.flightNo?.takeIf { it.isNotBlank() },
        segment.aircraftName?.takeIf { it.isNotBlank() } ?: segment.aircraft?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    if (line.isEmpty() && operating == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp + Space.s3 + 8.dp, end = Space.s4, bottom = Space.s2),
    ) {
        if (line.isNotEmpty()) {
            Text(Money.isolate(line), style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
        // A ticket sold by one carrier and flown by another is not the
        // experience the brand on the ticket implies.
        if (segment.marketingAirline != null && operating != segment.marketingAirline) {
            Text(
                words.operatedBy.replace("{airline}", Airlines.name(operating)),
                style = MaterialTheme.typography.labelMedium,
                color = Ink.notice,
            )
        }
    }
}

/**
 * How long the passenger is on the ground here, and when they leave again.
 *
 * Amber for anything over four hours, and the same amber with a plain
 * sentence for a wait that runs through the night. Neutral otherwise: a
 * ninety-minute connection is normal and does not deserve a warning colour.
 */
@Composable
private fun LayoverRow(
    minutes: Int,
    airport: String?,
    long: Boolean,
    overnight: Boolean,
    departsAt: String?,
    lang: Lang,
) {
    val words = LocalWords.current
    val place = airport?.let { code ->
        Airports.byIata(code)?.let { "${it.cityIn(lang)} ($code)" } ?: code
    }.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp + Space.s3 + 8.dp, end = Space.s4, bottom = Space.s3),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.sm))
                .background(if (long) Ink.noticeSoft else Ink.surfaceSoft)
                .padding(horizontal = Space.s3, vertical = 10.dp),
        ) {
            Text(
                words.layoverAt
                    .replace("{time}", Money.isolate(formatMinutes(minutes)))
                    .replace("{airport}", place),
                style = MaterialTheme.typography.bodyLarge,
                color = if (long) Ink.notice else Ink.inkSoft,
            )
            val note = listOfNotNull(
                departsAt?.takeIf { it.isNotBlank() }?.let {
                    words.layoverDeparts.replace("{time}", Money.isolate(it))
                },
                words.overnightStop.takeIf { overnight },
            ).joinToString(" · ")
            if (note.isNotEmpty()) {
                Text(
                    note,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (long) Ink.notice else Ink.muted,
                )
            }
        }
    }
}

/**
 * One end of a leg: the time, a dot, and where it is. The airport's own name
 * under its city, because "Paris (ORY)" and "Paris (CDG)" are an hour apart
 * by road and somebody meeting a plane needs to know which.
 */
@Composable
private fun Endpoint(
    time: String?,
    iata: String?,
    lang: Lang,
    leg: Leg? = null,
    // A stop is drawn hollow because the passenger does not end there.
    hollow: Boolean = false,
) {
    val days = leg?.let { arrivalDayOffset(it.departure, it.arrival, it.duration) } ?: 0
    val airport = iata?.let { Airports.byIata(it) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(Space.s4),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Wide enough for "16:45" plus a day marker. At 56dp the "+1" wrapped
        // onto a second line and read as a "+" above a "1".
        Row(modifier = Modifier.width(72.dp), verticalAlignment = Alignment.Top) {
            Text(
                Money.isolate(time.orEmpty()),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
            if (days > 0) {
                Text(
                    Money.isolate("+$days"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.alert,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(if (hollow) 10.dp else 8.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(if (hollow) Ink.surface else Ink.accentUi)
                .then(
                    if (hollow) Modifier.border(2.dp, Ink.lineStrong, RoundedCornerShape(Radius.pill))
                    else Modifier
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildString {
                    append(airport?.cityIn(lang) ?: iata.orEmpty())
                    if (airport != null && !iata.isNullOrBlank()) append(" ($iata)")
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val second = airport?.name?.takeIf { it.isNotBlank() }.orEmpty()
            if (second.isNotEmpty()) {
                Text(
                    Money.isolate(second),
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * One site, one price. Every number here was quoted by the site sitting next
 * to it — nothing on this screen is computed.
 *
 * The cheapest row is tinted rather than badged. A badge would be a fifth
 * thing to read on a row that already has three; the tint is read before
 * anything on it is.
 */
@Composable
private fun SiteRow(site: String, amount: Double, seats: Int?, best: Boolean) {
    val words = LocalWords.current
    val lang = LocalLang.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (best) Ink.accentSoft else Ink.surface)
            .padding(horizontal = Space.s4, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                Sites.name(site),
                style = MaterialTheme.typography.titleMedium,
                color = if (best) Ink.accentDeep else Ink.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Seats.left(seats)?.let { n ->
                Text(
                    seatsLabel(n, words),
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.alert,
                )
            }
        }
        Text(
            Money.format(amount, lang),
            style = MaterialTheme.typography.titleLarge,
            color = if (best) Ink.accentDeep else Ink.ink,
        )
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
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.inkSoft)
        Text(
            bagText(bag, words),
            style = MaterialTheme.typography.titleMedium,
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
        Text(
            words.conditions.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Ink.muted,
            modifier = Modifier.padding(bottom = Space.s1),
        )
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
                style = MaterialTheme.typography.labelMedium,
                color = Ink.notice,
            )
        }
    }
}

@Composable
private fun Hairline() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
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
