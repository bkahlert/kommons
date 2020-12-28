package koodies.test.debug

import koodies.debug.debug
import koodies.terminal.AnsiColors.brightCyan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import java.util.concurrent.TimeUnit

/**
 * Annotated [Test] methods are run [Isolated] and sibling tests and their descendants
 * are ignored.
 */
@Timeout(10, unit = TimeUnit.MINUTES)
@Isolated
@Execution(SAME_THREAD)
annotation class Debug(val includeInReport: Boolean = true)

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
    }.get("⬆ DEBUGGED".brightCyan()) { this() }
