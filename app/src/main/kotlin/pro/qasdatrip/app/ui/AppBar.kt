package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Space

/**
 * The bar every screen that is not a tab root wears.
 *
 * It exists because the app shipped without one. Several pages were reachable
 * with no way back except the system gesture, which on a phone with gesture
 * navigation turned off is no way back at all — a dead end inside a product
 * whose whole job is letting somebody change their mind about a date.
 *
 * The control is a circle rather than a bare glyph, and 40dp rather than 24,
 * because it is the most-pressed thing on the screen and a bare arrow on a
 * pale ground is both hard to see and hard to hit. `AutoMirrored` turns it
 * around in Arabic without a second asset.
 */
@Composable
fun QasdaAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    large: Boolean = false,
    /**
     * False where the bar scrolls with the list it sits in.
     *
     * A bar inside a LazyColumn cannot hold the status bar inset for the
     * screen: it scrolls away, and takes the protection with it, leaving the
     * rows underneath to slide under the clock. On those screens the list
     * itself is padded and the bar must not pad again.
     */
    inset: Boolean = true,
) {
    val words = LocalWords.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The status bar inset lives here rather than on the Scaffold, so
            // that the one screen without a bar - home, whose photograph runs
            // to the top of the glass - can put its own content under the
            // clock instead of being pushed below it.
            .then(if (inset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        onBack?.let {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Ink.surface)
                    .border(1.dp, Ink.line, CircleShape)
                    .clickable(onClick = it)
                    .semantics { contentDescription = words.cancel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Ink.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (large) MaterialTheme.typography.displaySmall
                else MaterialTheme.typography.titleLarge,
                color = Ink.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Ink.accentDeep,
                maxLines = 1,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.s2, vertical = Space.s1),
            )
        }
    }
}
