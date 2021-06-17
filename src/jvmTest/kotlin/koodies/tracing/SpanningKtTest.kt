package koodies.tracing

import koodies.debug.CapturedOutput
import koodies.test.actual
import koodies.test.output.OutputCaptureExtension
import koodies.text.matchesCurlyPattern
import koodies.toBaseName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.size

@Isolated
class SpanningKtTest {

    @Nested
    inner class SpanningFunction {

        @Nested
        inner class WithoutCurrentSpan {

            @Test
            @ExtendWith(OutputCaptureExtension::class)
            fun `should print to console`(output: CapturedOutput) {
                OpenTelemetrySpan.spanning("span name", tracer = Tracer.NOOP) {
                    log("event α")
                }
                expectThat(output).matchesCurlyPattern("""
                    ╭──╴span name
                    │
                    │   event α                                                                         
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class WithCurrentSpan {

            @Test
            fun TestSpan.`should create nested span on already active span`() {
                koodies.tracing.spanning("indirectly nested") {
                    log("event α")
                }
                endAndExpect {
                    all { isValid() }
                    all { isOkay() }
                    size.isEqualTo(2)
                    var parentSpanId: String? = null
                    get(1) and {
                        parentSpanId = actual.spanId
                        spanName.contains("should create nested span on already active span")
                        events.isEmpty()
                    }
                    get(0) and {
                        get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                        spanName.isEqualTo("indirectly nested")
                        events and {
                            size.isEqualTo(1)
                            get(0) and { eventName.isEqualTo("event α".toBaseName()) }
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class SpanningExtensionFunction {

        @Nested
        inner class WithoutCurrentSpan {

            @Test
            @ExtendWith(OutputCaptureExtension::class)
            fun `should print to console`(output: CapturedOutput) {
                OpenTelemetrySpan.spanning("span name", tracer = Tracer.NOOP) {
                    log("event α")
                }
                expectThat(output).matchesCurlyPattern("""
                    ╭──╴span name
                    │
                    │   event α                                                                         
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class WithCurrentSpan {

            @Test
            fun TestSpan.`should create nested span on already active span`() {
                spanning("indirectly nested") {
                    log("event α")
                }
                endAndExpect {
                    all { isValid() }
                    all { isOkay() }
                    size.isEqualTo(2)
                    var parentSpanId: String? = null
                    get(1) and {
                        parentSpanId = actual.spanId
                        spanName.contains("should create nested span on already active span")
                        events.isEmpty()
                    }
                    get(0) and {
                        get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                        spanName.isEqualTo("indirectly nested")
                        events and {
                            size.isEqualTo(1)
                            get(0) and { eventName.isEqualTo("event α".toBaseName()) }
                        }
                    }
                }
            }
        }
    }
}
