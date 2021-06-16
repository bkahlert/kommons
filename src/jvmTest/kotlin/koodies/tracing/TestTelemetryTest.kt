package koodies.tracing

import koodies.test.Smoke
import koodies.text.matchesCurlyPattern
import koodies.time.seconds
import koodies.unit.milli
import org.junit.jupiter.api.Test

class TestTelemetryTest {

    @Smoke @Test
    fun Span.`should render complex`() {
        log("event α")
        spanning("SPAN A") {
            log("event β")
            spanning("SPAN A.1") {
                log("event γ", "runtime" to 120.milli.seconds)
                log("event δ")
                log("event ε")
            }
            spanning("SPAN A.2") {
                // no events
            }
            event("event ζ")
        }
        spanning("SPAN B") {
            spanning("SPAN B.1") {
                spanning("SPAN B.1.1") {
                    // no events
                }
                spanning("SPAN B.1.2") {
                    log("event η")
                    spanning("SPAN B.1.2.1") {
                        log("event θ")
                    }
                }
                log("event ι")
            }
            spanning("SPAN B.2") {
                log("event κ")
                log("event λ")
            }
        }

        expectThatRendered().matchesCurlyPattern("""
            event α
            ╭──╴SPAN A
            │
            │   event β
            │   ╭──╴SPAN A.1
            │   │
            │   │   event γ
            │   │   event δ
            │   │   event ε
            │   │
            │   ╰──╴✔︎
            │   SPAN A.2 ✔︎
            │
            ╰──╴✔︎
            ╭──╴SPAN B
            │
            │   ╭──╴SPAN B.1
            │   │
            │   │   SPAN B.1.1 ✔︎
            │   │   ╭──╴SPAN B.1.2
            │   │   │
            │   │   │   event η
            │   │   │   ╭──╴SPAN B.1.2.1
            │   │   │   │
            │   │   │   │   event θ
            │   │   │   │
            │   │   │   ╰──╴✔︎
            │   │   │
            │   │   ╰──╴✔︎
            │   │   event ι
            │   │
            │   ╰──╴✔︎
            │   ╭──╴SPAN B.2
            │   │
            │   │   event κ
            │   │   event λ
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
