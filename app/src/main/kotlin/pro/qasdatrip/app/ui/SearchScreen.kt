package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Airport
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.routeArrow
import java.time.LocalDate

/**
 * The search, in the order somebody fills it in: where from, where to, when,
 * and how many.
 */
@Composable
fun SearchScreen(
    recent: List<SearchQuery> = emptyList(),
    today: String = LocalDate.now().toString(),
    onSearch: (SearchQuery) -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    var from by remember { mutableStateOf("ALG") }
    var to by remember { mutableStateOf("CDG") }
    var depart by remember { mutableStateOf<String?>(null) }
    var back by remember { mutableStateOf<String?>(null) }
    var roundTrip by remember { mutableStateOf(false) }
    var adults by remember { mutableStateOf(1) }
    var children by remember { mutableStateOf(0) }
    var infants by remember { mutableStateOf(0) }
    var cabin by remember { mutableStateOf(Cabin.ECONOMY) }
    var picking by remember { mutableStateOf<String?>(null) }
    var pickingDates by remember { mutableStateOf(false) }
    var pickingTravellers by remember { mutableStateOf(false) }

    if (pickingTravellers) {
        TravellersPicker(
            adults = adults,
            children = children,
            infants = infants,
            cabin = cabin,
            onApply = { a, c, i, klass ->
                adults = a; children = c; infants = i; cabin = klass
                pickingTravellers = false
            },
        )
        return
    }

    if (pickingDates) {
        DatesDialog(
            roundTrip = roundTrip,
            depart = depart,
            back = back,
            onDismiss = { pickingDates = false },
            onPick = { d, b ->
                depart = d
                back = if (roundTrip) b else null
                pickingDates = false
            },
        )
    }

    if (picking != null) {
        AirportPicker(
            originSide = picking == "from",
            onPick = { airport ->
                if (picking == "from") from = airport.iata else to = airport.iata
                picking = null
            },
            onDismiss = { picking = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.canvas)
            .verticalScroll(rememberScrollState())
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
            // The product's name, not a slogan. This is the first screen of an
            // app somebody chose to open; they know what it is for, and the
            // headline's job is to say where they are.
            Text(words.appName, style = MaterialTheme.typography.displaySmall)
            Text(words.heroSub, style = MaterialTheme.typography.bodyLarge, color = Ink.muted)
        }

        OfflineBanner()

        TripToggle(
            roundTrip = roundTrip,
            onChange = { wantsReturn ->
                roundTrip = wantsReturn
                // Switching to one way drops a return that is no longer part
                // of the question being asked.
                if (!wantsReturn) back = null
            },
        )

        // One card, not five loose boxes. The four fields and the button are
        // a single question, and a card that holds them says so; separately
        // they read as five unrelated settings on a form.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.md))
                .background(Ink.surface)
                .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                .padding(Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Field(label = words.from, value = "${cityName(from, lang)} ($from)") { picking = "from" }
            Field(label = words.to, value = "${cityName(to, lang)} ($to)") { picking = "to" }

            // Dates and travellers share a row: both are short answers, and
            // giving each a full-width box pushes the button off the fold on
            // a small phone.
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                Field(
                    label = if (roundTrip) words.dates else words.outbound,
                    value = datesLabel(depart, back, roundTrip, lang, words.chooseDates),
                    muted = depart == null,
                    modifier = Modifier.weight(1f),
                ) { pickingDates = true }

                Field(
                    label = words.travellers,
                    value = "${Money.isolate((adults + children + infants).toString())} · ${cabinName(cabin, words)}",
                    modifier = Modifier.weight(1f),
                ) { pickingTravellers = true }
            }

            Button(
                onClick = {
                    onSearch(
                        SearchQuery(
                            from = from, to = to,
                            departDate = depart.orEmpty(),
                            returnDate = back.takeIf { roundTrip },
                            adults = adults,
                            children = children,
                            infants = infants,
                            cabin = cabin,
                        ),
                    )
                },
                // A round trip without a return is half a question: the server
                // would answer it as a one-way and quote the wrong thing.
                enabled = depart != null && (!roundTrip || back != null) && from != to,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = Space.s1),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) { Text(words.search, style = MaterialTheme.typography.titleMedium) }
        }

        if (recent.isNotEmpty()) {
            Text(
                words.recentSearches.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
            )
            recent.take(3).forEach { past ->
                RecentRow(past, lang) {
                    from = past.from
                    to = past.to
                    roundTrip = past.roundTrip
                    adults = past.adults
                    children = past.children
                    infants = past.infants
                    cabin = past.cabin

                    // A trip whose date has gone is still a useful shortcut -
                    // the route and the passengers are right - so it fills the
                    // form and asks for a new date rather than searching a day
                    // that has passed and coming back with nothing.
                    val stillAhead = past.departDate >= today
                    depart = past.departDate.takeIf { stillAhead }
                    back = past.returnDate?.takeIf { stillAhead }
                    if (stillAhead) onSearch(past)
                }
            }
        }
    }
}

/**
 * A past search, as a shortcut. The price it found is deliberately not here:
 * it was true on the day, and a stale number beside a route reads as a quote.
 */
@Composable
private fun RecentRow(query: SearchQuery, lang: pro.qasdatrip.core.Lang, onPick: () -> Unit) {
    val words = LocalWords.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable(onClick = onPick)
            .padding(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${cityName(query.from, lang)} ${routeArrow(lang)} ${cityName(query.to, lang)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOfNotNull(
                    formatDate(query.departDate, lang),
                    query.returnDate?.let { formatDate(it, lang) },
                    Money.isolate(query.travellers.toString()) + " · " + cabinName(query.cabin, words),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * A field inside the search card: its label above, its answer below, in a
 * box with an 8dp corner. Not a text input — every one of these opens a
 * picker, because every one of them has a wrong answer that a keyboard
 * would happily accept.
 */
@Composable
private fun Field(
    label: String,
    value: String,
    muted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s3, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.muted, maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (muted) Ink.muted else Ink.ink,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/**
 * One way or return. Two words, and the difference between them decides
 * whether the calendar asks for one date or two.
 */
@Composable
private fun TripToggle(roundTrip: Boolean, onChange: (Boolean) -> Unit) {
    val words = LocalWords.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surfaceSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to words.oneWay, true to words.roundTrip).forEach { (isReturn, label) ->
            val on = isReturn == roundTrip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (on) Ink.surface else Color.Transparent)
                    .clickable { onChange(isReturn) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (on) Ink.ink else Ink.muted,
                )
            }
        }
    }
}

/**
 * What the dates field says before and after somebody has chosen. A round
 * trip with no return yet still shows the departure, so the field reflects
 * the half-answer rather than pretending nothing was picked.
 */
private fun datesLabel(
    depart: String?,
    back: String?,
    roundTrip: Boolean,
    lang: pro.qasdatrip.core.Lang,
    placeholder: String,
): String = when {
    depart == null -> placeholder
    roundTrip && back != null -> "${formatDate(depart, lang)} – ${formatDate(back, lang)}"
    else -> formatDate(depart, lang)
}

private fun cabinName(cabin: Cabin, words: pro.qasdatrip.core.Words): String = when (cabin) {
    Cabin.ECONOMY -> words.economy
    Cabin.PREMIUM -> words.premium
    Cabin.BUSINESS -> words.business
    Cabin.FIRST -> words.first
}

/**
 * The picker, with the matching from the shared module: أدرار and ادرار are
 * the same query, so are Séville and seville, and an airport answers to the
 * commune the databases file it under as well as to its own name.
 */
@Composable
fun AirportPicker(originSide: Boolean, onPick: (Airport) -> Unit, onDismiss: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) Airports.suggestions(originSide) else Airports.search(query)
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas).padding(Space.s4)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(words.searchCity) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (results.isEmpty()) {
            Text(
                words.noAirport,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
                modifier = Modifier.padding(Space.s4),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(results, key = { i, a -> "$i:${a.iata}" }) { _, airport ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(airport) }
                        .padding(vertical = Space.s3),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        airport.iata,
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.inkSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Ink.surfaceSoft)
                            .padding(horizontal = Space.s2, vertical = Space.s1),
                    )
                    Column {
                        Text(airport.cityIn(lang), style = MaterialTheme.typography.titleMedium)
                        if (airport.name.isNotEmpty()) {
                            Text(airport.name, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
                        }
                    }
                }
            }
        }
    }
}
