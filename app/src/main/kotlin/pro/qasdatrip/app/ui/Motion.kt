package pro.qasdatrip.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * How pages replace each other.
 *
 * The app shipped on Navigation Compose's defaults, and they read wrong on a
 * phone: the whole screen slides a full width in one direction while the old
 * one slides a full width out, with no fade, so the eye tracks two moving
 * pictures at once and the result looks cut together rather than navigated.
 *
 * What professional apps do instead is the shared-axis transition: the
 * outgoing page moves a *short* distance and fades out, the incoming page
 * moves the same short distance and fades in, and the fade — not the
 * distance — carries the change. Motion under about 40dp keeps the eye on the
 * content instead of on the animation, and the two halves overlap so there is
 * never a frame of empty canvas between them.
 *
 * Durations follow Material's guidance for a full-screen change and are
 * deliberately asymmetric: leaving is quicker than arriving, because nobody
 * needs to watch a page they have finished with.
 */
object Motion {

    /** Material's standard easing. Decelerates into place; never linear. */
    private val Emphasised = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    private const val ENTER_MS = 320
    private const val EXIT_MS = 240

    /**
     * How far a page travels. A twelfth of the screen: far enough to say
     * "this came from the right", short enough that it never reads as a swipe.
     */
    private fun offset(width: Int) = width / 12

    /**
     * Switching tabs is not travel.
     *
     * Sliding sideways between Accueil and Compte would imply they sit next to
     * each other and that one is "back" from the other, which is false in a
     * bar you can jump around. A fade with a hair of scale says "different
     * place" without claiming a direction.
     */
    private val tabEnter: EnterTransition =
        fadeIn(tween(260, easing = Emphasised)) +
            scaleIn(tween(260, easing = Emphasised), initialScale = 0.985f)

    private val tabExit: ExitTransition =
        fadeOut(tween(180, easing = Accelerate)) +
            scaleOut(tween(180, easing = Accelerate), targetScale = 1.015f)

    /** The four bar destinations. A move between any two of them is a jump. */
    private val roots = setOf("search", "tracking", "help", "account")

    private fun NavBackStackEntry.root(): Boolean =
        destination.route?.substringBefore('?') in roots

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.jumping(): Boolean =
        initialState.root() && targetState.root()

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (jumping()) tabEnter else {
            slideInHorizontally(tween(ENTER_MS, easing = Emphasised)) { offset(it) } +
                fadeIn(tween(ENTER_MS, delayMillis = 40, easing = Emphasised))
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (jumping()) tabExit else {
            slideOutHorizontally(tween(EXIT_MS, easing = Accelerate)) { -offset(it) } +
                fadeOut(tween(EXIT_MS, easing = Accelerate))
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (jumping()) tabEnter else {
            slideInHorizontally(tween(ENTER_MS, easing = Emphasised)) { -offset(it) } +
                fadeIn(tween(ENTER_MS, delayMillis = 40, easing = Emphasised))
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (jumping()) tabExit else {
            slideOutHorizontally(tween(EXIT_MS, easing = Accelerate)) { offset(it) } +
                fadeOut(tween(EXIT_MS, easing = Accelerate))
        }
    }
}
