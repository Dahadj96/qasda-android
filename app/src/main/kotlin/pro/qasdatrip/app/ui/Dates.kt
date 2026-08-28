package pro.qasdatrip.app.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import pro.qasdatrip.app.ui.theme.Ink
import pro.qasdatrip.app.ui.theme.LocalWords
import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dates, chosen from a calendar rather than typed.
 *
 * The search screen used to take YYYY-MM-DD in a text field. Every date this
 * app sends is a date somebody has to be at an airport for, and a typo in one
 * comes back as "no flights on this route" — a sentence about the route that
 * is really a sentence about the typing.
 *
 * Everything below deals in UTC on purpose. Material's pickers hand back the
 * UTC midnight of the day that was tapped, and the server wants a plain
 * calendar date, so converting through the device's zone would move the day
 * for anybody east or west of Greenwich.
 */

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().format(ISO)

fun String.isoToUtcMillis(): Long? = runCatching {
    LocalDate.parse(this, ISO).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

/**
 * A date the way the chosen language writes one — "28 août", "28 August",
 * "٢٨ أغسطس" — with the digits isolated so an Arabic sentence around them
 * cannot reorder them.
 */
fun formatDate(iso: String, lang: Lang): String {
    val date = runCatching { LocalDate.parse(iso, ISO) }.getOrNull() ?: return iso
    val locale = when (lang) {
        Lang.FR -> Locale.FRENCH
        Lang.AR -> Locale.forLanguageTag("ar")
        Lang.EN -> Locale.ENGLISH
    }
    return Money.isolate(date.format(DateTimeFormatter.ofPattern("d MMM", locale)))
}

/**
 * Yesterday is not a flight you can take.
 *
 * "Today" is the device's own day, converted to the UTC midnight the picker
 * compares against. Asking java.time for today *in UTC* instead would, at
 * 00:30 in Algiers, still be yesterday in Greenwich and would offer a day
 * that has already gone.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object FromTodayOnward : SelectableDates {
    private val firstDay: Long
        get() = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= firstDay

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
}

/**
 * The calendar. One date for a one-way, a range for a round trip — a range
 * cannot end before it starts, which is the return-before-departure mistake
 * handled by not being expressible.
 *
 * [onPick] gives back ISO dates, and null for a return that was not chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesDialog(
    roundTrip: Boolean,
    depart: String?,
    back: String?,
    onDismiss: () -> Unit,
    onPick: (depart: String?, back: String?) -> Unit,
) {
    val words = LocalWords.current

    val colors = DatePickerDefaults.colors(
        containerColor = Ink.surface,
        selectedDayContainerColor = Ink.ink,
        selectedDayContentColor = Ink.inverse,
        todayContentColor = Ink.accentDeep,
        todayDateBorderColor = Ink.accentUi,
        dayInSelectionRangeContainerColor = Ink.accentSoft,
        dayInSelectionRangeContentColor = Ink.ink,
    )

    if (roundTrip) {
        val state = androidx.compose.material3.rememberDateRangePickerState(
            initialSelectedStartDateMillis = depart?.isoToUtcMillis(),
            initialSelectedEndDateMillis = back?.isoToUtcMillis(),
            selectableDates = FromTodayOnward,
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            colors = colors,
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        onPick(
                            state.selectedStartDateMillis?.toIsoDate(),
                            state.selectedEndDateMillis?.toIsoDate(),
                        )
                    },
                ) { Text(words.confirm, color = Ink.accentDeep) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(words.cancel, color = Ink.muted) }
            },
        ) {
            DateRangePicker(state = state, colors = colors)
        }
    } else {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = depart?.isoToUtcMillis(),
            selectableDates = FromTodayOnward,
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            colors = colors,
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = { onPick(state.selectedDateMillis?.toIsoDate(), null) },
                ) { Text(words.confirm, color = Ink.accentDeep) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(words.cancel, color = Ink.muted) }
            },
        ) {
            DatePicker(state = state, colors = colors, title = null)
        }
    }
}
