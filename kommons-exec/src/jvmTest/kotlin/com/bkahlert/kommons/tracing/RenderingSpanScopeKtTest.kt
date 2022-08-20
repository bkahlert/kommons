package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.CapturedOutput
import com.bkahlert.kommons.test.SystemIOExclusive
import com.bkahlert.kommons.test.junit.DisplayName
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout.Companion.columns
import com.bkahlert.kommons.tracing.rendering.Renderable
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes.Keys.DESCRIPTION
import com.bkahlert.kommons.tracing.rendering.ReturnValue
import com.bkahlert.kommons.tracing.rendering.Styles
import com.bkahlert.kommons.tracing.rendering.Styles.None
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat

@Isolated
@SystemIOExclusive
@NoTestSpan
class RenderingSpanScopeKtTest {

    @Nested
    inner class Tracing {

        @Nested
        inner class WithNoSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have invalid span`() {
                    val spanId = spanScope { SpanId.current }
                    spanId.valid shouldBe false
                }

                @Test
                fun `should not trace`() {
                    val traceId = spanScope { TraceId.current }
                    traceId.spans.shouldBeEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    spanScope { log("event α") }
                    output.all.shouldBeEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have invalid span`() {
                    val spanId = spanScope { spanScope { SpanId.current } }
                    spanId.valid shouldBe false
                }

                @Test
                fun `should not trace`() {
                    val traceId = spanScope { spanScope { TraceId.current } }
                    traceId.spans.shouldBeEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    spanScope { spanScope { log("event α") } }
                    output.all.shouldBeEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = spanScope { runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); SpanId.current } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = spanScope { runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.spans.map { it.name }
                        .shouldContainExactly("RenderingSpanScopeKtTest ➜ tracing ➜ with no span scope ➜ nested spanning ➜ should trace")
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    spanScope {
                        runSpanning(
                            displayName.composedDisplayName,
                            layout = ColumnsLayout(DESCRIPTION columns 200)
                        ) { registerAsTestSpan(); log("event α") }
                    }
                    output.toString() shouldMatchGlob """
                        ╭──╴${displayName.composedDisplayName}
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }
        }

        @Nested
        inner class WithSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { spanScope { SpanId.current } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { spanScope { TraceId.current } }
                    traceId.spans.map { it.name }.shouldContainExactly(displayName.composedDisplayName)
                }

                @Test
                fun `should not render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { spanScope { log("event α") } }
                    output.all.shouldBeEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { spanScope { spanScope { SpanId.current } } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { spanScope { spanScope { TraceId.current } } }
                    traceId.spans.map { it.name }.shouldContainExactly(displayName.composedDisplayName)
                }

                @Test
                fun `should not render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { spanScope { spanScope { log("event α") } } }
                    output.all.shouldBeEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { spanScope { runSpanning("child") { SpanId.current } } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { spanScope { runSpanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.spans.map { it.name }.shouldContainExactly("child", displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { spanScope { runSpanning("child") { log("event α") } } }
                    output.toString() shouldMatchGlob """
                        ╭──╴child
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }
        }
    }

    @Nested
    inner class Spanning {

        @Nested
        inner class WithNoSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); SpanId.current }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); TraceId.current }
                    traceId.spans.map { it.name }.shouldContainExactly(displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    runSpanning(displayName.composedDisplayName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); log("event α") }
                    output.toString() shouldMatchGlob """
                        ╭──╴${displayName.composedDisplayName}
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); spanScope { SpanId.current } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); spanScope { TraceId.current } }
                    traceId.spans.map { it.name }.shouldContainExactly(displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    runSpanning(
                        displayName.composedDisplayName,
                        layout = ColumnsLayout(DESCRIPTION columns 200)
                    ) { registerAsTestSpan(); spanScope { log("event α") } }
                    output.toString() shouldMatchGlob """
                        ╭──╴${displayName.composedDisplayName}
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); runSpanning("child") { SpanId.current } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = runSpanning(displayName.composedDisplayName) { registerAsTestSpan(); runSpanning("child") { TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.spans.map { it.name }.shouldContainExactly("child", displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    runSpanning(
                        displayName.composedDisplayName,
                        layout = ColumnsLayout(DESCRIPTION columns 200)
                    ) { registerAsTestSpan(); runSpanning("child") { log("event α") } }
                    output.toString() shouldMatchGlob """
                        ╭──╴${displayName.composedDisplayName}
                        │
                        │   ╭──╴child
                        │   │
                        │   │   event α
                        │   │
                        │   ╰──╴✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }
        }

        @Nested
        inner class WithSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { runSpanning("parent") { SpanId.current } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { runSpanning("parent") { TraceId.current } }
                    traceId.spans.map { it.name }.shouldContainExactly("parent", displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { runSpanning("parent") { log("event α") } }
                    output.toString() shouldMatchGlob """
                        ╭──╴parent
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { runSpanning("parent") { spanScope { SpanId.current } } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { runSpanning("parent") { spanScope { TraceId.current } } }
                    traceId.spans.map { it.name }.shouldContainExactly("parent", displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { runSpanning("parent") { spanScope { log("event α") } } }
                    output.toString() shouldMatchGlob """
                        ╭──╴parent
                        │
                        │   event α
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(displayName: DisplayName) {
                    val spanId = withRootSpan(displayName) { runSpanning("parent") { runSpanning("child") { SpanId.current } } }
                    spanId.valid shouldBe true
                }

                @Test
                fun `should trace`(displayName: DisplayName) {
                    val traceId = withRootSpan(displayName) { runSpanning("parent") { runSpanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.spans.map { it.name }.shouldContainExactly("child", "parent", displayName.composedDisplayName)
                }

                @Test
                fun `should render`(displayName: DisplayName, output: CapturedOutput) {
                    withRootSpan(displayName) { runSpanning("parent") { runSpanning("child") { log("event α") } } }
                    output.toString() shouldMatchGlob """
                        ╭──╴parent
                        │
                        │   ╭──╴child
                        │   │
                        │   │   event α
                        │   │
                        │   ╰──╴✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent()
                }
            }
        }

        @Nested
        inner class UsingRenderable {

            private val renderableName = Renderable { columns, rows -> "$columns x $rows" }

            @Test
            fun `should use render with null args as recorded name`(displayName: DisplayName) {
                val traceId = withRootSpan(displayName) { runSpanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { TraceId.current } }
                traceId.spans.map { it.name }.first() shouldBe "null x null"
            }

            @Test
            fun `should use render as rendered name`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) { runSpanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { } }
                output.toString() shouldMatchGlob """
                    ╶──╴null x null ✔︎
                """.trimIndent()
            }
        }

        @Nested
        inner class CustomSettings {

            @Test
            fun `should update name formatter`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) { runSpanning("name", nameFormatter = { "!$it!" }) { log("message") } }
                output.toString() shouldMatchGlob """
                    ╭──╴!name!
                    │
                    │   message
                    │
                    ╰──╴✔︎
                """.trimIndent()
            }

            @Test
            fun `should update content formatter`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) { runSpanning("name", contentFormatter = { "!$it!" }) { log("message") } }
                output.toString() shouldMatchGlob """
                    ╭──╴name
                    │
                    │   !message!
                    │
                    ╰──╴✔︎
                """.trimIndent()
            }

            @Test
            fun `should update decoration formatter`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) { runSpanning("name", decorationFormatter = { "!$it!" }) { log("message") } }
                output.toString() shouldMatchGlob """
                    !╭──╴!name
                    !│!
                    !│!   message
                    !│!
                    !╰──╴!✔︎
                """.trimIndent()
            }

            @Test
            fun `should update return value transform`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) {
                    runSpanning("name", returnValueTransform = {
                        object : ReturnValue by it {
                            override fun format(): String = "✌️"
                        }
                    }) { log("message") }
                }
                output.toString() shouldMatchGlob """
                    ╭──╴name
                    │
                    │   message
                    │
                    ╰──╴✌️
                """.trimIndent()
            }

            @Disabled
            @Test
            fun `should update layout`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) {
                    runSpanning(
                        "${"1234567".ansi.blue}     ${"12345".ansi.brightBlue}",
                        style = None,
                        layout = ColumnsLayout(DESCRIPTION columns 7, EXTRA columns 5)
                    ) {
                        @Suppress("SpellCheckingInspection")
                        log("messagegoes      here", EXTRA to "worksgreat-----")
                    }
                }
                output.toString() shouldMatchGlob """
                    1234567     12345
                    message     works
                    goes        great
                       here     -----
                    ✔︎
                """.trimIndent()
            }

            @Test
            fun `should update block style`(displayName: DisplayName, output: CapturedOutput) {
                withRootSpan(displayName) { runSpanning("name", style = Styles.Dotted) { log("message") } }
                output.toString() shouldMatchGlob """
                    ▶ name
                    · message
                    ✔︎
                """.trimIndent()
            }

            @Test
            fun `should update printer`(displayName: DisplayName, output: CapturedOutput) {
                val rendered = mutableListOf<CharSequence>()
                withRootSpan(displayName) { runSpanning("name", printer = { rendered.add(it) }) { log("message") } }
                output.all.shouldBeEmpty()
                rendered.joinToString(LineSeparators.Default).ansiRemoved shouldMatchGlob """
                    ╭──╴name
                    │
                    │   message
                    │
                    ╰──╴✔︎
                """.trimIndent()
            }
        }
    }

    private fun <R> withRootSpan(displayName: DisplayName, block: () -> R): R {
        val parentSpan = KommonsTracer.spanBuilder(displayName.composedDisplayName).startSpan().registerAsTestSpan()
        val scope = parentSpan.makeCurrent()
        val result = runCatching(block)
        scope.close()
        parentSpan.end()
        return result.getOrThrow()
    }

    private val EXTRA: Key<String, Any> = Key.stringKey("kommons.extra") { it.toString() }
}
