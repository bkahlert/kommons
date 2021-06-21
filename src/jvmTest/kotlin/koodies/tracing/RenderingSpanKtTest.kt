package koodies.tracing

import koodies.debug.CapturedOutput
import koodies.junit.TestName
import koodies.test.output.OutputCaptureExtension
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

@Isolated
@ExtendWith(OutputCaptureExtension::class)
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
                fun `should have valid span`() {
                    val spanId = tracing { spanning("child") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`() {
                    val traceId = tracing { spanning("child") { TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child")
                }

                @Test
                fun `should render`(output: CapturedOutput) {
                    tracing { spanning("child") { log("event α") } }
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
                    val spanId = spanning(testName) { SpanId.current }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { TraceId.current }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName) { log("event α") }
                    expectThat(output).matchesCurlyPattern("""
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
                    val spanId = spanning(testName) { tracing { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { tracing { TraceId.current } }
                    traceId.expectTraced().spanNames.containsExactly(testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName) { tracing { log("event α") } }
                    expectThat(output).matchesCurlyPattern("""
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
                    val spanId = spanning(testName) { spanning("child") { SpanId.current } }
                    expectThat(spanId).isValid()
                }

                @Test
                fun `should trace`(testName: TestName) {
                    val traceId = spanning(testName) { spanning("child") { TraceId.current } }
                    expectThat(traceId).isValid()
                    traceId.expectTraced().spanNames.containsExactly("child", testName.value)
                }

                @Test
                fun `should render`(testName: TestName, output: CapturedOutput) {
                    spanning(testName) { spanning("child") { log("event α") } }
                    expectThat(output).matchesCurlyPattern("""
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
    }

    private fun <R> withRootSpan(testName: TestName, block: () -> R): R {
        val parentSpan = Tracer.spanBuilder(testName.value).startSpan()
        val scope = parentSpan.makeCurrent()
        val result = runCatching(block)
        scope.close()
        parentSpan.end()
        return result.getOrThrow()
    }
}
