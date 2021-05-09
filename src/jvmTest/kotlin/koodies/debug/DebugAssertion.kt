package koodies.test.debug

import koodies.debug.debug
import koodies.text.ANSI.Text.Companion.ansi
import strikt.api.Assertion

/**
 * Displays the current assertion subject using [debug] and returns
 * and [Assertion.Builder] with the original subject.
 */
fun <T> Assertion.Builder<T>.debug(): Assertion.Builder<T> =
    get("%s") {
        object : () -> T {
            override fun invoke(): T = this@get
            override fun toString(): String = this@get.debug
        }
    }.get("⬆ DEBUGGED".ansi.brightCyan.done) { this() }
