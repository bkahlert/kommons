package koodies.debug

import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.SystemIORead
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.junit.platform.commons.support.AnnotationSupport
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isNotNull

class DebugTest {

    @Test
    fun `should run in isolation`() {
        expectThat(AnnotationSupport.findAnnotation(Debug::class.java, Isolated::class.java).orElse(null)).isNotNull()
    }

    @SystemIORead
    @Test
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logLine { "test" }

        expectThatLogged().contains("test")
        expectThat(output).isEmpty()
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`() {
        expectThrows<RuntimeException> {
            logResult<Any>(Result.failure(RuntimeException("test")))
        }
    }
}
