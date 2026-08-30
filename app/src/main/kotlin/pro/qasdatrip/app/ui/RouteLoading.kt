package pro.qasdatrip.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Money

/**
 * The wait, drawn as the journey.
 *
 * A search takes as long as the slowest booking site, which on a bad
 * connection is most of half a minute. A bar filling to an invented
 * percentage lies; a spinner says nothing. A plane crossing from the origin
 * to the destination says the one true thing available — this is still
 * going, and it is going somewhere — and it names the route while it does,
 * so the screen is still readable as an answer to the question that was
 * asked.
 *
 * The count underneath is offers found so far, not sites contacted. The
 * server deliberately does not report which providers answered, and a number
 * the app cannot know is a number it must not draw.
 */
@Composable
fun RouteLoading(
    origin: String,
    destination: String,
    offersSoFar: Int,
    modifier: Modifier = Modifier,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val rtl = lang.rtl

    val flight = rememberInfiniteTransition(label = "route")
    val progress by flight.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "planeProgress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            val span = maxWidth - PlaneSize
            // Nothing here compensates for Arabic, and that is the fix.
            //
            // This animation ran backwards in RTL because it corrected for a
            // mirroring Compose had already done. `Alignment.CenterStart` is
            // the right-hand edge in an RTL layout and `Modifier.offset(x=)`
            // moves in the reading direction, so the aircraft already leaves
            // the origin and flies toward the destination on its own. The
            // old `1f - progress` then reversed that, and the plane took off
            // from the destination and flew home.
            //
            // The one thing Compose does not mirror is the artwork: the
            // drawable is not marked autoMirrored, so the glyph still points
            // right while the travel goes left. That is what the scaleX below
            // is for, and it is the only RTL special case this file needs.

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Ink.line),
            )
            Box(
                modifier = Modifier
                    // CenterStart, in both directions: it is the origin end
                    // of the rail, and in Arabic that is already the right.
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Ink.accent),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Ink.ink),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Ink.lineStrong),
            )
            Icon(
                painter = painterResource(R.drawable.ic_plane_right),
                contentDescription = null,
                tint = Ink.accentDeep,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = span * progress.coerceIn(0f, 1f))
                    .size(PlaneSize)
                    .graphicsLayer { scaleX = if (rtl) -1f else 1f },
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = origin,
                style = MaterialTheme.typography.labelLarge,
                color = Ink.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = destination,
                style = MaterialTheme.typography.labelLarge,
                color = Ink.ink,
                textAlign = TextAlign.End,
            )
        }

        Text(
            text = words.comparing,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // Nothing to say until something has actually arrived. "0 offers" in
        // the first second reads as a result, not as a wait.
        if (offersSoFar > 0) {
            Text(
                text = words.offersSoFar.replace("{n}", Money.isolate(offersSoFar.toString())),
                style = MaterialTheme.typography.labelLarge,
                color = Ink.accentDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Space.s1),
            )
        }
    }
}

private val PlaneSize = 22.dp
