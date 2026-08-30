package pro.qasdatrip.app.ui

import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import pro.qasdatrip.app.ui.theme.LocalDark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.qasdatrip.app.R
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.SearchQuery
import pro.qasdatrip.core.routeArrow

/**
 * Home: the whole question on one card, and every answer a page of its own.
 *
 * The card is still here because it is the fastest path for somebody
 * repeating a trip they have taken before — Alger to Paris is already filled
 * in and two taps away from results. What changed is what a field does when
 * you touch it. It used to open a dialog stacked on this screen; now it
 * navigates, to a page with a question at the top, a step bar under it, and
 * room for the list or the calendar the question actually needs.
 */
@Composable
fun SearchScreen(
    draft: SearchDraft,
    recent: List<SearchQuery> = emptyList(),
    onRoundTrip: (Boolean) -> Unit,
    onSwap: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onPickDates: () -> Unit,
    onPickTravellers: () -> Unit,
    onSearch: () -> Unit,
    onRecent: (SearchQuery) -> Unit,
    onLanguage: () -> Unit = {},
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val haptics = LocalHaptics.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        // The photograph and the card are one unit, overlapping, so the card
        // reads as sitting on the view rather than under a banner. A Box
        // rather than a negative offset: an offset would leave the space it
        // came from empty at the bottom of the scroll.
        Box(modifier = Modifier.fillMaxWidth()) {
            Hero(lang = lang, onLanguage = onLanguage)
            SearchCard(
                draft = draft,
                onRoundTrip = onRoundTrip,
                onSwap = onSwap,
                onPickFrom = onPickFrom,
                onPickTo = onPickTo,
                onPickDates = onPickDates,
                onPickTravellers = onPickTravellers,
                modifier = Modifier.padding(
                    top = CARD_TOP,
                    start = Space.s4,
                    end = Space.s4,
                ),
            )
        }

        Column(modifier = Modifier.padding(horizontal = Space.s4)) {
            Spacer(modifier = Modifier.height(Space.s4))

            OfflineBanner(modifier = Modifier.padding(bottom = Space.s3))

            // Outside the card, as drawn. Inside it the button read as the
            // last field of a form; on its own it reads as the thing the
            // card is for.
            Button(
                // Felt at the tap, not at the answer: this is the moment the
                // person committed. Whether four sites have anything to say
                // is not known yet, and pretending otherwise here would be
                // feedback for an outcome that does not exist.
                onClick = { haptics.play(Feedback.Commit); onSearch() },
                enabled = draft.complete,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.solid,
                    contentColor = Ink.onSolid,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) { Text(words.search, style = MaterialTheme.typography.titleMedium) }

            if (recent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Space.s5))
                Text(
                    words.recentSearches.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink.muted,
                )
                Spacer(modifier = Modifier.height(Space.s2))
                // One card with hairlines, not three floating ones. Three
                // cards read as three offers; this is one list of things you
                // have already asked, and the shape should say so.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Ink.surface)
                        .border(1.dp, Ink.line, RoundedCornerShape(Radius.md)),
                ) {
                    recent.take(3).forEachIndexed { index, past ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Ink.line),
                            )
                        }
                        RecentRow(past, lang) { onRecent(past) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space.s6))
        }
    }
}

/** How far down the photograph the card starts, as drawn. */
private val HERO_HEIGHT = 300.dp
private val CARD_TOP = 176.dp

/**
 * The view from the window.
 *
 * A photograph rather than the flat green panel that was here, because the
 * panel said nothing: it was a colour with an icon on it, and every app has
 * one. This is the thing the product is actually about, and the aircraft in
 * it deliberately carries no livery — Qasda compares sites and does not sell
 * tickets, and a branded jet would say the opposite.
 *
 * Two photographs, one per theme: golden hour for the light app, the same
 * seat at night for the dark one.
 */
@Composable
private fun Hero(lang: Lang, onLanguage: () -> Unit) {
    val words = LocalWords.current
    val dark = LocalDark.current
    val canvas = Ink.canvas

    // The clock and the battery sit on the photograph, and the photograph is
    // dark at the top in both themes - so the glyphs have to be light even
    // when the rest of the app is the light one. Restored on the way out, so
    // every other screen keeps the appearance its own background asks for.
    val view = LocalView.current
    DisposableEffect(view, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = !dark }
    }

    Box(modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT)) {
        Image(
            painter = painterResource(if (dark) R.drawable.hero_night else R.drawable.hero_day),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // Dark at the top, where the name and the tagline are. White type on
        // an unveiled sky is legible until the day the photograph changes.
        Box(
            modifier = Modifier.matchParentSize().background(
                if (dark) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.45f to Color.Black.copy(alpha = 0.30f),
                        1f to Color.Black.copy(alpha = 0.14f),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.56f),
                        0.32f to Color.Black.copy(alpha = 0.34f),
                        0.62f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0f),
                    )
                },
            ),
        )
        // And dissolving into the page at the bottom, so the photograph ends
        // in the app rather than at a hard edge behind the card.
        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.44f to canvas.copy(alpha = 0f),
                    0.72f to canvas.copy(alpha = 0.55f),
                    0.90f to canvas,
                    1f to canvas,
                ),
            ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = Space.s5, end = Space.s5, top = Space.s6),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The product's name, not a slogan. This is the first screen
                // of an app somebody chose to open; they know what it is for,
                // and the headline's job is to say where they are.
                Text(
                    words.appName,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 34.sp,
                        lineHeight = 42.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                // Two letters, top right, as drawn.
                //
                // The language lived only inside Compte -> Réglages, which is
                // three taps from here and behind a word somebody who cannot
                // read the current language cannot read either. This app is
                // used in a country where the same person switches between
                // French and Arabic mid-sentence; the switch belongs where
                // they can see it.
                LanguagePill(lang, onLanguage, onPhoto = true)
            }
            Spacer(modifier = Modifier.height(Space.s1))
            Text(
                words.heroTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
}

/**
 * One card, not five loose boxes. The four fields are a single question, and
 * a card that holds them says so; separately they read as five unrelated
 * settings on a form.
 */
@Composable
private fun SearchCard(
    draft: SearchDraft,
    onRoundTrip: (Boolean) -> Unit,
    onSwap: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onPickDates: () -> Unit,
    onPickTravellers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(Radius.lg), clip = false)
            .clip(RoundedCornerShape(Radius.lg))
            .background(Ink.surface)
            .padding(Space.s4),
    ) {
        // Inside the card, not above it. One way against round trip is the
        // first thing the card asks, not a setting that applies to the card
        // from outside.
        TripToggle(roundTrip = draft.roundTrip, onChange = onRoundTrip)
        Spacer(modifier = Modifier.height(10.dp))

        // The swap control sits over the seam between the two fields, which
        // is where the journey turns around. Somebody going home after a
        // holiday is running the same search backwards, and making them
        // retype both ends of it is work we can do for them.
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(
                    label = words.from,
                    value = "${cityName(draft.from, lang)} (${draft.from})",
                    icon = R.drawable.ic_pin,
                    trailingSpace = true,
                    onClick = onPickFrom,
                )
                Field(
                    label = words.to,
                    value = "${cityName(draft.to, lang)} (${draft.to})",
                    icon = R.drawable.ic_pin,
                    trailingSpace = true,
                    onClick = onPickTo,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-10).dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Ink.surface)
                    .border(1.dp, Ink.lineStrong, CircleShape)
                    .clickable(onClick = onSwap)
                    .semantics { contentDescription = words.swapRoute },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_swap),
                    contentDescription = null,
                    tint = Ink.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dates and travellers share a row: both are short answers, and
        // giving each a full-width box pushes the button off the fold on a
        // small phone.
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            Field(
                label = words.dates,
                value = datesLabel(draft, lang, words.chooseDates),
                muted = draft.depart == null,
                modifier = Modifier.weight(1f),
                onClick = onPickDates,
            )
            Field(
                label = words.travellers,
                value = "${Money.isolate(draft.travellers.toString())} · ${cabinLabel(draft.cabin, words)}",
                modifier = Modifier.weight(1f),
                onClick = onPickTravellers,
            )
        }
    }
}

/**
 * A past search, as a shortcut. The price it found is deliberately not here:
 * it was true on the day, and a stale number beside a route reads as a quote.
 */
@Composable
private fun RecentRow(query: SearchQuery, lang: Lang, onPick: () -> Unit) {
    val words = LocalWords.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${cityName(query.from, lang)} ${routeArrow(lang)} ${cityName(query.to, lang)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOfNotNull(
                    formatDate(query.departDate, lang),
                    query.returnDate?.let { formatDate(it, lang) },
                    Money.isolate(query.travellers.toString()) + " · " + cabinLabel(query.cabin, words),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.muted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * A field inside the search card: its label above, its answer below, in a
 * box with an 8dp corner. Not a text input — every one of these opens a
 * page, because every one of them has a wrong answer that a keyboard would
 * happily accept.
 */
@Composable
private fun Field(
    label: String,
    value: String,
    muted: Boolean = false,
    trailingSpace: Boolean = false,
    @androidx.annotation.DrawableRes icon: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(Ink.surface)
            .border(1.dp, Ink.line, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(
                start = Space.s3,
                // Room for the swap circle that overlaps this edge, so a long
                // airport name never runs underneath it.
                end = if (trailingSpace) 52.dp else Space.s3,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = Ink.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.muted, maxLines = 1)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (muted) Ink.muted else Ink.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The current language, as two letters in a pill. Tapping opens the picker. */
@Composable
private fun LanguagePill(lang: Lang, onClick: () -> Unit, onPhoto: Boolean = false) {
    Text(
        text = lang.tag.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (onPhoto) Color.White else Ink.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (onPhoto) Color.White.copy(alpha = 0.16f) else Ink.surface)
            .border(
                1.dp,
                if (onPhoto) Color.White.copy(alpha = 0.35f) else Ink.lineStrong,
                RoundedCornerShape(Radius.pill),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * One way or return. Two words, and the difference between them decides
 * whether the calendar asks for one date or two.
 */
@Composable
private fun TripToggle(roundTrip: Boolean, onChange: (Boolean) -> Unit) {
    val words = LocalWords.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surfaceSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to words.oneWay, true to words.roundTrip).forEach { (isReturn, label) ->
            val on = isReturn == roundTrip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (on) Ink.surface else Color.Transparent)
                    .clickable { onChange(isReturn) }
                    // 7, not 10: with the track's own 4dp this makes the
                    // control 40dp tall, which is what it is in the file.
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (on) Ink.ink else Ink.muted,
                )
            }
        }
    }
}

/**
 * What the dates field says before and after somebody has chosen. A round
 * trip with no return yet still shows the departure, so the field reflects
 * the half-answer rather than pretending nothing was picked.
 */
private fun datesLabel(draft: SearchDraft, lang: Lang, placeholder: String): String = when {
    draft.depart == null -> placeholder
    draft.roundTrip && draft.back != null ->
        "${formatDate(draft.depart, lang)} – ${formatDate(draft.back, lang)}"
    else -> formatDate(draft.depart, lang)
}
