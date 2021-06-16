package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import koodies.test.actual
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.string
import koodies.test.testEach
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import koodies.text.isMultiLine
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.Span.State.Initialized
import koodies.tracing.Span.State.Started
import koodies.tracing.rendering.Renderer
import koodies.unit.nano
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNull
import strikt.assertions.length
import strikt.assertions.none
import strikt.assertions.size
import strikt.assertions.startsWith
import java.time.Instant
import kotlin.time.Duration

class OpenTelemetrySpanTest {

    @Nested
    inner class Parent {

        @Test
        fun `should be root with no parent`() {
            expecting { OpenTelemetrySpan("root") } that { isRoot() }
        }

        @Test
        fun `should not be root with parent`() {
            val root = OpenTelemetrySpan("root")
            expecting { OpenTelemetrySpan("child", root) } that { hasParent(root) }
        }
    }

    @Nested
    inner class State {

        @TestFactory
        fun `should be initialized`() = testLoggers({ this }) { state.isA<Initialized>() }

        @TestFactory
        fun `should be started on start`() = testLoggers({ apply { start() } }) { state.isA<Started>() }

        @Test
        fun `should start parent on start`() {
            val parent = OpenTelemetrySpan("parent")
            val child = OpenTelemetrySpan("child", parent)
            child.start()
            expectThat(parent) {
                state.isA<Started>() and {
                    timestamp.isEqualTo(child.state.timestamp)
                }
            }
        }

        @TestFactory
        fun `should be ended on immediate end`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should be ended on delayed end`() = testLoggers({ apply { apply { start() }.end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should be succeeded on success`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should be failed on failure`() = testLoggers({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }

        @TestFactory
        fun `should return same span id on multiple ends`() = testLoggers({ apply { start() }.run { end() to end() } }) { first.isEqualTo(actual.second) }

        @TestFactory
        fun `should keep result of first end`() = testLoggers({
            apply {
                end(Result.success(1), Instant.ofEpochMilli(0L))
                end(Result.success(2), Instant.ofEpochMilli(10L))
            }
        }) {
            state.isA<Succeeded>() and {
                timestamp.isEqualTo(Instant.ofEpochMilli(0L))
                value.isEqualTo(1)
            }
        }
    }

    @Nested
    inner class Tracing {

        @TestFactory
        fun `should have no trace id if not started`() = testLoggers({ traceId }) { isNull() }

        @TestFactory
        fun `should have trace id if started`() = testLoggers({ apply { start() }.traceId }) { string.length.isGreaterThanOrEqualTo(32) }

        @TestFactory
        fun `should have no span id if not started`() = testLoggers({ spanId }) { isNull() }

        @TestFactory
        fun `should have span id if started`() = testLoggers({ apply { start() }.spanId }) { string.length.isGreaterThanOrEqualTo(16) }

        @TestFactory
        fun `should end successfully`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should end failed`() = testLoggers({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }


        @Test
        fun Span.`should trace`() {
            endAndExpect {
                all { isValid() }
                all { isOkay() }
                size.isEqualTo(1) and {
                    get(0) and {
                        spanName.contains("should trace")
                        events.isEmpty()
                    }
                }
            }
        }

        @Test
        fun Span.`should record event`() {
            event("event name")

            endAndExpect {
                all { isValid() }
                all { isOkay() }
                size.isEqualTo(1) and {
                    get(0) and {
                        spanName.contains("should record event")
                        events and {
                            size.isEqualTo(1)
                            get(0) and { eventName.isEqualTo("event name") }
                        }
                    }
                }
            }
        }

        @Test
        fun Span.`should record exception`() {
            exception(RuntimeException("exception message"))

            endAndExpect {
                all { isValid() }
                all { isOkay() }
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

        @Test
        fun Span.`should trace nested`() {
            spanning("SPAN A") { event("event α") }

            endAndExpect {
                all { isValid() }
                all { isOkay() }
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

        @Test
        fun Span.`should trace exception`() {
            @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE")
            (expectThrows<IllegalStateException> {
                spanning("SPAN A") {
                    event("event α")
                    spanning("SPAN A.1") {
                        error("error occurred")
                    }
                }
            })

            endAndExpect {
                size.isEqualTo(3)
                var parentSpanId: String? = null
                get(2) and {
                    parentSpanId = actual.spanId
                    spanName.contains("should trace exception")
                    events.isEmpty()
                    isOkay()
                }
                get(1) and {
                    get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                    parentSpanId = actual.spanId
                    spanName.isEqualTo("SPAN A")
                    events and {
                        size.isEqualTo(1)
                        get(0) and { eventName.isEqualTo("event α") }
                    }
                    isError("error occurred")
                }
                get(0) and {
                    get { parentSpanContext.spanId }.isEqualTo(parentSpanId)
                    spanName.isEqualTo("SPAN A.1")
                    events.isEmpty()
                    isError("error occurred")
                }
            }
        }
    }

    @Nested
    inner class Rendering {

        private fun capture(block: OpenTelemetrySpan.() -> Unit): List<String> {
            val rendered = mutableListOf<String>()
            val span = OpenTelemetrySpan("root", renderer = object : Renderer {
                override fun start(name: CharSequence, started: Started) {
                    rendered.add("START: $name; $started")
                }

                override fun event(name: CharSequence, description: CharSequence, attributes: Map<CharSequence, CharSequence>) {
                    rendered.add("EVENT: $name; $description; $attributes")
                }

                override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>) {
                    rendered.add("EXCEPTION: ${exception.message}; $attributes")
                }

                override fun spanning(name: CharSequence): Renderer = this

                override fun end(ended: Ended) {
                    rendered.add("END: $ended")
                }
            })

            span.block()
            return rendered
        }

        @Test
        fun `should render start`() {
            expecting { capture { event("custom-event") } } that {
                first().matchesCurlyPattern("START: root; Started({})")
            }
        }

        @Test
        fun `should render event`() {
            expecting { capture { event("custom-event", "custom description", "custom-attribute" to "custom attribute") } } that {
                get(1).isEqualTo("EVENT: custom-event; custom description; {custom-attribute=custom attribute}")
            }
        }

        @Test
        fun `should not render event without description`() {
            expecting { capture { event("custom-event", "custom-attribute" to "custom attribute") } } that {
                none { startsWith("EVENT") }
            }
        }

        @Test
        fun `should render log`() {
            expecting { capture { log("custom description", "custom-attribute" to "custom attribute") } } that {
                get(1).isEqualTo("EVENT: custom-description; custom description; {custom-attribute=custom attribute}")
            }
        }

        @Test
        fun `should render exception`() {
            expecting { capture { exception(RuntimeException("custom message"), "custom-attribute" to "custom attribute") } } that {
                get(1).isEqualTo("EXCEPTION: custom message; {custom-attribute=custom attribute}")
            }
        }

        @Test
        fun `should render end`() {
            expecting { capture { end("result") } } that {
                get(1).matchesCurlyPattern("END: Succeeded(value=result, {})")
            }
        }
    }

    @Nested
    inner class ToString {

        @TestFactory
        fun `should must not open`() = testLoggers({ apply { toString() } }) { state.isA<Initialized>() }

        @Test
        fun `should contain name`() {
            expecting { OpenTelemetrySpan("tracer") } that { toStringContains("name = tracer") }
        }

        @TestFactory
        fun `should contain parent`() = testEach(
            OpenTelemetrySpan("root") to "parent = null",
            OpenTelemetrySpan("parent").let { OpenTelemetrySpan("child", it) to "parent = $it" },
        ) { (tracer, expected) ->
            expecting { tracer } that { toStringContains(expected.ansiRemoved) }
        }

        @Test
        fun `should contain state`() {
            expecting { OpenTelemetrySpan("tracer") } that { toStringMatchesCurlyPattern("{} state = Initialized({}) {}") }
        }

        @Test
        fun `should have trace id`() {
            expecting { OpenTelemetrySpan("tracer").apply { start() } } that { toStringMatchesCurlyPattern("{} traceId = {}") }
        }

        @Test
        fun `should have span id`() {
            expecting { OpenTelemetrySpan("tracer").apply { start() } } that { toStringMatchesCurlyPattern("{} spanId = {}") }
        }
    }

    private fun <R> testLoggers(action: OpenTelemetrySpan.() -> R, assertions: Builder<R>.() -> Unit): List<DynamicContainer> =
        testEach(
            OpenTelemetrySpan("root"),
            OpenTelemetrySpan("child", OpenTelemetrySpan("root")),
            OpenTelemetrySpan("child", OpenTelemetrySpan("parent", OpenTelemetrySpan("root"))),
        ) {
            expecting { action() } that { assertions() }
        }
}

val Builder<out Span>.name
    get() = get("name") { name }

fun <T : Span> Builder<T>.isRoot() =
    assert("is root") {
        when (it.isRoot) {
            true -> pass()
            else -> fail("has parent ${it.parent}")
        }
    }

fun <T : Span> Builder<T>.hasParent(span: Span) =
    assert("has parent $span") {
        when (it.parent) {
            span -> pass()
            null -> fail("has no parent / is root")
            else -> fail("has parent ${it.parent}")
        }
    }

val Builder<out Span>.state
    get() = get("state") { state }

val Builder<out Span.State>.timestamp
    get() = get("timestamp") { timestamp }

val Builder<out Succeeded>.value
    get() = get("value") { value }

val Builder<out Failed>.exception
    get() = get("exception") { exception }


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

@JvmName("isValidSpanData")
fun Builder<SpanData>.isValid() =
    compose("has valid IDs") {
        traceId.isValid()
        spanId.isValid()
        duration.isGreaterThanOrEqualTo(Duration.ZERO)
    }.then { if (allPassed) pass() else fail() }

val Builder<SpanData>.spanName: Builder<String>
    get() = get("name") { name }

val Builder<SpanData>.duration: Builder<Duration>
    get() = get("duration") { (endEpochNanos - startEpochNanos).nano.seconds }

val Builder<SpanData>.events: Builder<List<EventData>>
    get() = get("events") { events }

val Builder<EventData>.eventName: Builder<String>
    get() = get("name") { name }

val Builder<EventData>.attributes: Builder<Attributes>
    get() = get("attributes") { attributes }

val Builder<SpanData>.status: Builder<StatusData>
    get() = get("status") { status }

val Builder<StatusData>.code: Builder<StatusCode>
    get() = get("code") { statusCode }

val Builder<StatusData>.description: Builder<String>
    get() = get("description") { description }

fun Builder<SpanData>.isOkay() =
    status.code.isEqualTo(OK)

fun Builder<SpanData>.isError(expectedDescription: String) =
    with(status) {
        code.isEqualTo(ERROR)
        description.isEqualTo(expectedDescription)
    }

operator fun Builder<Attributes>.get(key: String): Builder<String> =
    get("key $key") { this.get(AttributeKey.stringKey(key)) }
