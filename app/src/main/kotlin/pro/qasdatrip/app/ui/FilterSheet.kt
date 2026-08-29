package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Filters
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.TimeBand
import kotlin.math.roundToInt

/**
 * The filters, over the flights already on the screen.
 *
 * Nothing here goes back to the server. The search is a stream that took time
 * and four sites to produce, and re-running it to ask for morning flights
 * would throw that away to answer a question the list already answers.
 *
 * The count on the button is the honest one: it is computed against the same
 * rules the list will apply, so a filter set that would empty the screen says
 * so before it is applied rather than after.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    current: Filters,
    priceCeiling: Double?,
    matchCount: (Filters) -> Int,
    onDismiss: () -> Unit,
    onApply: (Filters) -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember { mutableStateOf(current) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Space.s4, end = Space.s4, bottom = Space.s6),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(words.filters, style = MaterialTheme.typography.headlineSmall)
                if (!draft.isEmpty) {
                    Text(
                        words.reset,
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink.accentDeep,
                        modifier = Modifier.clickable { draft = Filters() },
                    )
                }
            }

            // "At most this many stops", so each chip is exclusive of the
            // others: two of them at once would be a range with a floor, and
            // nobody wants flights with at least one stop.
            Section(words.stopsLabel) {
                Choice(words.direct, draft.maxStops == 0) {
                    draft = draft.copy(maxStops = if (draft.maxStops == 0) null else 0)
                }
                Choice(words.stopsOne, draft.maxStops == 1) {
                    draft = draft.copy(maxStops = if (draft.maxStops == 1) null else 1)
                }
                Choice(words.stopsTwoPlus, draft.maxStops == 2) {
                    draft = draft.copy(maxStops = if (draft.maxStops == 2) null else 2)
                }
            }

            Section(words.departureTime) {
                listOf(
                    TimeBand.MORNING to words.morning,
                    TimeBand.AFTERNOON to words.afternoon,
                    TimeBand.EVENING to words.evening,
                    TimeBand.NIGHT to words.night,
                ).forEach { (band, label) ->
                    Choice(label, band in draft.departBands) {
                        draft = draft.copy(
                            departBands = draft.departBands.toMutableSet()
                                .apply { if (!add(band)) remove(band) },
                        )
                    }
                }
            }

            // One chip, not the design's pair. "Cabin bag only" as a second
            // chip has to mean something, and the only thing it could mean is
            // "show me fares that do NOT include a bag", which nobody wants.
            // Drawn as a pair, the unselected half would also be lit whenever
            // no baggage filter was set at all.
            Section(words.baggage) {
                Choice(words.bagIncluded, draft.bagOnly) { draft = draft.copy(bagOnly = !draft.bagOnly) }
            }

            if (priceCeiling != null && priceCeiling > 0) {
                val chosen = draft.maxPrice ?: priceCeiling
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            words.maxPrice.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink.muted,
                        )
                        Text(Money.format(chosen, lang), style = MaterialTheme.typography.titleMedium)
                    }
                    Slider(
                        value = chosen.toFloat(),
                        onValueChange = { picked ->
                            // Snapping back to null at the top matters: "no
                            // ceiling" and "a ceiling that happens to be the
                            // most expensive flight" behave the same today and
                            // stop behaving the same the moment a dearer
                            // flight arrives from a slower site.
                            val rounded = (picked / STEP).roundToInt() * STEP
                            draft = draft.copy(
                                maxPrice = rounded.toDouble().takeIf { it < priceCeiling },
                            )
                        },
                        valueRange = 0f..priceCeiling.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Ink.accentUi,
                            activeTrackColor = Ink.accentUi,
                            inactiveTrackColor = Ink.surfaceSoft,
                        ),
                    )
                }
            }

            val matches = matchCount(draft)
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = matches > 0,
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                Text(
                    if (matches > 0) {
                        words.showCount.replace("{n}", Money.isolate(matches.toString()))
                    } else {
                        words.noMatchTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private const val STEP = 1_000

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Ink.muted)
        // Four bands do not fit on one line of a 360dp phone in every
        // language, and "Après-midi" is the one that proves it.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) { content() }
    }
}

@Composable
private fun Choice(label: String, on: Boolean, onToggle: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (on) Ink.accentDeep else Ink.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(if (on) Ink.accentSoft else Ink.surface)
            .border(1.dp, if (on) Ink.accentUi else Ink.lineStrong, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
