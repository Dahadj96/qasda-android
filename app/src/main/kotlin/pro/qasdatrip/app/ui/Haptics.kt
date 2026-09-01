package pro.qasdatrip.app.ui

import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * What the phone should feel like at a given moment, said in the product's
 * own words rather than the platform's.
 *
 * Semantic tokens, not constants: `Feedback.Commit` means "the person has
 * just committed to something", and each platform decides what that is. That
 * is the whole reason this indirection exists — the iOS half of this app is
 * coming, and Apple's vocabulary is a different shape (impact weights and
 * notification types, not a flat list of constants). A screen that asks for
 * `Commit` keeps working; a screen that asks for `CONFIRM` would not.
 */
enum class Feedback {
    /** A value changed among several: a tab, a date, a step on a slider. */
    Selection,

    /** The person committed: search pressed, a site opened. */
    Commit,

    /** An operation finished and went well. */
    Success,

    /** A real answer that is not the hoped-for one. No flights is not an error. */
    Warning,

    /** Something failed. */
    Error,

    ToggleOn,
    ToggleOff,
}

/** Something that can make the phone tick. */
fun interface Haptics {
    fun play(feedback: Feedback)
}

/**
 * Silence by default.
 *
 * A composable that asks for haptics outside a provider - a preview, a test -
 * gets nothing rather than a crash.
 */
val LocalHaptics = staticCompositionLocalOf<Haptics> { Haptics { } }

/**
 * The Android half.
 *
 * Built on `ViewCompat.performHapticFeedback` rather than `Vibrator`, for
 * three reasons that all matter: it needs no permission, it honours the
 * phone's own touch-feedback setting without being asked, and the compat
 * class maps every modern constant down to something sensible on older
 * releases. The whole modern vocabulary - toggles, thresholds, segment ticks
 * - only arrived in Android 14, and this app runs back to 8.
 *
 * The flags that override the user's global setting are deliberately not
 * used. If somebody has turned haptics off on their phone, they have turned
 * them off here.
 */
@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { ViewHaptics(view) }
}

private class ViewHaptics(private val view: View) : Haptics {

    /**
     * The last time each token fired.
     *
     * Without this, a fast toggler - somebody flicking through dates, or
     * changing their mind twice on a chip - gets a continuous buzz instead of
     * discrete taps, which is the difference between a control that feels
     * mechanical and one that feels broken.
     */
    private val lastAt = HashMap<Feedback, Long>()

    override fun play(feedback: Feedback) {
        val now = SystemClock.uptimeMillis()
        val previous = lastAt[feedback] ?: 0L
        if (now - previous < DEBOUNCE_MS) return
        lastAt[feedback] = now
        ViewCompat.performHapticFeedback(view, constantFor(feedback))
    }

    private fun constantFor(feedback: Feedback): Int = when (feedback) {
        Feedback.Selection -> HapticFeedbackConstantsCompat.SEGMENT_TICK
        Feedback.Commit -> HapticFeedbackConstantsCompat.CONFIRM
        Feedback.Success -> HapticFeedbackConstantsCompat.CONFIRM
        Feedback.Warning -> HapticFeedbackConstantsCompat.REJECT
        Feedback.Error -> HapticFeedbackConstantsCompat.REJECT
        Feedback.ToggleOn -> HapticFeedbackConstantsCompat.TOGGLE_ON
        Feedback.ToggleOff -> HapticFeedbackConstantsCompat.TOGGLE_OFF
    }

    private companion object {
        const val DEBOUNCE_MS = 50L
    }
}
