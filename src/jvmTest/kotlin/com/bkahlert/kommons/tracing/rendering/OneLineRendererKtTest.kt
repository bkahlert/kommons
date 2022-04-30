package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.runSpanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("xxx")
class OneLineRendererKtTest {

    @Nested
    inner class RendererSwitching {

        @Test
        fun TestSpanScope.`should switch to one-line rendering`() {
            runSpanning("block") {
                log("block event")
                runSpanningLine("one-line") {
                    log("one-line event")
                }
            }
            expectThatRendered().matchesCurlyPattern("""
                ╭──╴block
                │
                │   block event                         
                │   ╶──╴one-line╶─╴one-line event ✔︎
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Test
        fun TestSpanScope.`should be customizable`() {
            runSpanningLine("parent", contentFormatter = { "!$it!" }) { runSpanning("child") { log("event") } }
            expectThatRendered().matchesCurlyPattern("╶──╴parent ╶──╴child╶─╴!event! ✔︎ ✔︎")
        }

        @Test
        fun TestSpanScope.`should keep one-line rendering for nested spans`() {
            runSpanningLine("root") { runSpanning("parent") { runSpanning("child") { log("event") } } }
            expectThatRendered().matchesCurlyPattern("╶──╴root ╶──╴parent ╶──╴child╶─╴event ✔︎ ✔︎ ✔︎")
        }

        @Test
        fun TestSpanScope.`should support consecutive one-line span switches`() {
            runSpanningLine("parent") { runSpanningLine("child") { log("event") } }
            expectThatRendered().matchesCurlyPattern("╶──╴parent ╶──╴child╶─╴event ✔︎ ✔︎")
        }
    }
}
