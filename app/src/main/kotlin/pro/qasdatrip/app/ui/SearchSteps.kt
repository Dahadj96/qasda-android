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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import pro.qasdatrip.core.Words
import pro.qasdatrip.core.routeArrow

/**
 * How far in you are, drawn as four segments.
 *
 * The search is four answers — from, to, when, who — and it used to be four
 * dialogs stacked on one screen, which gives no sense of progress and no way
 * to see what you have already said. Four pages need this bar, because a
 * page that appears with no context reads as the app having jumped somewhere
 * rather than having moved forward.
 */
@Composable
fun StepperBar(step: Int, total: Int = 4, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (index < step) Ink.solid else Ink.line),
            )
        }
    }
}

/**
 * The head of every step: a way back, the question in plain words, the
 * progress bar, and — from step two onward — the answers already given, so
 * nobody has to remember what they typed on the previous page.
 */
@Composable
private fun StepHeader(
    title: String,
    step: Int,
    subtitle: String?,
    onBack: () -> Unit,
) {
    Column {
        QasdaAppBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier.padding(start = Space.s4, end = Space.s4, bottom = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            // Step 0 means "this page is not part of the four". The picker
            // is reused from Compte to set a home airport, and a progress bar
            // there would promise three more questions that never come.
            if (step > 0) StepperBar(step = step)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
            }
        }
    }
}

/**
 * Step one and step two: which airport.
 *
 * A full page rather than the dialog it was, for a reason that shows up on a
 * real phone: the keyboard takes half the screen, and a dialog under a
 * keyboard leaves room for two results. A page leaves room for six, which is
 * the difference between choosing from a list and typing until something
 * appears.
 */
@Composable
fun AirportStepScreen(
    originSide: Boolean,
    step: Int,
    subtitle: String?,
    onPick: (Airport) -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) Airports.suggestions(originSide) else Airports.search(query)
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        StepHeader(
            title = if (originSide) words.fromQuestion else words.toQuestion,
            step = step,
            subtitle = subtitle,
            onBack = onBack,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(words.searchCity, color = Ink.muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink.accentUi,
                unfocusedBorderColor = Ink.line,
                focusedContainerColor = Ink.surface,
                unfocusedContainerColor = Ink.surface,
                cursorColor = Ink.accentDeep,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.s4),
        )
        if (results.isEmpty()) {
            Text(
                words.noAirport,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink.muted,
                modifier = Modifier.padding(Space.s4),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            if (query.isBlank()) {
                item {
                    Text(
                        (if (originSide) words.airportsHere else words.popularDestinations).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.muted,
                    )
                }
            }
            // The index is part of the key: two entries can share an IATA
            // code in a bad data row, and a LazyColumn throws on a repeat.
            itemsIndexed(results, key = { i, a -> "$i:${a.iata}" }) { _, airport ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.surface)
                        .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                        .clickable { onPick(airport) }
                        .padding(horizontal = Space.s3, vertical = Space.s3),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 32.dp)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Ink.surfaceSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            airport.iata,
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink.inkSoft,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            airport.cityIn(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (airport.name.isNotBlank()) {
                            Text(
                                airport.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Step four, wrapped so it looks like a step rather than a sheet. */
@Composable
fun TravellersStepScreen(
    draft: SearchDraft,
    subtitle: String?,
    onApply: (Int, Int, Int, Cabin) -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        StepHeader(title = words.whoQuestion, step = 4, subtitle = subtitle, onBack = onBack)
        TravellersPicker(
            adults = draft.adults,
            children = draft.children,
            infants = draft.infants,
            cabin = draft.cabin,
            onApply = onApply,
            showTitle = false,
        )
    }
}


/** "1 voyageur", "3 voyageurs" — never "1 voyageurs". */
fun travellersLabel(draft: SearchDraft, words: Words): String =
    travellersLabel(draft.travellers, words)

fun travellersLabel(count: Int, words: Words): String =
    if (count == 1) words.travellerOne
    else words.travellersMany.replace("{n}", Money.isolate(count.toString()))

fun cabinLabel(cabin: Cabin, words: Words): String = when (cabin) {
    Cabin.ECONOMY -> words.economy
    Cabin.PREMIUM -> words.premium
    Cabin.BUSINESS -> words.business
    Cabin.FIRST -> words.first
}

/** "Alger → Paris", the line every step after the first carries. */
fun routeLine(draft: SearchDraft, lang: pro.qasdatrip.core.Lang): String =
    "${cityName(draft.from, lang)} ${routeArrow(lang)} ${cityName(draft.to, lang)}"
