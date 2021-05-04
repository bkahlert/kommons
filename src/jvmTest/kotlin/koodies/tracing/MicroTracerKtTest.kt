package koodies.tracing

import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test

class MicroTracerKtTest {

    @Test
    fun InMemoryLogger.`should micro seq`() {
        subTrace<Any?>("segment") {
            trace("@")
            microTrace<Any?>("🤠") {
                trace("a")
                trace("")
                trace("b c")
            }
            trace("@")
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   segment @ (🤠 a ˃  ˃ b c) @ {}
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
