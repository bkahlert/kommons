package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.test.CapturedOutput
import com.bkahlert.kommons.test.SystemIOExclusive
import com.bkahlert.kommons.test.junit.TestName
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import com.bkahlert.kommons.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout.Companion.columns
import com.bkahlert.kommons.tracing.rendering.Renderable
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes.Keys.DESCRIPTION
import com.bkahlert.kommons.tracing.rendering.ReturnValue
import com.bkahlert.kommons.tracing.rendering.Styles
import com.bkahlert.kommons.tracing.rendering.Styles.None
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Tag("xxx")
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
                    expectThat(spanId).not { isValid() }
                }

                @Test
                fun `should not trace`() {
                    val traceId = spanScope { TraceId.current }
                    traceId.expectTraced().isEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    spanScope { log("event α") }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have invalid span`() {
                    val spanId = spanScope { spanScope { SpanId.current } }
                    expectThat(spanId).not { isValid() }
                }

                @Test
                fun `should not trace`() {
                    val traceId = spanScope { spanScope { TraceId.current } }
                    traceId.expectTraced().isEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    spanScope { spanScope { log("event α") } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = spanScope { runSpanning(testName) { registerAsTestSpan(); SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanScope { runSpanning(testName) { registerAsTestSpan(); TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("RenderingSpanScopeKtTest ➜ Tracing ➜ WithNoSpanScope ➜ NestedSpanning ➜ should trace")
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanScope { runSpanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); log("event α") } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴$testName
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }
        }

        @Nested
        inner class WithSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { spanScope { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanScope { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should not render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanScope { log("event α") } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { spanScope { spanScope { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanScope { spanScope { TraceId.current } } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should not render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanScope { spanScope { log("event α") } } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { spanScope { runSpanning("child") { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanScope { runSpanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanScope { runSpanning("child") { log("event α") } } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴child
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
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
                fun `should have valid span`(testName: TestName) {
                    val spanId = runSpanning(testName) { registerAsTestSpan(); SpanId.current }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = runSpanning(testName) { registerAsTestSpan(); TraceId.current }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    runSpanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); log("event α") }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴$testName
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = runSpanning(testName) { registerAsTestSpan(); spanScope { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = runSpanning(testName) { registerAsTestSpan(); spanScope { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    runSpanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); spanScope { log("event α") } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴$testName
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = runSpanning(testName) { registerAsTestSpan(); runSpanning("child") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = runSpanning(testName) { registerAsTestSpan(); runSpanning("child") { TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    runSpanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); runSpanning("child") { log("event α") } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴$testName
                        │
                        │   ╭──╴child
                        │   │
                        │   │   event α
                        │   │
                        │   ╰──╴✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }
        }

        @Nested
        inner class WithSpanScope {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { runSpanning("parent") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { runSpanning("parent") { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly("parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { runSpanning("parent") { log("event α") } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴parent
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { runSpanning("parent") { spanScope { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { runSpanning("parent") { spanScope { TraceId.current } } }
                    traceId.expectTraced().spanNames.containsExactly("parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { runSpanning("parent") { spanScope { log("event α") } } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴parent
                        │
                        │   event α                                                                         
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { runSpanning("parent") { runSpanning("child") { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { runSpanning("parent") { runSpanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", "parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { runSpanning("parent") { runSpanning("child") { log("event α") } } }
                    expectThat(output).toStringMatchesCurlyPattern("""
                        ╭──╴parent
                        │
                        │   ╭──╴child
                        │   │
                        │   │   event α                                                                         
                        │   │
                        │   ╰──╴✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
                }
            }
        }

        @Nested
        inner class UsingRenderable {

            private val renderableName = Renderable { columns, rows -> "$columns x $rows" }

            @Test
            fun `should use render with null args as recorded name`(testName: TestName) {
                val traceId = withRootSpan(testName) { runSpanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { TraceId.current } }
                traceId.expectTraced().spanNames.first()
                    .isEqualTo("null x null")
            }

            @Test
            fun `should use render as rendered name`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { runSpanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { } }
                expectThat(output).toStringMatchesCurlyPattern("╶──╴null x null ✔︎")
            }
        }

        @Nested
        inner class CustomSettings {

            @Test
            fun `should update name formatter`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { runSpanning("name", nameFormatter = { "!$it!" }) { log("message") } }
                expectThat(output).toStringMatchesCurlyPattern("""
                    ╭──╴!name!
                    │
                    │   message                                                                         
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should update content formatter`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { runSpanning("name", contentFormatter = { "!$it!" }) { log("message") } }
                expectThat(output).toStringMatchesCurlyPattern("""
                    ╭──╴name
                    │
                    │   !message!                                                                       
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should update decoration formatter`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { runSpanning("name", decorationFormatter = { "!$it!" }) { log("message") } }
                expectThat(output).toStringMatchesCurlyPattern("""
                    !╭──╴!name
                    !│!
                    !│!   message                                                                         
                    !│!
                    !╰──╴!✔︎
                """.trimIndent())
            }

            @Test
            fun `should update return value transform`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) {
                    runSpanning("name", returnValueTransform = {
                        object : ReturnValue by it {
                            override fun format(): String = "✌️"
                        }
                    }) { log("message") }
                }
                expectThat(output).toStringMatchesCurlyPattern("""
                    ╭──╴name
                    │
                    │   message                                                                         
                    │
                    ╰──╴✌️
                """.trimIndent())
            }

            @Test
            fun `should update layout`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) {
                    runSpanning("${"1234567".ansi.blue}     ${"12345".ansi.brightBlue}",
                        style = None,
                        layout = ColumnsLayout(DESCRIPTION columns 7, EXTRA columns 5)) {
                        @Suppress("SpellCheckingInspection")
                        log("messagegoes      here", EXTRA to "worksgreat-----")
                    }
                }
                expectThat(output).toStringMatchesCurlyPattern("""
                    1234567     12345
                    message     works
                    goes        great
                       here     -----
                    ✔︎
                """.trimIndent())
            }

            @Test
            fun `should update block style`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { runSpanning("name", style = Styles.Dotted) { log("message") } }
                expectThat(output).toStringMatchesCurlyPattern("""
                    ▶ name
                    · message                                                                         
                    ✔︎
                """.trimIndent())
            }

            @Test
            fun `should update printer`(testName: TestName, output: CapturedOutput) {
                val rendered = mutableListOf<CharSequence>()
                withRootSpan(testName) { runSpanning("name", printer = { rendered.add(it) }) { log("message") } }
                expectThat(output.all).isEmpty()
                expectThat(rendered.joinLinesToString()).matchesCurlyPattern("""
                    ╭──╴name
                    │
                    │   message
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    private fun <R> withRootSpan(testName: TestName, block: () -> R): R {
        val parentSpan = KommonsTracer.spanBuilder(testName.value).startSpan().registerAsTestSpan()
        val scope = parentSpan.makeCurrent()
        val result = runCatching(block)
        scope.close()
        parentSpan.end()
        return result.getOrThrow()
    }

    private val EXTRA: Key<String, Any> = Key.stringKey("kommons.extra") { it.toString() }
}
