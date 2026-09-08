package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space

/**
 * The two filters people set before they have seen a price.
 *
 * They appear on the search form and again on the edit sheet, and they are
 * the same two switches as the Direct button and the bag toggle on the
 * results - one question, asked wherever the person happens to be, never a
 * second filter system that has to be reconciled with the first.
 */
@Composable
fun QuickFilters(
    directOnly: Boolean,
    bagOnly: Boolean,
    onDirectOnly: (Boolean) -> Unit,
    onBagOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalWords.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        QuickChip(
            label = words.direct,
            icon = R.drawable.ic_plane_right,
            on = directOnly,
            onToggle = onDirectOnly,
            modifier = Modifier.weight(1f),
        )
        QuickChip(
            label = words.checkedBag,
            icon = R.drawable.ic_bag_checked,
            on = bagOnly,
            onToggle = onBagOnly,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickChip(
    label: String,
    @androidx.annotation.DrawableRes icon: Int,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (on) Ink.accentSoft else Ink.canvas)
            .border(1.dp, if (on) Ink.accentUi else Ink.line, RoundedCornerShape(Radius.md))
            .clickable(role = Role.Switch) {
                haptics.play(if (on) Feedback.ToggleOff else Feedback.ToggleOn)
                onToggle(!on)
            }
            .padding(horizontal = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (on) Ink.accentDeep else Ink.inkSoft,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (on) Ink.accentDeep else Ink.ink,
            maxLines = 1,
        )
    }
}
