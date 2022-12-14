package com.bkahlert.kommons.debug

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class JvmTraceKtTest {

    @Test fun trace_with_highlighting() = testAll {
        buildString {
            "subject".trace("caption", out = this::append) { it.length.toString() }
        }.traceTestPostProcess() shouldBe """
                [1;36mcaption[0m [1;36m⟨[0m [96m"subject"[0m [1;36m⟩[0m [1;36m{[0m [96m"7"[0m [1;36m}[0m
            """.trimIndent()
    }

    @Test fun trace_with_call_site() = testAll {
        buildString {
            "subject".trace(out = this::append) { it.length.toString() }
        }.traceTestPostProcess() shouldBe """
                    .ͭ (JvmTraceKtTest.kt:21) ⟨ "subject" ⟩ { "7" }
                """.trimIndent()
    }

    @Test fun inspect_with_call_site() = testAll {
        buildString {
            "subject".inspect(out = this::append)
        }.traceTestPostProcess() shouldBe """
                    .ͭ (JvmTraceKtTest.kt:29) ⟨ !String "subject" ⟩
                """.trimIndent()
    }
}

internal actual fun String.traceTestPostProcess(): String {
    val testName = checkNotNull(StackTrace.get().findByLastKnownCallsOrNull(::traceTestPostProcess)?.methodName) { "failed to determine test name" }
    val keepHighlighting = testName.contains("_with_highlighting")
    val keepCallSite = testName.contains("_with_call_site")
    var output = this
    if (!keepHighlighting) output = output.ansiRemoved
    if (!keepCallSite) output = output.dropWhile { it != ')' }.drop(2)
    return output
}
