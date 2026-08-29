package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Cabin
import pro.qasdatrip.core.Money

/**
 * Who is travelling, and in which cabin.
 *
 * SearchQuery has carried children, infants and a cabin class since the first
 * commit; the screen only ever let somebody set the number of adults. A family
 * of four searching here got quoted one adult fare, which is not the price
 * they would pay.
 *
 * The limits are the airlines' own, and they are the reason this is a screen
 * rather than four free fields:
 *
 *  - nine passengers is where booking engines stop taking a single booking
 *  - an infant travels on somebody's lap, so there cannot be more infants
 *    than adults
 *  - a child cannot fly alone on a normal ticket, so there is always at least
 *    one adult
 */
@Composable
fun TravellersPicker(
    adults: Int,
    children: Int,
    infants: Int,
    cabin: Cabin,
    onApply: (adults: Int, children: Int, infants: Int, cabin: Cabin) -> Unit,
    // Off when this is a step of the search flow: the app bar above it
    // already asks the question, and two headings stacked read as two
    // screens that failed to merge.
    showTitle: Boolean = true,
) {
    val words = LocalWords.current

    var a by remember { mutableStateOf(adults) }
    var c by remember { mutableStateOf(children) }
    var i by remember { mutableStateOf(infants) }
    var klass by remember { mutableStateOf(cabin) }

    val total = a + c + i
    val room = MAX_TRAVELLERS - total

    Column(
        modifier = Modifier.fillMaxSize().background(Ink.canvas).padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        if (showTitle) {
            Text(words.travellers, style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.md))
                .background(Ink.surface)
                .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
                .padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            CountRow(
                label = words.adults,
                note = words.adultsAge,
                value = a,
                // Never below one: a child cannot fly on their own ticket.
                canRemove = a > 1 && a > i,
                canAdd = room > 0,
                onChange = { a = it },
            )
            CountRow(
                label = words.children,
                note = words.childrenAge,
                value = c,
                canRemove = c > 0,
                canAdd = room > 0,
                onChange = { c = it },
            )
            CountRow(
                label = words.infants,
                note = words.infantsAge,
                value = i,
                canRemove = i > 0,
                // One lap each.
                canAdd = room > 0 && i < a,
                onChange = { i = it },
            )
        }

        Text(words.cabinClass, style = MaterialTheme.typography.labelSmall, color = Ink.muted)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.md))
                .background(Ink.surface)
                .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
        ) {
            listOf(
                Cabin.ECONOMY to words.economy,
                Cabin.PREMIUM to words.premium,
                Cabin.BUSINESS to words.business,
                Cabin.FIRST to words.first,
            ).forEach { (option, label) ->
                CabinRow(label = label, selected = option == klass) { klass = option }
            }
        }

        Box(modifier = Modifier.weight(1f))

        Button(
            onClick = { onApply(a, c, i, klass) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.sm),
            colors = ButtonDefaults.buttonColors(containerColor = Ink.ink, contentColor = Ink.inverse),
        ) { Text(words.apply, fontWeight = FontWeight.SemiBold) }
    }
}

private const val MAX_TRAVELLERS = 9

@Composable
private fun CountRow(
    label: String,
    note: String,
    value: Int,
    canRemove: Boolean,
    canAdd: Boolean,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(Money.isolate(note), style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
        }
        StepButton(R.drawable.ic_minus, enabled = canRemove) { onChange(value - 1) }
        Text(
            Money.isolate(value.toString()),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        StepButton(R.drawable.ic_plus, enabled = canAdd) { onChange(value + 1) }
    }
}

/**
 * A step that is not allowed is shown greyed rather than hidden: the reason a
 * fourth infant cannot be added is easier to work out when the button is
 * still where it was.
 */
@Composable
private fun StepButton(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surfaceSoft)
            .border(1.dp, if (enabled) Ink.lineStrong else Ink.line, RoundedCornerShape(Radius.pill))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) Ink.ink else Ink.lineStrong,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CabinRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Ink.accentSoft else Ink.surface)
            .clickable(onClick = onSelect)
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
