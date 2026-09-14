package pro.qasdatrip.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.routeArrow

/**
 * Start watching a route and a date.
 *
 * This screen used to ask for two things and both were wrong.
 *
 * It asked for an email address, because the feature was built for a website
 * and a website has no other way to reach somebody later. A phone does. The
 * install registers itself, so there is nothing to type, nothing to confirm,
 * and no inbox in the path.
 *
 * And it asked for a target price — "tell me when it drops below…" — which
 * sounds helpful and is a trap. Somebody who names a figure the fare never
 * reaches has set an alert that can never fire, and nothing anywhere tells
 * them so; they simply conclude the feature does not work. The number that
 * actually matters is the one they were looking at when they decided to
 * watch, and the app already knows it. So the rule is stated rather than
 * asked for: cheaper than what you saw.
 *
 * That leaves exactly two reasons to be told anything, and this screen says
 * both of them in plain words with no controls attached. There is nothing to
 * fill in, which is the point — the only decision left is whether to do it.
 */
@Composable
fun TrackScreen(
    query: SearchQuery,
    /** The cheapest price on screen, or null when the search found nothing. */
    seenPrice: Double?,
    state: TrackingViewModel.State,
    onTrack: (Double?) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Tracking still starts if permission was refused. The Settings page
        // explains how to enable delivery later, and the watch must not be
        // lost merely because a system dialog was dismissed.
        onTrack(seenPrice)
    }
    val words = LocalWords.current
    val lang = LocalLang.current
    val soldOut = seenPrice == null

    if (state.stage == TrackingViewModel.Stage.SENT) {
        Confirmation(onDone = onDone)
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(title = words.trackRoute, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            item {
                Card {
                    Text(
                        "${cityName(query.from, lang)} ${routeArrow(lang)} ${cityName(query.to, lang)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Ink.ink,
                    )
                    Text(
                        listOfNotNull(
                            formatDateLong(query.departDate, lang),
                            query.returnDate?.let { formatDate(it, lang) },
                            travellersOf(query, words),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.muted,
                    )
                }
            }

            item {
                Text(
                    words.trackRulesLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                )
            }

            item {
                Card(gap = Space.s4) {
                    if (soldOut) {
                        // Nothing to compare against, because nothing was
                        // quoted. This is the case a price alert cannot
                        // express at all, and the one people ask for most.
                        Rule(
                            icon = null,
                            tone = Ink.notice,
                            wash = Ink.noticeSoft,
                            title = words.trackSeatTitle,
                            body = words.trackSeatBody,
                        )
                    } else {
                        Rule(
                            icon = R.drawable.ic_trending_down,
                            tone = Ink.accentDeep,
                            wash = Ink.accentSoft,
                            title = words.trackRulePrice.replace(
                                "{price}", Money.format(seenPrice, lang),
                            ),
                            body = words.trackRulePriceBody,
                        )
                        Rule(
                            icon = null,
                            tone = Ink.notice,
                            wash = Ink.noticeSoft,
                            title = words.trackRuleSeat,
                            body = words.trackRuleSeatBody,
                        )
                    }
                }
            }

            item {
                // The switch the design draws, wired to the only thing it
                // could honestly control.
                //
                // It cannot mean "track without telling me", because that is
                // a watch that does nothing. What it can mean is whether this
                // app is allowed to reach you at all, which is the phone's
                // setting and the actual reason a tracked route can go quiet.
                // So it reads that, and tapping it opens the page where it is
                // changed rather than pretending to change it here.
                val context = LocalContext.current
                val lifecycle = LocalLifecycleOwner.current
                var allowed by remember { mutableStateOf(notificationsAllowed(context)) }

                // Re-read on the way back from the settings page, which is
                // the only place it can have changed.
                DisposableEffect(lifecycle) {
                    val watcher = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) allowed = notificationsAllowed(context)
                    }
                    lifecycle.lifecycle.addObserver(watcher)
                    onDispose { lifecycle.lifecycle.removeObserver(watcher) }
                }

                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.s3),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                words.trackNotifications,
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink.ink,
                            )
                            Text(
                                if (allowed) words.trackNotificationsBody else words.notificationsOff,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (allowed) Ink.muted else Ink.alert,
                            )
                        }
                        Switch(
                            checked = allowed,
                            onCheckedChange = { openNotificationSettings(context) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Ink.accentUi,
                                checkedBorderColor = Ink.accentUi,
                                uncheckedThumbColor = Ink.surface,
                                uncheckedTrackColor = Ink.surfaceSoft,
                                uncheckedBorderColor = Ink.lineStrong,
                            ),
                        )
                    }
                }
            }

            item {
                Text(
                    words.trackPrivacy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                )
            }

            if (state.stage == TrackingViewModel.Stage.FAILED) {
                item {
                    Text(
                        words.trackFailed,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink.alert,
                    )
                }
            }

            item { Box(modifier = Modifier.height(Space.s4)) }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(Ink.surface).padding(Space.s4),
        ) {
            val sending = state.stage == TrackingViewModel.Stage.SENDING
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else onTrack(seenPrice)
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.solid,
                    contentColor = Ink.onSolid,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = Ink.onSolid,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(words.trackOn, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * What "done" looks like when there is no confirmation mail to wait for.
 *
 * The old flow ended on "check your inbox", which is an instruction. This
 * ends on a fact, because the watch exists the moment the server answers.
 */
@Composable
private fun Confirmation(onDone: () -> Unit) {
    val words = LocalWords.current
    Column(
        modifier = Modifier.fillMaxSize().background(Ink.canvas).padding(Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Ink.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = Ink.accentDeep,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            words.trackDone,
            style = MaterialTheme.typography.headlineSmall,
            color = Ink.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            words.trackDoneBody,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.muted,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = Space.s3),
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.solid, contentColor = Ink.onSolid),
        ) { Text(words.myTracking, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun Card(gap: androidx.compose.ui.unit.Dp = Space.s2, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) { content() }
}

/**
 * One reason the phone will buzz, stated and not configurable.
 *
 * A toggle here would imply the two rules are independent choices. They are
 * not: watching a price and watching for a seat are what the same watch does
 * at the two moments each can happen, and offering to turn one off would be
 * offering to be told less about the thing somebody just asked to be told
 * about.
 */
@Composable
private fun Rule(
    @androidx.annotation.DrawableRes icon: Int?,
    tone: Color,
    wash: Color,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(wash),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
    }
}

/** "1 voyageur", "3 voyageurs" — never "1 voyageurs". */
private fun travellersOf(q: SearchQuery, words: pro.qasdatrip.core.Words): String =
    if (q.travellers == 1) words.travellerOne
    else words.travellersMany.replace("{n}", Money.isolate(q.travellers.toString()))


/** Whether this phone will show anything we send it. */
private fun notificationsAllowed(context: android.content.Context): Boolean =
    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

/** The system page where that is turned back on. */
private fun openNotificationSettings(context: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
