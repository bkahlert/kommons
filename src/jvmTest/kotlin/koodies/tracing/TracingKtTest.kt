package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import koodies.test.Slow
import koodies.test.actual
import koodies.test.output.TestLogger
import koodies.text.isMultiLine
import koodies.time.seconds
import koodies.unit.milli
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion.Builder
import strikt.api.expectThrows
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.size

@Disabled
@Execution(CONCURRENT)
class TracingKtTest {

    @Slow @Test
    fun TestLogger.`should trace`() {
        expectThatTraced {
            all {
                hasValidIds()
                events.isEmpty()
            }
            size.isEqualTo(1)
            get(0) and { spanName.contains("should trace") }
        }
    }

    @Slow @Test
    fun TestLogger.`should record event`() {
        span.event("event name")

        expectThatTraced {
            all { hasValidIds() }
            size.isEqualTo(1)
            get(0) and {
                spanName.contains("should record event")
                events and {
                    size.isEqualTo(1)
                    get(0) and { eventName.isEqualTo("event name") }
                }
            }
        }
    }

    @Slow @Test
    fun TestLogger.`should record exception`() {
        span.exception(RuntimeException("exception message"))

        expectThatTraced {
            all { hasValidIds() }
            size.isEqualTo(1)
            get(0) and {
                spanName.contains("should record exception")
                events and {
                    size.isEqualTo(1)
                    get(0) and {
                        eventName.isEqualTo("exception")
                        attributes["exception.message"].isEqualTo("exception message")
                        attributes["exception.stacktrace"].isMultiLine()
                    }
                }
            }
        }
    }

    @Slow @Test
    fun TestLogger.`should trace nested`() {
        logging("SPAN A") {
            span.event("event α")
        }

        expectThatTraced {
            all { hasValidIds() }
            size.isEqualTo(2)
            var parentSpanId: String? = null
            get(1) and {
                parentSpanId = actual.spanId
                spanName.contains("should trace nested")
                events.isEmpty()
            }
            get(0) and {
                get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                spanName.isEqualTo("SPAN A")
                events and {
                    size.isEqualTo(1)
                    get(0) and { eventName.isEqualTo("event α") }
                }
            }
        }
    }

    @Slow @Test
    fun TestLogger.`should trace complex`() {
        logging("SPAN A") {
            span.event("event α")
            span.event("event β")
            logging("SPAN A.1") {
                span.event("event γ", "runtime" to 120.milli.seconds)
                span.event("event δ")
                span.event("event ε")
            }
            logging("SPAN A.2") {
                // no events
            }
            span.event("event ζ")
        }
        logging("SPAN B") {
            logging("SPAN B.1") {
                logging("SPAN B.1.1") {
                    // no events
                }
                logging("SPAN B.1.2") {
                    span.event("event η")
                    logging("SPAN B.1.2.1") {
                        span.event("event θ")
                    }
                }
                span.event("event ι")
            }
            logging("SPAN B.2") {
                span.event("event κ")
                span.event("event λ")
            }
        }
    }

    @Slow @Test
    fun TestLogger.`should trace exception`() {
        expectThrows<IllegalStateException> {
            logging("SPAN A") {
                span.event("event α")
                error("error occurred")
            }
        }

        expectThatTraced {
            all { hasValidIds() }
            size.isEqualTo(2)
            var parentSpanId: String? = null
            get(1) and {
                parentSpanId = actual.spanId
                spanName.contains("should trace nested")
                events.isEmpty()
            }
            get(0) and {
                get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                spanName.isEqualTo("SPAN A")
                events and {
                    size.isEqualTo(1)
                    get(0) and { eventName.isEqualTo("event α") }
                }
            }
        }
    }
}


val Builder<SpanData>.traceId: Builder<TraceId>
    get() = get("trace ID") { TraceId(traceId) }

fun Builder<TraceId>.isValid() =
    assert("is valid") {
        when (it.valid) {
            true -> pass()
            else -> fail("$it is not valid.")
        }
    }


val Builder<SpanData>.spanId: Builder<SpanId>
    get() = get("span ID") { SpanId(spanId) }

@JvmName("isValidSpanId")
fun Builder<SpanId>.isValid() =
    assert("is valid") {
        when (it.valid) {
            true -> pass()
            else -> fail("$it is not valid.")
        }
    }

fun Builder<SpanData>.hasValidIds() =
    compose("has valid IDs") {
        traceId.isValid()
        spanId.isValid()
    }.then { if (allPassed) pass() else fail() }


val Builder<SpanData>.spanName: Builder<String>
    get() = get("name") { name }

val Builder<SpanData>.events: Builder<List<EventData>>
    get() = get("events") { events }

val Builder<EventData>.eventName: Builder<String>
    get() = get("name") { name }

val Builder<EventData>.attributes: Builder<Attributes>
    get() = get("attributes") { attributes }

operator fun Builder<Attributes>.get(key: String): Builder<String> =
    get("key $key") { this.get(AttributeKey.stringKey(key)) }
