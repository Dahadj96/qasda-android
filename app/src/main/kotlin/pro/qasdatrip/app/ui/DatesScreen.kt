package pro.qasdatrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import java.util.Locale

/**
 * Step three: when.
 *
 * This screen is the one the app was missing outright. Dates were picked in
 * Material's `DatePickerDialog`, which is a fine control and the wrong one
 * here: it is a floating sheet with its own typography and its own idea of a
 * header, it writes its month name in the phone's language rather than the
 * app's, and — the part that showed on your phone — it gives a round trip a
 * range picker whose two ends are impossible to read at a glance.
 *
 * So it is a page, like every other step. One month at a time, moved with
 * two circles that are big enough to hit. A one-way takes one tap. A round
 * trip takes two, and the days between them tint so the trip is visible as a
 * shape rather than as two numbers in a summary line.
 *
 * There are no prices on this grid, and that is deliberate. The price
 * calendar exists and is worth its wait — but it takes the server the better
 * part of a minute to build, and a picker that hangs before it will let you
 * choose a day is worse than a picker with no prices. Prices belong to the
 * screen that is about prices, reachable from the results in one tap.
 */
@Composable
fun DatesScreen(
    depart: String?,
    back: String?,
    roundTrip: Boolean,
    routeSubtitle: String?,
    onPick: (depart: String?, back: String?) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val words = LocalWords.current
    val lang = LocalLang.current
    val locale = localeOf(lang)

    val departDay = remember(depart) { depart?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
    val backDay = remember(back) { back?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
    var month by remember { mutableStateOf(YearMonth.from(departDay ?: today)) }

    val done = depart != null && (!roundTrip || back != null)

    Column(modifier = Modifier.fillMaxSize().background(Ink.canvas)) {
        QasdaAppBar(title = words.whenQuestion, onBack = onBack)
        Column(
            modifier = Modifier.padding(start = Space.s4, end = Space.s4, bottom = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            StepperBar(step = 3)
            routeSubtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink.muted)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A month before the current one has nothing bookable in
                    // it, so the control is simply not offered there.
                    MonthStep(
                        forward = false,
                        enabled = month > YearMonth.from(today),
                        onClick = { month = month.minusMonths(1) },
                    )
                    Text(
                        text = month.atDay(1)
                            .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                            .replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    MonthStep(
                        forward = true,
                        enabled = month < YearMonth.from(today).plusMonths(MonthsAhead),
                        onClick = { month = month.plusMonths(1) },
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    WeekDays.forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            items(weeksOf(month), key = { it.first().toString() }) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            inMonth = YearMonth.from(day) == month,
                            past = day < today,
                            depart = departDay,
                            back = backDay,
                            roundTrip = roundTrip,
                            modifier = Modifier.weight(1f),
                            onPick = { picked -> onPick.pick(picked, departDay, backDay, roundTrip) },
                        )
                    }
                }
            }

            // The one line worth saying here, and only when it is true: a
            // round trip with a departure and no return yet. The price
            // calendar's disclaimer used to sit here, which was a sentence
            // about prices on a grid that has none.
            if (roundTrip && departDay != null && backDay == null) {
                item {
                    Text(
                        text = words.pickReturn,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink.accentDeep,
                        modifier = Modifier.padding(vertical = Space.s2),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink.surface)
                .padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Button(
                onClick = onConfirm,
                enabled = done,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.ink,
                    contentColor = Ink.inverse,
                    disabledContainerColor = Ink.surfaceSoft,
                    disabledContentColor = Ink.muted,
                ),
            ) {
                Text(
                    text = if (done) {
                        val chosen = listOfNotNull(
                            depart?.let { formatDate(it, lang) },
                            back?.takeIf { roundTrip }?.let { formatDate(it, lang) },
                        ).joinToString(" – ")
                        "${words.continueLabel} · $chosen"
                    } else words.chooseDates,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Twelve months is as far ahead as any of the four sites will quote. */
private const val MonthsAhead = 11L

private val WeekDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

private fun localeOf(lang: Lang): Locale = when (lang) {
    Lang.FR -> Locale.FRENCH
    Lang.AR -> Locale.forLanguageTag("ar")
    Lang.EN -> Locale.ENGLISH
}

/**
 * The month as whole weeks, Monday first, padded at both ends with the days
 * of the neighbouring months so every row has seven cells and the columns
 * line up under their letters.
 */
private fun weeksOf(month: YearMonth): List<List<LocalDate>> {
    val first = month.atDay(1)
    val start = first.minusDays(((first.dayOfWeek.value + 6) % 7).toLong())
    val last = month.atEndOfMonth()
    val end = last.plusDays((7 - last.dayOfWeek.value).toLong())
    val weeks = mutableListOf<List<LocalDate>>()
    var cursor = start
    while (!cursor.isAfter(end)) {
        weeks += (0..6).map { cursor.plusDays(it.toLong()) }
        cursor = cursor.plusDays(7)
    }
    return weeks
}

/**
 * What a tap means depends on what is already chosen.
 *
 * One way: the day you tapped, every time. Round trip: the first tap sets
 * the departure and clears any return, the second sets the return — unless
 * it lands before the departure, in which case the honest reading is that
 * you have changed your mind about leaving, not that you want to come back
 * before you go.
 */
private fun ((String?, String?) -> Unit).pick(
    picked: LocalDate,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
) {
    val iso = picked.toString()
    if (!roundTrip) { this(iso, null); return }
    when {
        depart == null || back != null -> this(iso, null)
        picked.isBefore(depart) -> this(iso, null)
        picked == depart -> this(iso, null)
        else -> this(depart.toString(), iso)
    }
}

@Composable
private fun MonthStep(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Ink.surface)
            .border(1.dp, if (enabled) Ink.line else Ink.surfaceSoft, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = if (enabled) Ink.ink else Ink.line,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * One day.
 *
 * Four states have to be told apart at a glance: the day you leave, the day
 * you come back, the days in between, and the days you cannot have. The ends
 * are solid ink, the middle is the accent tint, and a day in the past is
 * simply faint — never struck through, which reads as "cancelled" rather
 * than "gone".
 */
@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    past: Boolean,
    depart: LocalDate?,
    back: LocalDate?,
    roundTrip: Boolean,
    modifier: Modifier = Modifier,
    onPick: (LocalDate) -> Unit,
) {
    val isEnd = day == depart || (roundTrip && day == back)
    val between = roundTrip && depart != null && back != null && day.isAfter(depart) && day.isBefore(back)
    val selectable = inMonth && !past

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(
                when {
                    isEnd -> Ink.ink
                    between -> Ink.accentSoft
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
            )
            .then(if (selectable) Modifier.clickable { onPick(day) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Money.isolate(day.dayOfMonth.toString()),
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                isEnd -> Ink.inverse
                !inMonth -> Ink.line
                past -> Ink.line
                between -> Ink.accentDeep
                else -> Ink.ink
            },
        )
    }
}
