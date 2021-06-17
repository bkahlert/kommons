package koodies.tracing

import koodies.test.actual
import koodies.text.matchesCurlyPattern
import koodies.toBaseName
import koodies.tracing.Span.State.Started
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.size

class TestSpanTest {

    @Test
    fun TestSpan.`should be started`() {
        expectThat(state).isA<Started>()
    }

    @Test
    fun TestSpan.`should have trace ID`() {
        expectThat(traceId).isNotNull() and {
            isValid()
        }
    }

    @Test
    fun TestSpan.`should have span ID`() {
        expectThat(spanId).isNotNull() and {
            isValid()
        }
    }

    @Test
    fun TestSpan.`should trace`() {
        log("event -1")
        spanning("SPAN A") { log("event α") }
        log("event n+1")
        endAndExpect {
            all { isValid() }
            all { isOkay() }
            size.isEqualTo(2)
            var parentSpanId: String? = null
            get(1) and {
                parentSpanId = actual.spanId
                spanName.contains("should trace")
                events and {
                    size.isEqualTo(2)
                    get(0) and { eventName.isEqualTo("event -1".toBaseName()) }
                    get(1) and { eventName.isEqualTo("event n+1".toBaseName()) }
                }
            }
            get(0) and {
                get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                spanName.isEqualTo("SPAN A")
                events and {
                    size.isEqualTo(1)
                    get(0) and { eventName.isEqualTo("event α".toBaseName()) }
                }
            }
        }
    }

    @Test
    fun TestSpan.`should render`() {
        log("event -1")
        spanning("SPAN A") { log("event α") }
        log("event n+1")
        expectThatRendered().matchesCurlyPattern("""
            event--1: {description=event -1}
            ╭──╴SPAN A
            │
            │   event α                                                                 
            │
            ╰──╴✔︎
            event-n_1: {description=event n+1}
        """.trimIndent())
    }
}
