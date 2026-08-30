package pro.qasdatrip.app.ui

import pro.qasdatrip.core.Lang
import pro.qasdatrip.core.Money
import pro.qasdatrip.core.Words
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

/** "AOÛT 2026" — the month a rail of day tiles sits in, written once. */
fun monthLabel(iso: String, lang: Lang): String {
    val date = runCatching { LocalDate.parse(iso, ISO) }.getOrNull() ?: return ""
    return Money.isolate(date.format(DateTimeFormatter.ofPattern("MMMM yyyy", localeOf(lang))))
}

/** "lun", "mar" — three letters over a day tile. */
fun weekdayLabel(iso: String, lang: Lang): String {
    val date = runCatching { LocalDate.parse(iso, ISO) }.getOrNull() ?: return ""
    return date.format(DateTimeFormatter.ofPattern("EEE", localeOf(lang))).trimEnd('.')
}

/** Just the number, in the digits the language uses. */
fun dayNumber(iso: String, lang: Lang): String {
    val date = runCatching { LocalDate.parse(iso, ISO) }.getOrNull() ?: return ""
    return Money.isolate(date.format(DateTimeFormatter.ofPattern("d", localeOf(lang))))
}

private fun localeOf(lang: Lang): Locale = when (lang) {
    Lang.FR -> Locale.FRENCH
    Lang.AR -> Locale.forLanguageTag("ar")
    Lang.EN -> Locale.ENGLISH
}

/**
 * "il y a 2 h", "hier", "il y a 3 sem".
 *
 * Notifications are read by how recent they are, not by their timestamp: the
 * question is "is this still true?" and a clock time from three days ago
 * answers it badly. Anything the server sends that cannot be parsed simply
 * shows nothing, because a wrong age on a price alert is worse than no age.
 */
fun relativeTime(iso: String?, words: Words, now: Long = System.currentTimeMillis()): String? {
    val at = parseInstant(iso) ?: return null
    val minutes = ((now - at) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 60 -> words.timeNow
        minutes < 60 * 24 -> words.timeHours.replace("{n}", Money.isolate((minutes / 60).toString()))
        minutes < 60 * 48 -> words.timeYesterday
        minutes < 60 * 24 * 14 -> words.timeDays.replace("{n}", Money.isolate((minutes / (60 * 24)).toString()))
        else -> words.timeWeeks.replace("{n}", Money.isolate((minutes / (60 * 24 * 7)).toString()))
    }
}

/** Epoch millis from whatever shape the server sent, or null. */
fun parseInstant(iso: String?): Long? {
    val text = iso?.trim().orEmpty()
    if (text.isEmpty()) return null
    runCatching { return java.time.Instant.parse(text).toEpochMilli() }
    runCatching {
        return java.time.LocalDateTime.parse(text.replace(' ', 'T'))
            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    }
    runCatching {
        return LocalDate.parse(text, ISO).atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli()
    }
    return null
}
