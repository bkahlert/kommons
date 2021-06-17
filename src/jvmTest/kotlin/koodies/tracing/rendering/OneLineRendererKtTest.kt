package koodies.tracing.rendering

import koodies.text.matchesCurlyPattern
import koodies.tracing.TestSpan
import koodies.tracing.log
import koodies.tracing.spanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OneLineRendererKtTest {

    @Nested
    inner class RendererSwitching {

        @Test
        fun TestSpan.`should switch to one-line rendering`() {
            spanning("block") {
                log("block event")
                spanningLine("one-line") {
                    log("one-line event")
                }
            }
            expectThatRendered().matchesCurlyPattern("""
                ╭──╴block
                │
                │   block event
                │   ❰❰ one-line ❱ one-line event ❱ ✔︎ ❱❱
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should be customizable`() {
            spanningLine("parent", { copy(contentFormatter = { "!$it!" }) }) { spanning("child") { log("event") } }
            expectThatRendered().matchesCurlyPattern("❰❰ !parent! ❱  !child! » !event! » ✔︎  ❱ ✔︎ ❱❱")
        }

        @Test
        fun TestSpan.`should keep one-line rendering for nested spans`() {
            spanningLine("parent") { spanning("child") { log("event") } }
            expectThatRendered().matchesCurlyPattern("❰❰ parent ❱  child » event » ✔︎  ❱ ✔︎ ❱❱")
        }

        @Test
        fun TestSpan.`should support consecutive one-line span switches`() {
            spanningLine("parent") { spanningLine("child") { log("event") } }
            expectThatRendered().matchesCurlyPattern("❰❰ parent ❱  child » event » ✔︎  ❱ ✔︎ ❱❱")
        }
    }
}
