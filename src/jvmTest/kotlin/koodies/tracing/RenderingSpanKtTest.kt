package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import koodies.debug.CapturedOutput
import koodies.junit.TestName
import koodies.test.output.OutputCaptureExtension
import koodies.text.matchesCurlyPattern
import koodies.text.truncateByColumns
import koodies.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import koodies.tracing.rendering.Renderable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@Isolated
@ExtendWith(OutputCaptureExtension::class)
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
                    expectThat(output).isEmpty()
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
                    expectThat(output).isEmpty()
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
                    tracing { spanning(testName) { registerAsTestSpan(); log("event α") } }
                    expectThat(output).matchesCurlyPattern("""
                        ╭──╴${testName.truncateByColumns(76)}
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
                    expectThat(output).isEmpty()
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
                    expectThat(output).isEmpty()
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
                    expectThat(output).matchesCurlyPattern("""
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
                    spanning(testName) { registerAsTestSpan(); log("event α") }
                    expectThat(output).matchesCurlyPattern("""
                        ╭──╴${testName.truncateByColumns(76)}
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
                    spanning(testName) { registerAsTestSpan(); tracing { log("event α") } }
                    expectThat(output).matchesCurlyPattern("""
                        ╭──╴${testName.truncateByColumns(76)}
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
                    spanning(testName) { registerAsTestSpan(); spanning("child") { log("event α") } }
                    expectThat(output).matchesCurlyPattern("""
                        ╭──╴${testName.truncateByColumns(76)}
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
                    expectThat(output).matchesCurlyPattern("""
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
                    expectThat(output).matchesCurlyPattern("""
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
                    expectThat(output).matchesCurlyPattern("""
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
                val traceId = withRootSpan(testName) { spanning(renderableName) { TraceId.current } }
                traceId.expectTraced().spanNames.first()
                    .isEqualTo("null x null")
            }

            @Test
            fun `should use render as rendered name`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { spanning(renderableName) { } }
                expectThat(output).matchesCurlyPattern("""
                    ╭──╴76 x 4
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class SpanName {

            @Test
            fun `should use name`(testName: TestName) {
                val traceId = withRootSpan(testName) { spanning("name") { TraceId.current } }
                traceId.expectTraced().spanNames.first()
                    .isEqualTo("name")
            }

            @Test
            fun `should use renderable name for rendering if exists`(testName: TestName, output: CapturedOutput) {
                withRootSpan(testName) { spanning("name", "name.renderable" to "renderable name") { } }
                expectThat(output).matchesCurlyPattern("""
                    ╭──╴renderable name
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should filter renderable name from attributes`(testName: TestName) {
                val traceId = withRootSpan(testName) { spanning("name", "name.renderable" to "renderable name") { TraceId.current } }
                traceId.expectTraced().first().spanAttributes.get { get(AttributeKey.stringKey(RenderingAttributes.Keys.NAME)) }.isNull()
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
