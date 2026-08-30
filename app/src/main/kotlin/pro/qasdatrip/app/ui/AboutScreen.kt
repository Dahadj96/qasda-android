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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money

/**
 * What Qasda is, and what it is not.
 *
 * The second sentence is the one that matters and it is why this screen
 * exists rather than being a line in Settings: somebody who thinks they
 * bought a ticket here will come looking for a refund here. Saying plainly
 * that the booking happens on the agency's own site, before they book, is
 * cheaper for everybody than explaining it afterwards.
 *
 * Every link on this page goes to a page that exists. The design also drew
 * terms of use, a legal notice, an open-source licence list and two social
 * accounts; none of those exist yet, and a row that opens a 404 is worse
 * than a row that is not there.
 */
@Composable
fun AboutScreen(
    lang: Lang,
    versionName: String,
    onOpen: (path: String) -> Unit,
    onFaq: () -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        // The shared bar. This screen drew its own — a bare arrow on a white
        // strip — which made the back control here a different size and a
        // different shape from the one on every other page.
        QasdaAppBar(
            title = words.aboutTitle,
            subtitle = "${words.appName} ${Money.isolate(versionName)}",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            item { Banner(words.appName, words.aboutTagline) }

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
                    Text(words.aboutWhat, style = MaterialTheme.typography.bodyMedium)
                    Text(words.aboutNotTickets, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item { SectionLabel(words.readMore) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.surface)
                        .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
                ) {
                    // The FAQ is the one page kept inside the app: it is what
                    // somebody needs when a booking has gone wrong, which is
                    // often the same moment their signal has gone with it.
                    LinkRow(words.faq, onFaq)
                    HairLine()
                    LinkRow(words.howItWorks) { onOpen(sitePath("how", lang)) }
                    HairLine()
                    LinkRow(words.about) { onOpen(sitePath("about", lang)) }
                    HairLine()
                    LinkRow(words.privacy) { onOpen(sitePath("privacy", lang)) }
                }
            }
        }
    }
}

@Composable
private fun Banner(name: String, tagline: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.ink)
            .padding(vertical = Space.s6, horizontal = Space.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.headlineMedium,
            color = Ink.canvas,
            textAlign = TextAlign.Center,
        )
        Text(
            tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.canvas.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
}

@Composable
private fun HairLine() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s4)) {
        Box(modifier = Modifier.fillMaxWidth().size(1.dp).background(Ink.line))
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Space.s4),
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

/**
 * The site's URL words are transliterated rather than translated into Arabic
 * script, so a link survives being pasted into WhatsApp. The app has to use
 * the same ones or it links to a page that is not there.
 */
private fun sitePath(page: String, lang: Lang): String {
    val slug = when (page) {
        "how" -> mapOf(Lang.FR to "comment-ca-marche", Lang.AR to "kayf-yaamal", Lang.EN to "how-it-works")
        "about" -> mapOf(Lang.FR to "a-propos", Lang.AR to "man-nahnu", Lang.EN to "about")
        else -> mapOf(Lang.FR to "confidentialite", Lang.AR to "khususiya", Lang.EN to "privacy")
    }
    return "/${lang.tag}/${slug[lang]}"
}
