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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import pro.qasdatrip.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow
import pro.qasdatrip.core.PriceTrend
import pro.qasdatrip.core.Watch

/**
 * Everything this phone knows it is watching.
 *
 * "This phone" and not "this person", and the difference is the design. The
 * list lives behind a signed link that arrives by email, so a phone that has
 * never opened one has nothing to show — not because there is nothing, but
 * because it has not been told. Saying that plainly, and offering to take
 * the link, is better than an empty list that looks like a bug.
 */
@Composable
fun TrackingScreen(
    state: TrackingViewModel.State,
    onOpen: (Watch) -> Unit,
    onStop: (Long) -> Unit,
    onNew: () -> Unit,
    onNotifications: () -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            item {
                Box {
                    // "Suivi", the same word as the tab underneath it. The
                    // screen called itself "Mon suivi" while the bar called
                    // it "Suivi", which is two names for one place.
                    QasdaAppBar(
                        title = words.navTracking,
                        large = true,
                        actionLabel = words.notificationsTitle,
                        onAction = onNotifications,
                        modifier = Modifier.padding(horizontal = 0.dp),
                    )
                    // Registering counts as loading here: until the install
                    // has an identity there is nothing to list, and a bare
                    // empty state would read as "you have no alerts".
                    if (state.loading || state.registering) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Ink.accentDeep,
                            modifier = Modifier.size(20.dp).align(Alignment.CenterEnd),
                        )
                    }
                }
            }

            if (state.keyRejected) {
                item {
                    Text(words.linkNotValid, style = MaterialTheme.typography.bodyMedium, color = Ink.alert)
                }
            }

            itemsIndexed(state.watches, key = { i, w -> "$i:${w.id}" }) { _, watch ->
                WatchCard(
                    watch = watch,
                    onOpen = { onOpen(watch) },
                    onStop = { onStop(watch.id) },
                )
            }

            if (state.watches.isEmpty() && !state.loading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s3),
                    ) {
                        Text(words.noTracking, style = MaterialTheme.typography.titleMedium)
                        Text(
                            words.noTrackingSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onNew,
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
                    modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
                ) { Text(words.newTracking) }
            }

            // There is no "paste your manage link" box any more.
            //
            // It existed because alerts belonged to a mailbox and a phone had
            // no way to prove which mailbox it was. This install registers
            // itself, so it always knows which watches are its own. A link
            // tapped in an older alert email still lands here — the deep link
            // declared on this destination handles it — but asking somebody to
            // find and paste a URL was never a thing to put in front of them,
            // and now it is not needed at all.
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) { content() }
}

@Composable
private fun WatchCard(watch: Watch, onOpen: () -> Unit, onStop: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val seats = watch.watchingSeats

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable(onClick = onOpen)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${cityName(watch.origin, lang)} ${routeArrow(lang)} ${cityName(watch.destination, lang)}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(watch.departDate, watch.returnDate)
                        .joinToString(" – ") { formatDate(it, lang) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    maxLines = 1,
                )
            }
            // What kind of watch this is, said in one word. A list of four
            // routes all reading "Alger → Paris" needs something at a glance
            // that separates the one waiting for a seat from the three
            // waiting for a price.
            if (seats) {
                Tag(words.soldOut, fg = Ink.notice, bg = Ink.noticeSoft)
            } else {
                Tag(words.statusActive, fg = Ink.accentDeep, bg = Ink.accentSoft)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))

        // The rule, in the sentence somebody would use to describe it — not
        // two numbers labelled "baseline" and "target" that mean nothing
        // without the code that reads them.
        //
        // No number here is a price we are quoting now. It is what this watch
        // is measured against, and re-running the search is the only thing
        // that can say what the route costs today.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalAlignment = Alignment.Top,
        ) {
            if (seats) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = Ink.notice,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_trending_down),
                    contentDescription = null,
                    tint = Ink.accentDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = when {
                    seats -> words.watchingSeat
                    watch.reference != null ->
                        words.watchingPrice.replace("{price}", Money.format(watch.reference!!, lang))
                    else -> words.noTrackingSub
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.inkSoft,
            )
        }

        watch.seenPrice?.let {
            Text(
                words.seenAtSearch.replace("{price}", Money.format(it, lang)),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
        }

        // Two controls, side by side and equal, as drawn. Stopping used to be
        // the only one — a red word alone at the bottom right — which made
        // the card's obvious action the destructive one.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            WatchAction(
                label = words.stopTracking,
                onClick = onStop,
                bordered = false,
                modifier = Modifier.weight(1f),
            )
            WatchAction(
                label = words.seeOffers,
                onClick = onOpen,
                bordered = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 44 tall, half the card wide. Outlined is the one you are meant to press. */
@Composable
private fun WatchAction(
    label: String,
    onClick: () -> Unit,
    bordered: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .then(
                if (bordered) Modifier.border(1.dp, Ink.lineStrong, RoundedCornerShape(Radius.sm))
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Ink.ink,
            maxLines = 1,
        )
    }
}
@Composable
fun PriceHistoryScreen(
    watch: Watch,
    trend: PriceTrend?,
    loading: Boolean,
    onSearch: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        // The shared bar. This screen drew its own bare arrow on a white
        // strip, so the back control here was a different size and shape
        // from the one on every other page.
        QasdaAppBar(
            title = words.priceHistoryTitle,
            subtitle = "${cityName(watch.origin, lang)} ${routeArrow(lang)} " +
                "${cityName(watch.destination, lang)} · ${formatDate(watch.departDate, lang)}",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            if (loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s6),
                        horizontalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator(strokeWidth = 2.dp, color = Ink.accentDeep) }
                }
            }

            if (!loading && (trend == null || trend.points.isEmpty())) {
                item {
                    Panel {
                        Text(words.historyNone, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
                    }
                }
            }

            trend?.takeIf { it.points.isNotEmpty() }?.let { data ->
                item { Text(words.priceHistory.replace("{n}", Money.isolate(data.days.toString())), style = MaterialTheme.typography.labelSmall, color = Ink.muted) }
                item {
                    if (data.worthDrawing) {
                        Bars(data)
                    } else {
                        Panel { Text(words.historyThin, style = MaterialTheme.typography.bodyMedium, color = Ink.muted) }
                    }
                }
                item { Summary(data) }
                item {
                    Text(
                        words.historyObserved.replace("{n}", Money.isolate(data.observedDays.toString())),
                        style = MaterialTheme.typography.labelSmall,
                        color = Ink.muted,
                    )
                }
            }

            item {
                Button(
                    onClick = onSearch,
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(words.seeTodayOffers, style = MaterialTheme.typography.titleMedium) }
            }
            // At the bottom, in red, away from the thumb's resting place.
            // It used to sit in the top bar a finger's width from the back
            // arrow, which is a bad place for the one control here that
            // cannot be undone.
            item {
                Text(
                    words.stopTracking,
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink.alert,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.pill))
                        .clickable(onClick = onStop)
                        .padding(vertical = Space.s3),
                )
            }
        }
    }
}

private val ChartHeight = 140.dp
private val BarWidth = 10.dp

/**
 * One bar per day we have a reading for, newest last.
 *
 * Heights are scaled against the observed low and high rather than zero: the
 * question somebody has is whether today is dear or cheap for this route, and
 * a bar chart anchored at zero answers it with forty identical columns.
 * The last bar is the most recent reading and is drawn in the accent.
 */
@Composable
private fun Bars(trend: PriceTrend) {
    val lang = LocalLang.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ChartHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Space.s1, Alignment.CenterHorizontally),
        ) {
            trend.points.forEachIndexed { index, point ->
                // A floor of a fifth, so the cheapest day is still a bar and
                // not an invisible sliver on the axis.
                val share = trend.position(point.cheapest) ?: 0.5f
                val newest = index == trend.points.lastIndex
                Box(
                    modifier = Modifier
                        .width(BarWidth)
                        .height(ChartHeight * (0.2f + 0.8f * share))
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(if (newest) Ink.accentUi else Ink.line),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatDate(trend.points.first().date, lang),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
            )
            Text(
                formatDate(trend.points.last().date, lang),
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
            )
        }
    }
}

@Composable
private fun Summary(trend: PriceTrend) {
    val words = LocalWords.current
    val lang = LocalLang.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        listOf(
            words.historyLow to trend.low,
            words.historyAverage to trend.average,
            words.historyHigh to trend.high,
        ).forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Ink.surface)
                    .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                    .padding(Space.s3),
                verticalArrangement = Arrangement.spacedBy(Space.s1),
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
                Text(
                    value?.let { Money.format(it, lang) } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
