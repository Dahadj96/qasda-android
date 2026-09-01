package pro.qasdatrip.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Help
import pro.qasdatrip.core.HelpEntry

/**
 * The FAQ, in the words the site already uses and without a network.
 *
 * These are the questions somebody asks when a booking has gone sideways -
 * which is often exactly when they have no signal - so the answers ship with
 * the app rather than living behind a fetch.
 */
@Composable
fun HelpScreen(onContact: () -> Unit) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val groups = remember(lang) { Help.groups(lang) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        item {
            QasdaAppBar(
                    inset = false,
                title = words.navHelp,
                large = true,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        groups.forEach { group ->
            item {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                    modifier = Modifier.padding(top = Space.s2),
                )
            }
            items(group.entries.size) { index -> Entry(group.entries[index]) }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Ink.accentSoft)
                    .clickable(onClick = onContact)
                    .padding(Space.s4),
                verticalArrangement = Arrangement.spacedBy(Space.s1),
            ) {
                Text(words.contactUs, style = MaterialTheme.typography.titleMedium, color = Ink.accentDeep)
                Text(words.openOnSite, style = MaterialTheme.typography.bodyMedium, color = Ink.inkSoft)
            }
        }
    }
}

@Composable
private fun Entry(entry: HelpEntry) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable { open = !open }
            .padding(Space.s4)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                entry.question,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Ink.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        if (open) {
            Text(entry.answer, style = MaterialTheme.typography.bodyLarge, color = Ink.inkSoft)
        }
    }
}
