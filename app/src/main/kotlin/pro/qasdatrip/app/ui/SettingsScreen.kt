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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money

/**
 * Language, and the pages that are read rather than operated.
 *
 * The legal and about pages open on the site instead of being copied here.
 * They are long, they are rarely read, and a privacy policy that exists in
 * two places is a privacy policy that will one day say two things. The FAQ
 * is the exception, and it is the exception because it is the page somebody
 * needs when their booking has gone wrong and their signal has gone with it.
 */
@Composable
fun SettingsScreen(
    current: Lang?,
    effective: Lang,
    versionName: String,
    onLanguage: (Lang?) -> Unit,
    onOpen: (path: String) -> Unit,
    onAbout: () -> Unit = {},
    onBack: () -> Unit = {},
    alertDrops: Boolean = true,
    alertSeats: Boolean = true,
    alertEnded: Boolean = true,
    onAlertDrops: (Boolean) -> Unit = {},
    onAlertSeats: (Boolean) -> Unit = {},
    onAlertEnded: (Boolean) -> Unit = {},
    recentCount: Int = 0,
    watchCount: Int = 0,
    onClearRecent: () -> Unit = {},
    onStopAllWatches: () -> Unit = {},
) {
    val words = LocalWords.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ink.canvas),
        contentPadding = PaddingValues(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        // Réglages is reached from Compte now, so it is a page you walked
        // into and it owes you a way out.
        item {
            QasdaAppBar(
                title = words.navSettings,
                large = true,
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        // Which alerts may interrupt you.
        //
        // These silence the message, never the watching: a route you asked
        // us to follow stays followed while its notifications are off, and
        // turning them back on shows you what happened in the meantime. A
        // switch that quietly cancelled the watch would be the app throwing
        // away work somebody asked for.
        item { Label(words.notificationsTitle) }
        item {
            Group {
                Toggle(words.prefDrops, words.prefDropsSub, alertDrops, onAlertDrops)
                Divider()
                Toggle(words.prefSeats, words.prefSeatsSub, alertSeats, onAlertSeats)
                Divider()
                Toggle(words.prefEnded, words.prefEndedSub, alertEnded, onAlertEnded)
            }
        }
        item {
            Text(
                words.prefMutedNote,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
        }

        item { Label(words.display) }
        item {
            Group {
                // Null first, and named for what it does rather than for a
                // language: it is the only option that keeps following the
                // phone when the phone changes.
                Choice(words.systemLanguage, current == null) { onLanguage(null) }
                Divider()
                Lang.entries.forEach { option ->
                    Choice(nameOf(option), current == option) { onLanguage(option) }
                    Divider()
                }
                // Stated, not offered: every site this app reads quotes in
                // dinars, so a picker here would change nothing.
                Stated(words.currency, words.currencyDzd)
            }
        }

        // What this device is holding, and how to make it stop.
        //
        // Both of these are the only copy there is. Nothing here is on a
        // server under an account, so this page is the only place somebody
        // can undo what the app has remembered about them.
        item { Label(words.dataSection) }
        item {
            Group {
                Action(
                    label = words.clearRecent,
                    // "1 searches kept on this device" is the sort of thing
                    // that makes an app look machine-written.
                    subtitle = when (recentCount) {
                        0 -> words.nothingToClear
                        1 -> words.clearRecentOneSub
                        else -> words.clearRecentSub.replace("{n}", Money.isolate(recentCount.toString()))
                    },
                    enabled = recentCount > 0,
                    onClick = onClearRecent,
                )
                if (watchCount > 0) {
                    Divider()
                    Action(
                        label = words.stopAllWatches,
                        subtitle = if (watchCount == 1) {
                            words.stopAllWatchesOneSub
                        } else {
                            words.stopAllWatchesSub.replace("{n}", Money.isolate(watchCount.toString()))
                        },
                        enabled = true,
                        destructive = true,
                        onClick = onStopAllWatches,
                    )
                }
            }
        }

        item { Label(words.about) }
        item {
            Group {
                // About is a screen now rather than a link off to the site.
                // It is the page that says we do not sell the ticket, and
                // that sentence should not need a working connection.
                Link(words.aboutTitle, onAbout)
                Divider()
                Link(words.howItWorks) { onOpen(pathOf("how", effective)) }
                Divider()
                Link(words.privacy) { onOpen(pathOf("privacy", effective)) }
            }
        }

        item {
            Text(
                "${words.version} ${Money.isolate(versionName)}",
                style = MaterialTheme.typography.labelSmall,
                color = Ink.muted,
                modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
            )
        }
    }
}

/**
 * The site's own URL words. They are transliterated rather than translated
 * into Arabic script so a link survives being pasted into WhatsApp, and the
 * app has to use the same ones or it links to a 404.
 */
private fun pathOf(page: String, lang: Lang): String {
    val slug = when (page) {
        "how" -> mapOf(Lang.FR to "comment-ca-marche", Lang.AR to "kayf-yaamal", Lang.EN to "how-it-works")
        "about" -> mapOf(Lang.FR to "a-propos", Lang.AR to "man-nahnu", Lang.EN to "about")
        else -> mapOf(Lang.FR to "confidentialite", Lang.AR to "khususiya", Lang.EN to "privacy")
    }
    return "/${lang.tag}/${slug[lang]}"
}

private fun nameOf(lang: Lang): String = when (lang) {
    Lang.FR -> "Français"
    Lang.AR -> "العربية"
    Lang.EN -> "English"
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s4)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(1.dp)
                .background(Ink.line),
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Ink.accentSoft else Ink.surface)
            .clickable(onClick = onClick)
            .padding(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Ink.accentDeep else Ink.ink,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(if (selected) Ink.accentUi else Ink.surface)
                .border(
                    width = if (selected) 5.dp else 1.dp,
                    color = if (selected) Ink.accentUi else Ink.lineStrong,
                    shape = RoundedCornerShape(Radius.pill),
                ),
        )
    }
}

@Composable
private fun Link(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A row that turns something on or off. */
@Composable
private fun Toggle(
    label: String,
    subtitle: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!on) }
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
        Switch(
            checked = on,
            onCheckedChange = onChange,
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

/** A row that says something and does nothing. No chevron: it does not lead anywhere. */
@Composable
private fun Stated(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Ink.inkSoft, maxLines = 1)
    }
}

/**
 * A row that does something irreversible.
 *
 * Red for the destructive one, and greyed rather than hidden when there is
 * nothing to act on — a control that disappears when the list empties makes
 * people wonder where it went.
 */
@Composable
private fun Action(
    label: String,
    subtitle: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !enabled -> Ink.muted
                    destructive -> Ink.alert
                    else -> Ink.ink
                },
            )
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Ink.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
