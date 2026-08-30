package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Filters
import pro.qasdatrip.core.Flight
import pro.qasdatrip.core.FlightList
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SiteOption
import pro.qasdatrip.core.Sites
import pro.qasdatrip.core.SortBy
import pro.qasdatrip.core.TimeBand
import kotlin.math.roundToInt

/**
 * The filters, as a page.
 *
 * This was a bottom sheet. A sheet is the wrong container for it: it opens
 * over the results at about two thirds of the screen, so the last section is
 * always below the fold, the sticky button competes with the phone's own
 * gesture bar, and there is no back arrow — you dismiss it by dragging, which
 * is a gesture people do not find and cannot see. Every other multi-step
 * choice in this app is a page with a bar on top; this is the same kind of
 * choice and it is now the same kind of screen.
 *
 * Nothing here goes back to the server. The search is a stream that took time
 * and four sites to produce, and re-running it to ask for morning flights
 * would throw that away to answer a question the list already answers.
 *
 * The count on the button is the honest one: it is computed against the same
 * rules the list will apply, so a filter set that would empty the screen says
 * so before it is applied rather than after.
 */
@Composable
fun FiltersScreen(
    current: Filters,
    flights: List<Flight>,
    sort: SortBy,
    onApply: (Filters) -> Unit,
    onBack: () -> Unit,
) {
    val words = LocalWords.current
    val lang = LocalLang.current

    var draft by remember { mutableStateOf(current) }

    val priceCeiling = flights.mapNotNull { it.cheapest?.second }.maxOrNull()
    val sites = remember(flights) { FlightList.sitesOn(flights) { Sites.name(it) } }
    val matches = FlightList.apply(flights, draft, sort).size

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(
            title = words.filters,
            onBack = onBack,
            // Only offered when there is something to clear. A permanently
            // lit "clear" on an untouched page is a button that does nothing
            // and teaches people the page is unresponsive.
            actionLabel = words.clearAll.takeIf { !draft.isEmpty },
            onAction = { draft = Filters() },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(
                start = Space.s4, end = Space.s4, bottom = Space.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.s5),
        ) {
            // "At most this many stops", so each chip is exclusive of the
            // others: two of them at once would be a range with a floor, and
            // nobody wants flights with at least one stop.
            item {
                Section(words.stopsLabel) {
                    Choice(words.direct, draft.maxStops == 0) {
                        draft = draft.copy(maxStops = if (draft.maxStops == 0) null else 0)
                    }
                    Choice(words.stopsOne, draft.maxStops == 1) {
                        draft = draft.copy(maxStops = if (draft.maxStops == 1) null else 1)
                    }
                    Choice(words.stopsTwoPlus, draft.maxStops == 2) {
                        draft = draft.copy(maxStops = if (draft.maxStops == 2) null else 2)
                    }
                }
            }

            item {
                Section(words.departureTime) {
                    listOf(
                        Triple(TimeBand.MORNING, words.morning, R.drawable.ic_sunrise),
                        Triple(TimeBand.AFTERNOON, words.afternoon, R.drawable.ic_sun),
                        Triple(TimeBand.EVENING, words.evening, R.drawable.ic_moon),
                        Triple(TimeBand.NIGHT, words.night, R.drawable.ic_moon),
                    ).forEach { (band, label, icon) ->
                        Choice(label, band in draft.departBands, icon) {
                            draft = draft.copy(
                                departBands = draft.departBands.toMutableSet()
                                    .apply { if (!add(band)) remove(band) },
                            )
                        }
                    }
                }
            }

            // One switch, not the design's pair of chips. "Cabin bag only" as
            // a second chip has to mean something, and the only thing it
            // could mean is "show me fares that do NOT include a bag", which
            // nobody wants. A switch also says plainly that this is on or off
            // rather than one of two states you must pick between.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    Label(words.baggage)
                    SwitchCard(
                        title = words.bagHoldIncluded,
                        subtitle = words.bagHoldSub,
                        on = draft.bagOnly,
                        onToggle = { draft = draft.copy(bagOnly = it) },
                    )
                }
            }

            // Which sites to trust with this trip.
            //
            // Everybody has one they will not book on — a card that was
            // refused once, a refund that took two months — and until now the
            // only way to act on that was to read every card's site name and
            // skip past it. Unticking every box is the same as ticking all of
            // them, because a screen filtered down to nothing is never what
            // somebody meant.
            if (sites.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                        Label(words.bookingSites)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.md))
                                .background(Ink.surface)
                                .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
                        ) {
                            sites.forEachIndexed { index, site ->
                                SiteRow(
                                    site = site,
                                    // Empty means "all", so an untouched page
                                    // shows every box ticked rather than none.
                                    on = draft.sites.isEmpty() || site.key in draft.sites,
                                    divider = index > 0,
                                    onToggle = {
                                        val all = sites.map { it.key }.toSet()
                                        val now = draft.sites.ifEmpty { all }
                                        val next = now.toMutableSet()
                                            .apply { if (!add(site.key)) remove(site.key) }
                                        draft = draft.copy(
                                            sites = if (next.isEmpty() || next == all) emptySet() else next,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (priceCeiling != null && priceCeiling > 0) {
                item {
                    val chosen = draft.maxPrice ?: priceCeiling
                    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Label(words.maxPrice)
                            Text(
                                Money.format(chosen, lang),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Slider(
                            value = chosen.toFloat(),
                            onValueChange = { picked ->
                                // Snapping back to null at the top matters:
                                // "no ceiling" and "a ceiling that happens to
                                // be the most expensive flight" behave the
                                // same today and stop behaving the same the
                                // moment a dearer flight arrives from a
                                // slower site.
                                val rounded = (picked / STEP).roundToInt() * STEP
                                draft = draft.copy(
                                    maxPrice = rounded.toDouble().takeIf { it < priceCeiling },
                                )
                            },
                            valueRange = 0f..priceCeiling.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Ink.accentUi,
                                activeTrackColor = Ink.accentUi,
                                inactiveTrackColor = Ink.surfaceSoft,
                            ),
                        )
                    }
                }
            }
        }

        // The button is pinned rather than scrolled to, because on a page
        // this tall the answer to "how many does that leave?" has to stay
        // visible while you are still changing your mind.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink.canvas)
                .padding(start = Space.s4, end = Space.s4, top = Space.s3, bottom = Space.s4),
        ) {
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = matches > 0,
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                Text(
                    if (matches > 0) {
                        words.showOffers.replace("{n}", Money.isolate(matches.toString()))
                    } else {
                        words.noMatchTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private const val STEP = 1_000

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Ink.muted,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        Label(title)
        // Four bands do not fit on one line of a 360dp phone in every
        // language, and "Après-midi" is the one that proves it.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) { content() }
    }
}

/**
 * A chip that fills solid when chosen.
 *
 * The tint-and-outline treatment used elsewhere is for things you are
 * browsing; these are answers you have given, and on a page of nothing but
 * chips the tinted version left too little between "picked" and "not". Ink on
 * white is unambiguous at arm's length, which is where phones are held.
 */
@Composable
private fun Choice(
    label: String,
    on: Boolean,
    @androidx.annotation.DrawableRes icon: Int? = null,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (on) Ink.ink else Ink.surface)
            .border(
                1.dp,
                if (on) Ink.ink else Ink.lineStrong,
                RoundedCornerShape(Radius.pill),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = if (on) Ink.inverse else Ink.ink,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (on) Ink.inverse else Ink.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun SwitchCard(
    title: String,
    subtitle: String,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.md))
            .clickable { onToggle(!on) }
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink.muted)
        }
        Switch(
            checked = on,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Ink.accentUi,
                checkedBorderColor = Ink.accentUi,
                uncheckedThumbColor = Ink.surface,
                uncheckedTrackColor = Ink.surfaceSoft,
                uncheckedBorderColor = Ink.lineStrong,
            ),
        )
    }
}

@Composable
private fun SiteRow(site: SiteOption, on: Boolean, divider: Boolean, onToggle: () -> Unit) {
    Column {
        if (divider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ink.line)
                    .padding(horizontal = Space.s4),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = Space.s4, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            // A square with a tick rather than Material's checkbox: the stock
            // control brings its own 48dp touch padding and ripple, which on
            // a row that is already tappable draws a second target inside the
            // first one.
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (on) Ink.accentUi else Ink.surface)
                    .border(
                        1.5.dp,
                        if (on) Ink.accentUi else Ink.lineStrong,
                        RoundedCornerShape(7.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (on) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Text(
                site.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(0.dp))
        }
    }
}
