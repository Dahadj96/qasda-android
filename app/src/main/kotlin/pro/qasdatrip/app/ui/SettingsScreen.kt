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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

        item { Label(words.language) }
        item {
            Group {
                // Null first, and named for what it does rather than for a
                // language: it is the only option that keeps following the
                // phone when the phone changes.
                Choice(words.systemLanguage, current == null) { onLanguage(null) }
                Divider()
                Lang.entries.forEachIndexed { index, option ->
                    Choice(nameOf(option), current == option) { onLanguage(option) }
                    if (index < Lang.entries.size - 1) Divider()
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
