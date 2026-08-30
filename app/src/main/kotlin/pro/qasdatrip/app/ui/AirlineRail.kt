package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.AirlineOption
import pro.qasdatrip.core.Money

/**
 * The carriers on this route, with what each one costs from.
 *
 * This is the filter people actually reach for. "Which airlines fly this, and
 * what does the cheap one want?" is the first question after a search, and
 * until now the only way to answer it was to scroll the whole list and read
 * the cards. A row of logos answers it in one glance and doubles as the
 * narrowing, which is why it sits above the count rather than inside the
 * filters page — a filter you have to open a page to find is a filter that
 * gets used once.
 *
 * Selection is a set, not a single choice: "Air Algérie or Tassili" is a real
 * question and a radio button cannot ask it. Tapping a lit card unlights it,
 * and unlighting the last one is the same as choosing all of them, which is
 * why there is no way to reach an empty list from here.
 */
@Composable
fun AirlineRail(
    airlines: List<AirlineOption>,
    selected: Set<String>,
    onSelect: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One carrier is not a comparison. The rail would be a single card that
    // filters nothing, taking 120dp off the first result for no answer.
    if (airlines.size < 2) return

    val words = LocalWords.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = words.airlinesOnRoute.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Ink.muted,
            modifier = Modifier.padding(horizontal = Space.s4),
        )
        Spacer(modifier = Modifier.height(Space.s2))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            items(airlines, key = { it.code }) { option ->
                AirlineCard(
                    option = option,
                    on = option.code in selected,
                    onClick = {
                        onSelect(
                            selected.toMutableSet().apply {
                                if (!add(option.code)) remove(option.code)
                            },
                        )
                    },
                )
            }
        }
    }
}

/**
 * A logo and a number. That is the whole card.
 *
 * It carried the airline's name as well, which made it 108dp wide and 126dp
 * tall — two full lines of text for "Turkish Airlines" — and three of them
 * ate the top third of the results before a single flight was visible. The
 * name was never doing much work: the logo is the name, which is what a
 * logo is for, and anybody who cannot place a mark can read it on the cards
 * below.
 *
 * The price loses its "dès" for the same reason. A column of prices under a
 * row of logos is unambiguous without a preposition in front of every one,
 * and the section heading above already says what this row is.
 */
@Composable
private fun AirlineCard(option: AirlineOption, on: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (on) Ink.accentSoft else Ink.surface)
            .border(
                width = if (on) 2.dp else 1.dp,
                color = if (on) Ink.accentUi else Ink.line,
                shape = RoundedCornerShape(Radius.md),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AirlineLogo(code = option.code, size = 30.dp)
        Text(
            // No price at all is possible: a carrier can be on the route with
            // every site still thinking. A dash is the shortest honest way to
            // hold the space until one answers.
            text = option.from?.let { Money.amount(it) } ?: "—",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (on) Ink.accentDeep else Ink.inkSoft,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
