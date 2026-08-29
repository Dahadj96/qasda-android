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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money

/**
 * Compte — the fourth tab, and the one that was missing.
 *
 * The app had a Settings tab, which is a developer's idea of a destination.
 * Nobody opens an app to visit its settings; they open it to see what is
 * theirs. So the tab is Compte, it opens on what this install actually
 * holds, and Réglages is one row inside it.
 *
 * There is no sign-in, and this screen says so in the first sentence rather
 * than offering a button that leads to one. Nothing here needs an account:
 * the searches, the tracking and the preferences live on the device, and
 * asking for an address to keep them would be asking for something we do
 * not need.
 */
@Composable
fun AccountScreen(
    lang: Lang,
    chosenLang: Lang?,
    versionName: String,
    activeWatches: Int,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onOpen: (path: String) -> Unit,
) {
    val words = LocalWords.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ink.canvas),
        contentPadding = PaddingValues(start = Space.s4, end = Space.s4, bottom = Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        item {
            QasdaAppBar(
                title = words.navAccount,
                large = true,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Ink.surface)
                    .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                    .padding(Space.s4),
                verticalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Ink.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Ink.accentDeep,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(words.thisDevice, style = MaterialTheme.typography.titleLarge, color = Ink.ink)
                        Text(
                            words.thisDeviceSub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.muted,
                        )
                    }
                }
                // One number, and only when it is true. A row of zeroes
                // dressed as statistics is decoration.
                if (activeWatches > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(Ink.surfaceSoft)
                            .clickable(onClick = onSettings)
                            .padding(Space.s3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    ) {
                        Text(
                            Money.isolate(activeWatches.toString()),
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink.accentDeep,
                        )
                        Text(
                            words.myTracking,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink.inkSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item { GroupLabel(words.preferences) }
        item {
            CardGroup {
                RowLink(
                    title = words.language,
                    value = nameOfLang(chosenLang) ?: words.systemLanguage,
                    onClick = onSettings,
                )
                HairLine()
                RowLink(
                    title = words.settingsAndAlerts,
                    subtitle = words.settingsAndAlertsSub,
                    onClick = onSettings,
                )
            }
        }

        item { GroupLabel(words.helpAndInfo) }
        item {
            CardGroup {
                RowLink(title = words.aboutTitle, onClick = onAbout)
                HairLine()
                RowLink(title = words.howItWorks, onClick = { onOpen(sitePath("how", lang)) })
                HairLine()
                RowLink(title = words.privacy, onClick = { onOpen(sitePath("privacy", lang)) })
                HairLine()
                RowLink(title = words.contactUs, onClick = { onOpen("/${lang.tag}/") })
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

private fun nameOfLang(lang: Lang?): String? = when (lang) {
    Lang.FR -> "Français"
    Lang.AR -> "العربية"
    Lang.EN -> "English"
    null -> null
}

/**
 * The site's own URL words, transliterated rather than translated into
 * Arabic script so a link survives being pasted into WhatsApp. The app has
 * to use the same ones or it links to a 404.
 */
private fun sitePath(page: String, lang: Lang): String {
    val slug = when (page) {
        "how" -> mapOf(Lang.FR to "comment-ca-marche", Lang.AR to "kayf-yaamal", Lang.EN to "how-it-works")
        else -> mapOf(Lang.FR to "confidentialite", Lang.AR to "khususiya", Lang.EN to "privacy")
    }
    return "/${lang.tag}/${slug[lang]}"
}

@Composable
private fun GroupLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Ink.muted)
}

@Composable
private fun CardGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
    ) { content() }
}

@Composable
private fun HairLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s4)
            .height(1.dp)
            .background(Ink.line),
    )
}

@Composable
private fun RowLink(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink.ink)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        value?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = Ink.inkSoft, maxLines = 1)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}
