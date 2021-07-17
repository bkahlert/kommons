package koodies.tracing

import koodies.junit.TestName
import koodies.test.CapturedOutput
import koodies.test.SystemIOExclusive
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.joinLinesToString
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import koodies.tracing.rendering.Renderable
import koodies.tracing.rendering.RenderingAttributes
import koodies.tracing.rendering.RenderingAttributes.Keys.DESCRIPTION
import koodies.tracing.rendering.ReturnValue
import koodies.tracing.rendering.Styles
import koodies.tracing.rendering.Styles.None
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Isolated
@SystemIOExclusive
@NoTestSpan
class RenderingSpanKtTest {

    @Nested
    inner class Tracing {

        @Nested
        inner class WithNoCurrentSpan {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have invalid span`() {
                    val spanId = tracing { SpanId.current }
                    expectThat(spanId).not { isValid() }
                }

                @Test
                fun `should not trace`() {
                    val traceId = tracing { TraceId.current }
                    traceId.expectTraced().isEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    tracing { log("event α") }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have invalid span`() {
                    val spanId = tracing { tracing { SpanId.current } }
                    expectThat(spanId).not { isValid() }
                }

                @Test
                fun `should not trace`() {
                    val traceId = tracing { tracing { TraceId.current } }
                    traceId.expectTraced().isEmpty()
                }

                @Test
                fun `should not render`(output: CapturedOutput) {
                    tracing { tracing { log("event α") } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = tracing { spanning(testName) { registerAsTestSpan(); SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = tracing { spanning(testName) { registerAsTestSpan(); TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("RenderingSpanKtTest ➜ Tracing ➜ WithNoCurrentSpan ➜ NestedSpanning ➜ should trace")
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    tracing { spanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); log("event α") } }
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
        inner class WithCurrentSpan {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { tracing { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { tracing { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should not render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { tracing { log("event α") } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedTracing {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { tracing { tracing { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { tracing { tracing { TraceId.current } } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should not render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { tracing { tracing { log("event α") } } }
                    expectThat(output.all).isEmpty()
                }
            }

            @Nested
            inner class NestedSpanning {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { tracing { spanning("child") { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { tracing { spanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { tracing { spanning("child") { log("event α") } } }
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
        inner class WithNoCurrentSpan {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = spanning(testName) { registerAsTestSpan(); SpanId.current }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { registerAsTestSpan(); TraceId.current }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); log("event α") }
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
                    val spanId = spanning(testName) { registerAsTestSpan(); tracing { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { registerAsTestSpan(); tracing { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); tracing { log("event α") } }
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
                    val spanId = spanning(testName) { registerAsTestSpan(); spanning("child") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { registerAsTestSpan(); spanning("child") { TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName, layout = ColumnsLayout(DESCRIPTION columns 200)) { registerAsTestSpan(); spanning("child") { log("event α") } }
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
        inner class WithCurrentSpan {

            @Nested
            inner class TopLevel {

                @Test
                fun `should have valid span`(testName: TestName) {
                    val spanId = withRootSpan(testName) { spanning("parent") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanning("parent") { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly("parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanning("parent") { log("event α") } }
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
                    val spanId = withRootSpan(testName) { spanning("parent") { tracing { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanning("parent") { tracing { TraceId.current } } }
                    traceId.expectTraced().spanNames.containsExactly("parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanning("parent") { tracing { log("event α") } } }
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
                    val spanId = withRootSpan(testName) { spanning("parent") { spanning("child") { SpanId.current } } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = withRootSpan(testName) { spanning("parent") { spanning("child") { TraceId.current } } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", "parent", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    withRootSpan(testName) { spanning("parent") { spanning("child") { log("event α") } } }
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
                val traceId = withRootSpan(testName) { spanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { TraceId.current } }
                traceId.expectTraced().spanNames.first()
                    .isEqualTo("null x null")
            }

            @Test
            fun `should use render as rendered name`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { spanning(renderableName, nameFormatter = FilteringFormatter.ToCharSequence) { } }
                expectThat(output).toStringMatchesCurlyPattern("╎ null x null ✔︎")
            }
        }

        @Nested
        inner class CustomSettings {

            @Test
            fun `should update name formatter`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { spanning("name", nameFormatter = { "!$it!" }) { log("message") } }
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
                withRootSpan(testName) { spanning("name", contentFormatter = { "!$it!" }) { log("message") } }
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
                withRootSpan(testName) { spanning("name", decorationFormatter = { "!$it!" }) { log("message") } }
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
                    spanning("name", returnValueTransform = {
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
                    spanning("${"1234567".ansi.blue}     ${"12345".ansi.brightBlue}",
                        style = None,
                        layout = ColumnsLayout(RenderingAttributes.DESCRIPTION columns 7, RenderingAttributes.EXTRA columns 5)) {
                        @Suppress("SpellCheckingInspection")
                        log("messagegoes      here", RenderingAttributes.EXTRA to "worksgreat-----")
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
                withRootSpan(testName) { spanning("name", style = Styles.Dotted) { log("message") } }
                expectThat(output).toStringMatchesCurlyPattern("""
                    ▶ name
                    · message                                                                         
                    ✔︎
                """.trimIndent())
            }

            @Test
            fun `should update printer`(testName: TestName, output: CapturedOutput) {
                val rendered = mutableListOf<CharSequence>()
                withRootSpan(testName) { spanning("name", printer = { rendered.add(it) }) { log("message") } }
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
        val parentSpan = Tracer.spanBuilder(testName.value).startSpan().registerAsTestSpan()
        val scope = parentSpan.makeCurrent()
        val result = runCatching(block)
        scope.close()
        parentSpan.end()
        return result.getOrThrow()
    }
}
