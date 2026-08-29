package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.looksLikeEmailShape

/**
 * Ask to be told when this route and date gets cheaper.
 *
 * The design put three toggles here — any drop, a threshold, and a seat
 * freeing up on a full flight. Two of those are real: the server watches
 * every route and date it has a watch for, and compares against a target
 * price when one is set. The third is not built, and a switch that does
 * nothing is worse than a switch that is missing, so it is not drawn.
 *
 * The route and the date are the subject, never a particular flight. The
 * 14:25 with one stop that a site quoted this morning can be gone by
 * tonight; "Alger to Paris on the 12th" cannot.
 */
@Composable
fun TrackScreen(
    query: SearchQuery,
    currentCheapest: Double?,
    state: TrackingViewModel.State,
    onCreate: (email: String, target: Double?) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    var email by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    val emailOk = looksLikeEmailShape(email)

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Ink.surface).padding(Space.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Column {
                Text(words.trackTitle, style = MaterialTheme.typography.titleMedium)
                Text(words.trackSub, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            }
        }

        if (state.stage == TrackingViewModel.Stage.SENT) {
            Sent(
                email = state.sentTo.orEmpty(),
                onDone = onDone,
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            item {
                Panel {
                    Text(
                        "${cityName(query.from, lang)} ${routeArrow(lang)} ${cityName(query.to, lang)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        listOfNotNull(query.departDate, query.returnDate)
                            .joinToString(" – ") { formatDate(it, lang) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.muted,
                    )
                    // The price we are starting from, and only when we have
                    // one. A watch created from an empty search has no
                    // baseline, and inventing a number here would make the
                    // first email compare against fiction.
                    currentCheapest?.let {
                        Text(
                            words.whenYouSubscribed.replace("{price}", Money.format(it, lang)),
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink.accentDeep,
                            modifier = Modifier.padding(top = Space.s2),
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; touched = true },
                        label = { Text(words.trackEmail) },
                        placeholder = { Text(words.trackEmailHint) },
                        singleLine = true,
                        isError = touched && email.isNotBlank() && !emailOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(Radius.md),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (touched && email.isNotBlank() && !emailOk) {
                        Text(words.trackBadEmail, style = MaterialTheme.typography.labelSmall, color = Ink.alert)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = target,
                    onValueChange = { new -> target = new.filter { it.isDigit() }.take(9) },
                    label = { Text(words.trackTarget) },
                    placeholder = { Text(words.trackTargetHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(Radius.md),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.stage == TrackingViewModel.Stage.FAILED) {
                item {
                    Text(words.trackFailed, style = MaterialTheme.typography.bodyMedium, color = Ink.alert)
                }
            }

            item {
                Button(
                    onClick = { onCreate(email, target.toDoubleOrNull()) },
                    enabled = emailOk && state.stage != TrackingViewModel.Stage.SENDING,
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
                    modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
                ) {
                    if (state.stage == TrackingViewModel.Stage.SENDING) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Ink.inverse, modifier = Modifier.size(18.dp))
                    } else {
                        Text(words.trackStart)
                    }
                }
            }

            item {
                Text(
                    words.trackNoAccount,
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                )
            }
        }
    }
}

/**
 * The mail is out, and nothing is being watched yet.
 *
 * Saying so is the point. "You are now tracking this route" while the
 * confirmation sits unopened would be the one lie this feature cannot
 * afford: the server does exactly nothing until that link is clicked.
 */
@Composable
private fun Sent(email: String, onDone: () -> Unit) {
    val words = LocalWords.current
    Column(
        modifier = Modifier.fillMaxSize().padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Panel {
            Text(
                words.trackSent.replace("{email}", email),
                style = MaterialTheme.typography.titleMedium,
                color = Ink.accentDeep,
            )
            Text(
                words.trackSentSub,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
                modifier = Modifier.padding(top = Space.s2),
            )
        }
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(words.confirm) }
    }
}

@Composable
internal fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
    ) { content() }
}
