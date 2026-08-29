package pro.qasdatrip.app.ui

import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How a date is written, in the language the app is speaking.
 *
 * Material's `DatePickerDialog` used to live here too. It is gone, and the
 * reason is the same reason this file is now four lines long: the dialog
 * read the *phone's* locale rather than the app's, so an app set to French
 * on an Arabic phone put "أغسطس" above French weekday letters, and on a
 * round trip it gave a range picker whose two ends could not be told apart
 * at a glance. Dates are a page of the search flow now — see DatesScreen —
 * drawn from the same tokens as every other screen, and this file keeps only
 * the formatting the rest of the app shares.
 */

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

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

/** The same date with its weekday, for the one place that has room for it. */
fun formatDateLong(iso: String, lang: Lang): String {
    val date = runCatching { LocalDate.parse(iso, ISO) }.getOrNull() ?: return iso
    val locale = when (lang) {
        Lang.FR -> Locale.FRENCH
        Lang.AR -> Locale.forLanguageTag("ar")
        Lang.EN -> Locale.ENGLISH
    }
    return Money.isolate(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)))
}
