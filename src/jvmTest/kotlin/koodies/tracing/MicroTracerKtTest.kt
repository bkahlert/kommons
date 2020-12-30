package koodies.tracing

import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.matchesCurlyPattern
import koodies.text.Grapheme
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class MicroTracerKtTest {
    @Test
    fun InMemoryLogger.`should micro seq`() {
        subTrace<Any?>("segment") {
            trace("@")
            microTrace<Any?>(Grapheme("🤠")) {
                trace("a")
                trace("")
                trace("b c")
            }
            trace("@")
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   segment @ (🤠 a ˃  ˃ b c) @ {}
        """.trimIndent())
    }
}
