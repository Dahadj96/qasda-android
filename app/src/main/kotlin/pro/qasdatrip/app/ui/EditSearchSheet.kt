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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space

/**
 * Change the search without losing the answers.
 *
 * The pencil on the results used to walk somebody back to the front of the
 * app, past the prices they were reading. This is what it opens instead: a
 * sheet over the results, with the list still visible behind it, holding the
 * three edits people actually make on a results screen.
 *
 * The day steppers are the reason it exists. On a route out of Algiers most
 * of the difference in price is which day you fly, and the old app made
 * finding that out a five-page round trip through a calendar. A tap either
 * side of the date is the whole gesture now, and on a return trip the
 * outbound stepper carries the return with it so the length of the holiday
 * is preserved - "the same trip, a day earlier" rather than two separate
 * edits that have to agree.
 *
 * Everything here applies to the search in place. Nothing on this sheet
 * navigates except the rows that genuinely need a page of their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSearchSheet(
    draft: SearchDraft,
    onDismiss: () -> Unit,
    onRoundTrip: (Boolean) -> Unit,
    onSwap: () -> Unit,
    onShiftDepart: (Long) -> Unit,
    onShiftReturn: (Long) -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onPickDates: () -> Unit,
    onPickTravellers: () -> Unit,
    onApply: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val haptics = LocalHaptics.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        // On the canvas colour the sheet was invisible: same fill as the page
        // behind it, no scrim, so it read as the results screen having grown
        // a form. A lifted surface and an explicit scrim put it in front.
        containerColor = Ink.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.s4)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = Space.s3)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Ink.lineStrong)
                    .align(Alignment.CenterHorizontally),
            )

            Text(
                words.edit,
                style = MaterialTheme.typography.titleLarge,
                color = Ink.ink,
                modifier = Modifier.padding(top = Space.s2),
            )

            // The route, with the swap where the journey turns around.
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.surfaceSoft)
                        .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
                ) {
                    PlaceRow(words.from, "${cityName(draft.from, lang)} (${draft.from})", onPickFrom)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
                    PlaceRow(words.to, "${cityName(draft.to, lang)} (${draft.to})", onPickTo)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = Space.s3)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Ink.canvas)
                        .border(1.dp, Ink.lineStrong, CircleShape)
                        .clickable {
                            haptics.play(Feedback.Commit)
                            onSwap()
                        }
                        .semantics { contentDescription = words.swapRoute },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_swap),
                        contentDescription = null,
                        tint = Ink.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            TripToggleRow(roundTrip = draft.roundTrip, onChange = onRoundTrip)

            // A day either side, which is where the money is.
            DayStepper(
                label = words.outbound,
                value = draft.depart?.let { formatDate(it, lang) } ?: words.chooseDates,
                onEarlier = { onShiftDepart(-1) },
                onLater = { onShiftDepart(1) },
                enabled = draft.depart != null,
            )
            if (draft.roundTrip) {
                DayStepper(
                    label = words.inbound,
                    value = draft.back?.let { formatDate(it, lang) } ?: words.pickReturn,
                    onEarlier = { onShiftReturn(-1) },
                    onLater = { onShiftReturn(1) },
                    enabled = draft.back != null,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Ink.surfaceSoft)
                    .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
            ) {
                PlaceRow(
                    words.dates,
                    listOfNotNull(
                        draft.depart?.let { formatDate(it, lang) },
                        draft.back?.takeIf { draft.roundTrip }?.let { formatDate(it, lang) },
                    ).joinToString(" – ").ifBlank { words.chooseDates },
                    onPickDates,
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
                PlaceRow(words.travellers, travellersLabel(draft, words), onPickTravellers)
            }

            Button(
                onClick = {
                    haptics.play(Feedback.Commit)
                    onApply()
                },
                enabled = draft.complete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = Space.s1),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.solid,
                    contentColor = Ink.onSolid,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) { Text(words.search, style = MaterialTheme.typography.titleMedium) }

            Box(modifier = Modifier.height(Space.s4))
        }
    }
}

@Composable
private fun PlaceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = Ink.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TripToggleRow(roundTrip: Boolean, onChange: (Boolean) -> Unit) {
    val words = LocalWords.current
    val haptics = LocalHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.canvas)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(true to words.roundTrip, false to words.oneWay).forEach { (wantsReturn, label) ->
            val on = wantsReturn == roundTrip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (on) Ink.surface else Color.Transparent)
                    .clickable {
                        haptics.play(Feedback.Selection)
                        onChange(wantsReturn)
                    }
                    .padding(vertical = 9.dp),
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

/** One day either side of a date, without opening anything. */
@Composable
private fun DayStepper(
    label: String,
    value: String,
    enabled: Boolean,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
) {
    val haptics = LocalHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surfaceSoft)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(horizontal = Space.s2, vertical = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepArrow(forward = false, enabled = enabled) {
            haptics.play(Feedback.Selection)
            onEarlier()
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Ink.ink else Ink.muted,
                maxLines = 1,
            )
        }
        StepArrow(forward = true, enabled = enabled) {
            haptics.play(Feedback.Selection)
            onLater()
        }
    }
}

@Composable
private fun StepArrow(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Ink.canvas)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = if (enabled) Ink.ink else Ink.lineStrong,
            modifier = Modifier.size(24.dp),
        )
    }
}
