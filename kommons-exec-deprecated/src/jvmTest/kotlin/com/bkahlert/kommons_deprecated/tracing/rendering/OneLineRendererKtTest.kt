package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons_deprecated.tracing.TestSpanScope
import com.bkahlert.kommons_deprecated.tracing.runSpanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
            rendered() shouldMatchGlob """
                ╭──╴block
                │
                │   block event
                │   ╶──╴one-line╶─╴one-line event ✔︎
                │
                ╰──╴✔︎
            """.trimIndent()
        }

        @Test
        fun TestSpanScope.`should be customizable`() {
            runSpanningLine("parent", contentFormatter = { "!$it!" }) { runSpanning("child") { log("event") } }
            rendered() shouldMatchGlob "╶──╴parent ╶──╴child╶─╴!event! ✔︎ ✔︎"
        }

        @Test
        fun TestSpanScope.`should keep one-line rendering for nested spans`() {
            runSpanningLine("root") { runSpanning("parent") { runSpanning("child") { log("event") } } }
            rendered() shouldMatchGlob "╶──╴root ╶──╴parent ╶──╴child╶─╴event ✔︎ ✔︎ ✔︎"
        }

        @Test
        fun TestSpanScope.`should support consecutive one-line span switches`() {
            runSpanningLine("parent") { runSpanningLine("child") { log("event") } }
            rendered() shouldMatchGlob "╶──╴parent ╶──╴child╶─╴event ✔︎ ✔︎"
        }
    }
}
