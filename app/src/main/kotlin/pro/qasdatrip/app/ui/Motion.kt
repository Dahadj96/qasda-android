package pro.qasdatrip.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavBackStackEntry

/**
 * How pages replace each other.
 *
 * This used to be six cubic-beziers and two durations, and they were not the
 * wrong ones: 320ms on (0.2, 0, 0, 1) is exactly Material 3's "emphasized"
 * token, copied faithfully. It still looked like footage rather than
 * navigation, and the reason is structural.
 *
 * A tween is a timeline: position is a function of elapsed time alone. It has
 * no idea how fast anything was already moving. Interrupt one - a second tab
 * pressed before the first has settled, a back gesture released halfway - and
 * Compose starts a brand new curve from wherever the screen happens to be,
 * at zero speed. Every emphasized curve leaves the origin flat, so the eye
 * sees the page moving, stopping dead, and starting again. That stutter is
 * what reads as "edited together".
 *
 * A spring is a simulation over position *and* velocity. Redirect it and the
 * new motion inherits the speed the old one had, so an interruption is a
 * curve rather than a cut. It also solves a second complaint for free:
 * settle time falls out of distance and stiffness, so a chip and a
 * full-screen push no longer take the same 320ms - one of them was always
 * going to look wrong at a shared duration.
 *
 * The numbers below are Material 3 Expressive's own `standard` motion scheme,
 * written out as springs. They are not invented: when this project moves to
 * material3 1.4, every one of these can be replaced by the matching
 * `MaterialTheme.motionScheme` spec and nothing on screen should change.
 * Standard rather than expressive, deliberately - expressive's 0.6 damping is
 * lovely on a hero and wearing on the fortieth filter chip of a price search.
 */
object Motion {

    // Spatial: anything that changes shape or position. May overshoot.
    private const val SPATIAL_DAMPING = 0.9f
    private const val STIFF_FAST = 1400f
    private const val STIFF_DEFAULT = 700f
    private const val STIFF_SLOW = 300f

    // Effects: colour, alpha, elevation. Critically damped, always - an alpha
    // that overshoots is a flash, not a bounce.
    private const val EFFECT_DAMPING = 1f
    private const val EFFECT_FAST = 3800f
    private const val EFFECT_DEFAULT = 1600f

    val spatialFast: FiniteAnimationSpec<Float> = spring(SPATIAL_DAMPING, STIFF_FAST)
    val spatialDefault: FiniteAnimationSpec<Float> = spring(SPATIAL_DAMPING, STIFF_DEFAULT)
    val effectsFast: FiniteAnimationSpec<Float> = spring(EFFECT_DAMPING, EFFECT_FAST)
    val effectsDefault: FiniteAnimationSpec<Float> = spring(EFFECT_DAMPING, EFFECT_DEFAULT)

    /**
     * The offset springs carry a visibility threshold, and must.
     *
     * Compose's default threshold is 0.01, calibrated for a float between 0
     * and 1. Left at that on an IntOffset measured in pixels, the spring keeps
     * simulating long after the movement is invisible - burning frames and
     * holding the transition open. `IntOffset.VisibilityThreshold` is half a
     * pixel, which is where the eye actually stops.
     */
    /**
     * For anything whose measured height changes - a header folding away.
     *
     * Its own threshold for the same reason the offset spring has one: the
     * default 0.01 is calibrated for a float between 0 and 1, and left alone
     * on a size in pixels the spring keeps simulating long after the fold
     * has visibly finished.
     */
    val sizeDefault: FiniteAnimationSpec<IntSize> =
        spring(SPATIAL_DAMPING, STIFF_DEFAULT, IntSize.VisibilityThreshold)

    private val slideSlow: FiniteAnimationSpec<IntOffset> =
        spring(SPATIAL_DAMPING, STIFF_SLOW, IntOffset.VisibilityThreshold)

    /**
     * Switching tabs is not travel.
     *
     * Sliding sideways between Accueil and Compte would imply they sit next to
     * each other and that one is "back" from the other, which is false in a
     * bar you can jump around from any position. A fast fade with a hair of
     * scale says "different place" without claiming a direction - and it is
     * fast, because a root destination should feel like it was already there.
     */
    private val tabEnter: EnterTransition =
        fadeIn(effectsDefault) + scaleIn(spatialFast, initialScale = 0.96f)

    private val tabExit: ExitTransition = fadeOut(effectsFast)

    /** The four bar destinations. A move between any two of them is a jump. */
    private val roots = setOf("search", "tracking", "help", "account")

    private fun NavBackStackEntry.root(): Boolean =
        destination.route?.substringBefore('?') in roots

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.jumping(): Boolean =
        initialState.root() && targetState.root()

    /**
     * Push and pop travel different distances on purpose.
     *
     * The arriving page comes a quarter of the screen; the leaving one only
     * moves a sixth, the other way. That difference is what reads as depth -
     * the near thing moves further than the far thing - where two pages
     * sliding the same distance read as one filmstrip being dragged past.
     */
    private const val ENTER_FRACTION = 4
    private const val EXIT_FRACTION = 6

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (jumping()) tabEnter else {
            slideInHorizontally(slideSlow) { it / ENTER_FRACTION } + fadeIn(effectsFast)
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (jumping()) tabExit else {
            slideOutHorizontally(slideSlow) { -it / EXIT_FRACTION } + fadeOut(effectsFast)
        }
    }

    /**
     * Pop is written out rather than left to the framework.
     *
     * Navigation Compose falls back to a cross-fade for pop when it is not
     * told otherwise, and a cross-fade is precisely the "video edit" look.
     * It matters twice over now: predictive back *seeks* these transitions
     * against the drag, so whatever is written here is what the person's
     * thumb is scrubbing through.
     */
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (jumping()) tabEnter else {
            slideInHorizontally(slideSlow) { -it / EXIT_FRACTION } + fadeIn(effectsFast)
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (jumping()) tabExit else {
            slideOutHorizontally(slideSlow) { it / ENTER_FRACTION } + fadeOut(effectsFast)
        }
    }

    /**
     * A page that arrives from the bottom of the screen.
     *
     * For the two screens that are modal in feel rather than in stack
     * position - the filters, and the search summary reached from the
     * results - where sideways travel would claim they sit beside the list
     * they came from. The page underneath stays put and only dims, which is
     * what makes the thing on top read as temporary.
     *
     * Vertical travel is the full height, so this uses its own spring: a
     * quarter-screen slide reads as a nudge when the direction is down.
     */
    private val slideVertical: FiniteAnimationSpec<IntOffset> =
        spring(SPATIAL_DAMPING, STIFF_SLOW, IntOffset.VisibilityThreshold)

    val sheetEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(slideVertical) { it } + fadeIn(effectsFast)
    }

    val sheetExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(effectsFast)
    }

    val sheetPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(effectsFast)
    }

    val sheetPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(slideVertical) { it } + fadeOut(effectsFast)
    }
}
