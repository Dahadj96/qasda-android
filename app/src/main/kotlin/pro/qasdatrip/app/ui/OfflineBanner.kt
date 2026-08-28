package pro.qasdatrip.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Space

/**
 * Whether the phone has a network worth trying. True by default: a screen
 * rendered before the first reading arrives should not accuse somebody of
 * being offline.
 */
val LocalOnline: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

/**
 * A line that says the connection is gone, and does not take the search
 * button away.
 *
 * The reading can be wrong - a network is briefly "not validated" while it is
 * coming up, and a captive portal lies in the other direction - so this
 * informs rather than blocks. Somebody who taps Search on a network we
 * thought was dead gets the search they asked for; the honest failure that
 * follows is a better answer than a button that refused to try.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val words = LocalWords.current
    AnimatedVisibility(visible = !LocalOnline.current) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius))
                .background(Ink.alertSoft)
                .padding(horizontal = Space.s3, vertical = Space.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Ink.alert),
            )
            Text(
                words.offline,
                style = MaterialTheme.typography.labelLarge,
                color = Ink.alert,
            )
        }
    }
}

private val Radius = 10.dp

/** Convenience for the one place that provides it. */
@Composable
fun ProvideOnline(online: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOnline provides online, content = content)
}
