package koodies.test.debug

import koodies.concurrent.process.IO
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.SystemIORead
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import org.junit.platform.commons.support.AnnotationSupport
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull

@Execution(CONCURRENT)
class DebugTest {

    @Test
    fun `should run in isolation`() {
        expectThat(AnnotationSupport.findAnnotation(InternalDebug::class.java, Isolated::class.java).orElse(null)).isNotNull()
    }

    @SystemIORead
    @Test
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logLine { IO.Output typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.ansiRemoved).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`() {
        logLine { IO.Output typed "(*｀へ´*)" }

        expectCatching { logResult<Any> { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}
