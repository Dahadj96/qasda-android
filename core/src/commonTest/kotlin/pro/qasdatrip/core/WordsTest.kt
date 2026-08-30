package pro.qasdatrip.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards on the string table.
 *
 * `Words` was a data class with a parameter per string until it reached 257
 * of them and stopped loading on a real phone: Dalvik can address at most 255
 * argument registers in a single invocation, so the generated constructor
 * call could no longer be expressed and the verifier rejected the class on
 * launch. It compiled, it minified, and these tests passed — a JVM has no
 * such limit, so nothing but a dex loader could see it.
 *
 * The shape that broke is gone (see Strings.kt), and the first test here
 * exists to stop it coming back: an interface with overridden properties has
 * no ceiling, and a constructor would reintroduce one silently.
 */
class WordsTest {

    private val all = listOf(Lang.FR, Lang.AR, Lang.EN).map { it to Words.of(it) }

    @Test
    fun `every language answers with its own table`() {
        assertEquals(3, all.map { it.second }.toSet().size)
    }

    @Test
    fun `no language is missing a string`() {
        // A blank is the shape a forgotten translation takes once the
        // compiler is no longer checking a constructor call.
        for ((lang, words) in all) {
            for ((name, value) in words.everyString()) {
                assertTrue(value.isNotBlank(), "$lang has an empty $name")
            }
        }
    }

    @Test
    fun `placeholders survive translation`() {
        // "{n} vols" translated to "رحلات" loses the number entirely, and the
        // screen then shows a sentence with a hole where the count was.
        val fr = Words.of(Lang.FR)
        for ((name, template) in fr.everyString()) {
            val slots = SLOT.findAll(template).map { it.value }.toSet()
            if (slots.isEmpty()) continue
            for ((lang, words) in all) {
                val theirs = words.everyString().toMap()[name].orEmpty()
                for (slot in slots) {
                    assertTrue(slot in theirs, "$lang lost $slot from $name")
                }
            }
        }
    }

    private companion object {
        val SLOT = Regex("\\{\\w+}")
    }
}
