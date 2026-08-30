package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Alert
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.routeArrow

/**
 * Everything the app has told this person, newest first.
 *
 * A push is read on a lock screen in about a second and then it is gone, so
 * there has to be somewhere it went. This is that place: the same news, in
 * full, still linked to the search that produced it.
 *
 * Unread lives on this phone and nowhere else. Whether a notification has
 * been seen is the phone's business, and the moment that is stored
 * server-side it becomes a record of what somebody has read — so the dot is
 * drawn from a timestamp in this device's own preferences and nothing about
 * it is ever sent back. Alongside it is what actually happened — the price then, the price now, and how
 * long ago the price was seen, because a number from six hours ago is not a
 * quote and this screen must not read like one.
 */
@Composable
fun NotificationsScreen(
    state: TrackingViewModel.State,
    onLoad: () -> Unit,
    onOpen: (Alert) -> Unit,
    onBack: () -> Unit,
    /**
     * When this phone last opened this screen, as epoch millis.
     *
     * Unread is computed here and stored here, on the device, and never sent
     * anywhere. That keeps the dot the design asks for without the server
     * ever holding a record of what somebody has read — which is the line
     * this product does not cross.
     */
    seenAt: Long = 0L,
    onMarkAllRead: () -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    // Re-read on every visit rather than trusting what was here last time: a
    // phone that was offline when a price moved should see it the first time
    // it can, not the first time it happens to be launched afterwards.
    LaunchedEffect(Unit) { onLoad() }

    val unread = state.alerts.count { (parseInstant(it.at) ?: 0L) > seenAt }

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(
            title = words.notificationsTitle,
            onBack = onBack,
            // Offered only when it would do something. "Mark all read" over
            // a list with nothing unread is a button that reports success at
            // having changed nothing.
            actionLabel = words.markAllRead.takeIf { unread > 0 },
            onAction = onMarkAllRead,
        )

        if (state.alertsLoading && state.alerts.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Space.s6),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(strokeWidth = 2.dp, color = Ink.accentDeep) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Space.s6),
        ) {
            itemsIndexed(state.alerts, key = { i, a -> "$i:${a.id}" }) { _, alert ->
                AlertRow(
                    alert = alert,
                    unread = (parseInstant(alert.at) ?: 0L) > seenAt,
                    onClick = { onOpen(alert) },
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
            }

            if (state.alerts.isEmpty() && !state.alertsLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Space.s6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s3),
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Ink.surfaceSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = Ink.muted,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(words.noNotifications, style = MaterialTheme.typography.titleLarge, color = Ink.ink)
                        Text(
                            words.noNotificationsSub,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Text(
                    words.notificationsFoot,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    modifier = Modifier.padding(Space.s4),
                )
            }
        }
    }
}

/**
 * One message.
 *
 * The route and date are the first thing on the row, not the last: somebody
 * with three watches is looking for which one this is about before they care
 * what happened to it.
 */
@Composable
private fun AlertRow(alert: Alert, unread: Boolean, onClick: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val seat = alert.seat

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The tint is the second half of the same signal as the dot. One
            // of them alone is easy to miss on a list read at a glance; a
            // dot that is also a lighter row is not.
            .background(if (unread) Ink.surface else Ink.canvas)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (seat) Ink.noticeSoft else Ink.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (seat) {
                Icon(
                    Icons.Outlined.Notifications,
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
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                if (seat) words.alertSeatTitle else words.alertDropTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Ink.ink,
            )
            Text(
                "${cityName(alert.origin, lang)} ${routeArrow(lang)} ${cityName(alert.destination, lang)}" +
                    (alert.departDate?.let { " · ${formatDate(it, lang)}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.inkSoft,
            )
            Text(
                text = when {
                    seat -> words.alertSeatBody.replace(
                        "{price}", Money.format(alert.price ?: 0.0, lang),
                    )
                    alert.saved != null -> words.alertDropBody
                        .replace("{price}", Money.format(alert.price ?: 0.0, lang))
                        .replace("{saved}", Money.format(alert.saved!!, lang))
                    else -> Money.format(alert.price ?: 0.0, lang)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
            // How old this is, in the only unit that matters here. A price
            // seen three days ago is not a price, and the row has to say so
            // before somebody acts on it.
            relativeTime(alert.at, words)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
            }
        }
        if (unread) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Ink.accentUi),
            )
        }
    }
}
