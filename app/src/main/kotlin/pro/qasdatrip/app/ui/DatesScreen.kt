package pro.qasdatrip.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalLang
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.app.ui.theme.Radius
import pro.qasdatrip.app.ui.theme.Space
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Step three: when.
 *
 * Rewritten after testers were put in front of it. Every complaint they had
 * was the same complaint underneath: the screen never said what it was
 * asking for.
 *
 * They opened a grid of numbers with no way of knowing whether one tap was
 * expected or two - the trip type had been chosen two screens earlier and
 * forgotten by the time the calendar appeared, and there was no way to
 * change it from here. So the choice now lives at the top of this screen,
 * beside two slots that show what has been picked so far and which one is
 * being filled in. Nothing about the question is off-screen.
 *
 * The month arrows are gone. Twelve months scroll continuously, which is
 * what every calendar built for a phone does, and it dissolves the other
 * complaint for free: there is no longer a row of next-month days that look
 * tappable and are not, because the next month is a thumb-flick below rather
 * than behind a button.
 *
 * The grid itself was too small and too flat to read. Days are 48dp targets
 * with 16sp numbers now - the accessibility minimum, not a guess - and every
 * state carries something besides colour: a filled circle for the two ends,
 * an outline ring for today, a continuous tinted bar for the nights between,
 * and no touch feedback at all on a day that cannot be chosen.
 *
 * There are still no prices on this grid, deliberately. The price calendar
 * takes the server the better part of a minute against four sites, and a
 * picker that hangs before it will let you choose a day is worse than a
 * picker with no prices.
 */
@Composable
fun DatesScreen(
    depart: String?,
    back: String?,
    roundTrip: Boolean,
    routeSubtitle: String?,
    onPick: (depart: String?, back: String?) -> Unit,
    onTripType: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val locale = localeOf(lang)
    val haptics = LocalHaptics.current

    val departDay = remember(depart) { depart?.parseDay() }
    val backDay = remember(back) { back?.parseDay() }

    // Which slot the next tap fills. Held rather than derived so that tapping
    // "Retour" can aim the next tap at the return without changing anything
    // else - the one gesture that lets somebody correct half of a range.
    var filling by rememberSaveable { mutableStateOf(Slot.DEPART) }
    val effective = when {
        !roundTrip -> Slot.DEPART
        departDay == null -> Slot.DEPART
        backDay == null -> Slot.RETURN
        else -> filling
    }

    val months = remember(today) { (0 until MonthsShown).map { YearMonth.from(today).plusMonths(it.toLong()) } }
    // Opened already in the right place rather than animated into it: a
    // picker that starts in January and flies to June looks broken.
    val startIndex = remember(departDay, months) {
        val target = YearMonth.from(departDay ?: today)
        months.indexOf(target).coerceAtLeast(0) * 2
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    val done = depart != null && (!roundTrip || back != null)

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(title = words.whenQuestion, onBack = onBack)

        Column(
            modifier = Modifier.padding(horizontal = Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            routeSubtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
            }

            // The question this screen is really asking, answerable here.
            TripTypeToggle(
                roundTrip = roundTrip,
                onChange = { wantsReturn ->
                    haptics.play(Feedback.Selection)
                    onTripType(wantsReturn)
                    // Switching to a return trip aims the next tap at it;
                    // switching away drops a return that no longer applies.
                    if (wantsReturn) filling = Slot.RETURN else onPick(depart, null)
                },
            )

            DateSlots(
                depart = departDay,
                back = backDay,
                roundTrip = roundTrip,
                filling = effective,
                lang = lang,
                onAim = { slot -> filling = slot },
            )
        }

        Spacer(modifier = Modifier.height(Space.s3))
        WeekdayHeader(locale = locale)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Space.s6),
        ) {
            months.forEach { month ->
                monthBlock(
                    month = month,
                    locale = locale,
                    today = today,
                    depart = departDay,
                    back = backDay,
                    roundTrip = roundTrip,
                    onPick = { day ->
                        haptics.play(Feedback.Selection)
                        val next = choose(day, departDay, backDay, roundTrip, effective)
                        filling = next.filling
                        onPick(next.depart?.toString(), next.back?.toString())
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink.surface)
                .padding(Space.s4),
        ) {
            Button(
                onClick = onConfirm,
                enabled = done,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.solid,
                    contentColor = Ink.onSolid,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                Text(
                    text = when {
                        done -> {
                            val chosen = listOfNotNull(
                                depart?.let { formatDate(it, lang) },
                                back?.takeIf { roundTrip }?.let { formatDate(it, lang) },
                            ).joinToString(" – ")
                            "${words.continueLabel} · $chosen"
                        }
                        effective == Slot.RETURN -> words.pickReturn
                        else -> words.chooseDates
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private enum class Slot { DEPART, RETURN }

/** Twelve months is as far ahead as any airline will sell. */
private const val MonthsShown = 12

private data class Chosen(val depart: LocalDate?, val back: LocalDate?, val filling: Slot)

/**
 * What a tap means.
 *
 * The rule androidx uses for its own range picker, with one deliberate
 * difference: a return on the day of departure is allowed. A hotel needs a
 * night; a flight does not, and somebody flying out and back the same day is
 * a real Algiers-Paris trip.
 *
 * A tap before the departure is never an error and never silently swaps the
 * two. It reads as "I have changed my mind about leaving", which is what
 * somebody who taps an earlier day almost always means.
 */
private fun choose(
    day: LocalDate,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    filling: Slot,
): Chosen {
    if (!roundTrip) return Chosen(day, null, Slot.DEPART)
    return when {
        filling == Slot.DEPART -> {
            // Keep a return that still makes sense; drop one that no longer does.
            val keep = back?.takeIf { !it.isBefore(day) }
            Chosen(day, keep, if (keep == null) Slot.RETURN else Slot.DEPART)
        }
        depart == null || day.isBefore(depart) -> Chosen(day, null, Slot.RETURN)
        else -> Chosen(depart, day, Slot.DEPART)
    }
}

/**
 * Aller-retour or aller simple, on the screen where it matters.
 *
 * It was on the home screen only, which is two pages and one forgotten
 * decision away from here.
 */
@Composable
private fun TripTypeToggle(roundTrip: Boolean, onChange: (Boolean) -> Unit) {
    val words = LocalWords.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.pill))
            .background(Ink.surfaceSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(true to words.roundTrip, false to words.oneWay).forEach { (wantsReturn, label) ->
            val on = wantsReturn == roundTrip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (on) Ink.surface else Color.Transparent)
                    .clickable { onChange(wantsReturn) }
                    .padding(vertical = 9.dp),
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
 * The two ends of the trip, always visible, and one of them lit.
 *
 * This is the answer to "am I picking one day or two". It is also the only
 * way to change your mind about one end without losing the other: tapping a
 * slot aims the next tap at it.
 */
@Composable
private fun DateSlots(
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    filling: Slot,
    lang: Lang,
    onAim: (Slot) -> Unit,
) {
    val words = LocalWords.current
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
        DateSlot(
            label = words.outbound,
            value = depart?.let { formatDate(it.toString(), lang) },
            placeholder = words.chooseDates,
            active = filling == Slot.DEPART,
            enabled = true,
            modifier = Modifier.weight(1f),
            onClick = { onAim(Slot.DEPART) },
        )
        DateSlot(
            label = words.inbound,
            value = if (roundTrip) back?.let { formatDate(it.toString(), lang) } else words.oneWay,
            placeholder = words.pickReturn,
            active = roundTrip && filling == Slot.RETURN,
            // Dimmed rather than hidden on a one-way: a slot that disappears
            // makes the row jump and takes the evidence of the mode with it.
            enabled = roundTrip,
            modifier = Modifier.weight(1f),
            onClick = { if (roundTrip) onAim(Slot.RETURN) },
        )
    }
}

@Composable
private fun DateSlot(
    label: String,
    value: String?,
    placeholder: String,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(if (active) Ink.accentSoft else Ink.surface)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) Ink.accentUi else Ink.line,
                shape = RoundedCornerShape(Radius.md),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.s3, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Ink.muted else Ink.lineStrong,
        )
        Text(
            value ?: placeholder,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> Ink.lineStrong
                value == null -> Ink.muted
                else -> Ink.ink
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun WeekdayHeader(locale: Locale) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s3)) {
            weekOrder(locale).forEach { day ->
                Text(
                    // SHORT, not NARROW: narrow gives "S" for both Saturday
                    // and Sunday, and a tester who cannot tell the two columns
                    // apart cannot read the grid under them at all.
                    text = day.getDisplayName(TextStyle.SHORT, locale).trimEnd('.'),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) Ink.lineStrong else Ink.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = Space.s2),
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.monthBlock(
    month: YearMonth,
    locale: Locale,
    today: LocalDate,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    stickyHeader(key = "h$month", contentType = "month-header") {
        Text(
            text = month.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                .replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleMedium,
            color = Ink.ink,
            modifier = Modifier
                .fillMaxWidth()
                // Opaque, or the numbers scroll visibly through it.
                .background(Ink.canvas)
                .padding(start = Space.s4, top = Space.s4, bottom = Space.s2),
        )
    }
    item(key = "m$month", contentType = "month-grid") {
        MonthGrid(month, locale, today, depart, back, roundTrip, onPick)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    locale: Locale,
    today: LocalDate,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    val rows = remember(month, locale) { weeksOf(month, locale) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.s3)) {
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        // A day of the neighbouring month is not drawn at all.
                        // Drawn and greyed, it looks like a target that
                        // refuses the tap, which is what testers read as a
                        // bug - and the next month is one flick away now.
                        Spacer(modifier = Modifier.weight(1f).height(CellHeight))
                    } else {
                        DayCell(
                            day = day,
                            today = today,
                            depart = depart,
                            back = back,
                            roundTrip = roundTrip,
                            modifier = Modifier.weight(1f),
                            onPick = onPick,
                        )
                    }
                }
            }
        }
    }
}

private val CellHeight = 52.dp
private val CircleSize = 42.dp

/**
 * One day.
 *
 * Six states, and none of them told apart by colour alone: the two ends are
 * a filled circle with a word under them, the nights between are a
 * continuous tinted bar, today wears an outline ring, and a day that cannot
 * be chosen has no touch feedback at all - the absence of a ripple is the
 * strongest "not this one" a touchscreen has.
 *
 * The bar is drawn as two half-width blocks rather than one background, so
 * consecutive days join into an unbroken run instead of showing a seam at
 * every cell edge. Earlier half and later half, never left and right: in
 * Arabic the Row lays its children out the other way and the halves follow
 * the trip rather than the screen.
 */
@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    modifier: Modifier = Modifier,
    onPick: (LocalDate) -> Unit,
) {
    val words = LocalWords.current
    val isDepart = day == depart
    val isReturn = roundTrip && day == back
    val isEnd = isDepart || isReturn
    val between = roundTrip && depart != null && back != null &&
        day.isAfter(depart) && day.isBefore(back)
    val past = day.isBefore(today)
    val selectable = !past

    val earlierHalf = (between || isReturn) && depart != null && back != null && day.isAfter(depart)
    val laterHalf = (between || isDepart) && depart != null && back != null && day.isBefore(back)

    Box(
        modifier = modifier
            .height(CellHeight)
            .then(if (selectable) Modifier.clickable { onPick(day) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // The run between the two ends.
        if (earlierHalf || laterHalf) {
            Row(modifier = Modifier.fillMaxWidth().height(CircleSize)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(if (earlierHalf) Ink.accentSoft else Color.Transparent),
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(if (laterHalf) Ink.accentSoft else Color.Transparent),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(CircleSize)
                .clip(CircleShape)
                .background(if (isEnd) Ink.solid else Color.Transparent)
                .then(
                    if (day == today && !isEnd) Modifier.border(1.dp, Ink.accentUi, CircleShape)
                    else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = Money.isolate(day.dayOfMonth.toString()),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = if (isEnd) FontWeight.Bold else FontWeight.Normal,
                ),
                color = when {
                    isEnd -> Ink.onSolid
                    past -> Ink.lineStrong
                    between -> Ink.accentDeep
                    day == today -> Ink.accentDeep
                    else -> Ink.ink
                },
            )
        }

        // Which end this is, in a word. Two filled circles otherwise leave
        // the reader to work out which one they leave on.
        if (isEnd) {
            Text(
                text = if (isDepart) words.outbound else words.inbound,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Ink.accentDeep,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The app is used in Algeria, so the week starts on Saturday and the weekend
 * is Friday and Saturday - whichever of the three languages the phone is
 * reading. Pinning the region rather than the language is what makes a
 * French-speaking Algerian see the same calendar as an Arabic-speaking one,
 * and it is also what gets the Maghrebi month names (janvier is جانفي here,
 * not يناير) instead of the Levantine set.
 */
private fun localeOf(lang: Lang): Locale = when (lang) {
    Lang.FR -> Locale.forLanguageTag("fr-DZ")
    Lang.AR -> Locale.forLanguageTag("ar-DZ")
    Lang.EN -> Locale.forLanguageTag("en-DZ")
}

private fun weekOrder(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0 until 7).map { first.plus(it.toLong()) }
}

/**
 * The month as whole weeks. A cell that belongs to another month is null and
 * is drawn as nothing at all - see DayCell's note.
 */
private fun weeksOf(month: YearMonth, locale: Locale): List<List<LocalDate?>> {
    val order = weekOrder(locale)
    val first = month.atDay(1)
    val lead = order.indexOf(first.dayOfWeek)
    val length = month.lengthOfMonth()
    val cells = buildList<LocalDate?> {
        repeat(lead) { add(null) }
        for (d in 1..length) add(month.atDay(d))
        while (size % 7 != 0) add(null)
    }
    return cells.chunked(7)
}

private fun String.parseDay(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
