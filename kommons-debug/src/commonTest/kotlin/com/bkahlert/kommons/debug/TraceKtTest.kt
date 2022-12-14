package com.bkahlert.kommons.debug

import com.bkahlert.kommons.debug.CustomToString.Ignore
import com.bkahlert.kommons.debug.Typing.SimplyTyped
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@Suppress("DEPRECATION")
class TraceTest {

    @Test fun trace_with_no_arguments() = testAll {
        buildString {
            "subject".trace(out = this::append)
        }.traceTestPostProcess() shouldBe """
            ⟨ "subject" ⟩
        """.trimIndent()
    }

    @Test fun trace_with_caption() = testAll {
        buildString {
            "subject".trace("caption", out = this::append)
        }.traceTestPostProcess() shouldBe """
            caption ⟨ "subject" ⟩
        """.trimIndent()
    }

    @Test fun trace_with_inspect() = testAll {
        buildString {
            "subject".trace(out = this::append) { it.length }
        }.traceTestPostProcess() shouldBe """
            ⟨ "subject" ⟩ { 7／0x07 }
        """.trimIndent()
    }

    @Test fun trace_with_caption_and_inspect() = testAll {
        buildString {
            "subject".trace("caption", out = this::append) { it.length }
        }.traceTestPostProcess() shouldBe """
            caption ⟨ "subject" ⟩ { 7／0x07 }
        """.trimIndent()
    }

    @Test fun trace_with_custom_render() = testAll {
        buildString {
            "subject".trace("caption", render = { ">>> $it <<<" }, out = this::append) { it.length.toString() }
        }.traceTestPostProcess() shouldBe """
            caption ⟨ >>> subject <<< ⟩ { >>> 7 <<< }
        """.trimIndent()
    }

    @Test fun trace_with_multiline() = testAll {
        buildString {
            "subject 1\nsubject 2".trace(
                out = this::append,
            )
        }.traceTestPostProcess() shouldBe """
            ⟨
            "${"\""}"
            subject 1
            subject 2
            "${"\""}"
            ⟩
        """.trimIndent()

        buildString {
            "subject 1\nsubject 2".trace(
                "caption",
                out = this::append,
            )
        }.traceTestPostProcess() shouldBe """
            caption ⟨
            "${"\""}"
            subject 1
            subject 2
            "${"\""}"
            ⟩
        """.trimIndent()

        buildString {
            "subject 1\nsubject 2".trace(
                "caption",
                out = this::append,
            ) { "inspect" }
        }.traceTestPostProcess() shouldBe """
            caption ⟨
            "${"\""}"
            subject 1
            subject 2
            "${"\""}"
            ⟩ { "inspect" }
        """.trimIndent()

        buildString {
            "subject".trace(
                "caption",
                out = this::append,
            ) { "inspect 1\ninspect 2" }
        }.traceTestPostProcess() shouldBe """
            caption ⟨ "subject" ⟩ {
            "${"\""}"
            inspect 1
            inspect 2
            "${"\""}"
            }
        """.trimIndent()

        buildString {
            "subject 1\nsubject 2".trace(
                "caption",
                out = this::append,
            ) { "inspect 1\ninspect 2" }
        }.traceTestPostProcess() shouldBe """
            caption ⟨
            "${"\""}"
            subject 1
            subject 2
            "${"\""}"
            ⟩ {
            "${"\""}"
            inspect 1
            inspect 2
            "${"\""}"
            }
        """.trimIndent()
    }

    @Test fun inspect() = testAll {
        buildString {
            "subject".inspect(
                out = this::append,
            ) { it.length.toString() }
        }.traceTestPostProcess() shouldBe buildString {
            "subject".trace(
                render = { it.render { typing = SimplyTyped; customToString = Ignore } },
                out = this::append,
            ) { it.length.toString() }
        }.traceTestPostProcess()
    }
}

internal expect fun String.traceTestPostProcess(): String
