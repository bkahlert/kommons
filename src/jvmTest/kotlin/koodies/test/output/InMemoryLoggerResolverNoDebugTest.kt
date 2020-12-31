package koodies.test.output

import koodies.concurrent.process.IO.Type.OUT
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.toStringContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class InMemoryLoggerResolverNoDebugTest {

    @Test
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

        expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`() {
        logStatus { OUT typed "(*｀へ´*)" }

        expectCatching { logResult<Any> { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}
