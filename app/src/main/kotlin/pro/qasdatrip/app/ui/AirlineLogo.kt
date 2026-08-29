package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.core.Airlines

/**
 * The carrier's mark, in a circle.
 *
 * The card used to carry no logo, on the reasoning that a wordmark crushed
 * into a circle is decoration. Testing on a real phone said otherwise: a
 * list of a dozen rows that differ only in typography is read line by line,
 * and the same list with marks down the left is scanned. People know what
 * Air Algérie looks like before they have read the word.
 *
 * Three things make it safe to put back. The mark is loaded at 70px and
 * drawn at 36dp, so it costs one small request that Coil then caches for
 * the session. It is never stretched — a mark that fails to load leaves the
 * two-letter code on the soft surface, which is legible and honest, rather
 * than an empty circle or the wrong airline's badge. And the circle is
 * exactly the same size as the one in the loading skeleton, so the list does
 * not jump when the real answer replaces the placeholder.
 */
@Composable
fun AirlineLogo(
    code: String?,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val clean = code?.trim()?.uppercase().orEmpty()
    var failed by remember(clean) { mutableStateOf(clean.isEmpty()) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (failed) Ink.surfaceSoft else Ink.surface)
            .border(1.dp, Ink.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            // Two letters, never three: IATA codes are two characters and a
            // longer string here means the payload sent something that is
            // not a carrier code.
            Text(
                text = clean.take(2).ifEmpty { "··" },
                style = MaterialTheme.typography.labelMedium,
                color = Ink.muted,
                textAlign = TextAlign.Center,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Airlines.logoUrl(clean))
                    .crossfade(true)
                    .build(),
                contentDescription = Airlines.name(clean),
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier
                    .size(size)
                    .padding(size * 0.22f),
            )
        }
    }
}
