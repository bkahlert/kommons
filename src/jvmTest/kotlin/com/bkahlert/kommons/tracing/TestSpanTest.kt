package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.test.actual
import com.bkahlert.kommons.test.hasElements
import com.bkahlert.kommons.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isEqualTo
import strikt.assertions.size

class TestSpanTest {

    @Test
    fun TestSpanScope.`should trace`() {
        log("event -1")
        runSpanning("SPAN A") { log("event α") }
        log("event n+1")
        endAndExpect {
            all { isValid() }
            size.isEqualTo(2)
            var parentSpanId: String? = null
            get(1) and {
                parentSpanId = actual.spanId
                spanName.contains("should trace")
                events.hasElements(
                    {
                        eventName.isEqualTo("log")
                        eventDescription.isEqualTo("event -1")
                    },
                    {
                        eventName.isEqualTo("log")
                        eventDescription.isEqualTo("event n+1")
                    },
                )
            }
            get(0) and {
                isOkay()
                get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                spanName.isEqualTo("SPAN A")
                events.hasElements(
                    {
                        eventName.isEqualTo("log")
                        eventDescription.isEqualTo("event α")
                    },
                )
            }
        }
    }

    @Test
    fun TestSpanScope.`should render`() {
        log("event -1")
        runSpanning("SPAN A") { log("event α") }
        log("event n+1")
        expectThatRendered().matchesCurlyPattern("""
            event -1
            ╭──╴SPAN A
            │
            │   event α                                                                 
            │
            ╰──╴✔︎
            event n+1
        """.trimIndent())
    }
}
