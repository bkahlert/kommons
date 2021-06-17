package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.extension.annotations.WithSpan
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import koodies.debug.CapturedOutput
import koodies.test.actual
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.output.OutputCaptureExtension
import koodies.test.string
import koodies.test.testEach
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import koodies.text.isMultiLine
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.toBaseName
import koodies.tracing.OpenTelemetrySpan.Companion.linked
import koodies.tracing.Span.State
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.Span.State.Initialized
import koodies.tracing.Span.State.Started
import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.Settings
import koodies.unit.nano
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
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
import strikt.assertions.size
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
        fun `should be initialized`() = testSpans({ this }) { state.isA<Initialized>() }

        @TestFactory
        fun `should not be linked when initialized`() = testSpans({ this }) { isNotLinked() }

        @TestFactory
        fun `should be started on start`() = testSpans({ apply { start() } }) { state.isA<Started>() }

        @TestFactory
        fun `should be linked on start`() = testSpans({ apply { start() } }) { isLinked() }

        @Test
        fun `should start parent on start`() {
            val parent = OpenTelemetrySpan("parent", Tracer.NOOP)
            val child = OpenTelemetrySpan("child", parent)
            child.start()
            expectThat(parent) {
                state.isA<Started>() and {
                    timestamp.isEqualTo(child.state.timestamp)
                }
            }
        }

        @TestFactory
        fun `should be ended on immediate end`() = testSpans({ apply { end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should no more be linked on immediate end`() = testSpans({ apply { end(Result.success(true)) } }) { isNotLinked() }

        @TestFactory
        fun `should be ended on delayed end`() = testSpans({ apply { apply { start() }.end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should no more be linked on delayed end`() = testSpans({ apply { apply { start() }.end(Result.success(true)) } }) { isNotLinked() }

        @TestFactory
        fun `should be succeeded on success`() = testSpans({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should no more be linked on success`() = testSpans({ apply { end(Result.success(true)) } }) { isNotLinked() }

        @TestFactory
        fun `should be failed on failure`() = testSpans({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }

        @TestFactory
        fun `should no more be linked on failure`() = testSpans({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { isNotLinked() }

        @TestFactory
        fun `should return same span id on multiple ends`() = testSpans({ apply { start() }.run { end() to end() } }) { first.isEqualTo(actual.second) }

        @TestFactory
        fun `should keep result of first end`() = testSpans({
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
        fun `should have no trace id if not started`() = testSpans({ traceId }) { isNull() }

        @TestFactory
        fun `should have trace id if started`() = testSpans({ apply { start() }.traceId }) { string.length.isGreaterThanOrEqualTo(32) }

        @TestFactory
        fun `should have no span id if not started`() = testSpans({ spanId }) { isNull() }

        @TestFactory
        fun `should have span id if started`() = testSpans({ apply { start() }.spanId }) { string.length.isGreaterThanOrEqualTo(16) }

        @TestFactory
        fun `should end successfully`() = testSpans({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should end failed`() = testSpans({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }


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
        fun Span.`should trace exception`() {
            @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE")
            expectThrows<IllegalStateException> {
                OpenTelemetrySpan.spanning("SPAN A") {
                    event("event α")
                    OpenTelemetrySpan.spanning("SPAN A.1") {
                        error("error occurred")
                    }
                }
            }

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

        @Test
        fun Span.`should trace spanning`() {
            OpenTelemetrySpan.spanning("SPAN A") { event("event α") }

            endAndExpect {
                all { isValid() }
                all { isOkay() }
                size.isEqualTo(2)
                var parentSpanId: String? = null
                get(1) and {
                    parentSpanId = actual.spanId
                    spanName.contains("should trace spanning")
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

    @Isolated
    @Nested
    inner class Spanning {

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
                OpenTelemetrySpan.spanning("indirectly nested") {
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

            @Test @WithSpan
            fun `should create nested span on already active native span`() {
                OpenTelemetrySpan.spanning("indirectly nested") {
                    log("event α")
                }
                endCurrentAndExpect {
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
    inner class Rendering {

        private inner class CapturingRenderer(settings: Settings, private val captured: MutableList<String>) : Renderer {
            private val contentFormatter = settings.contentFormatter

            override fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant) {
                captured.add(contentFormatter("START").toString())
            }

            override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
                captured.add(contentFormatter("EVENT: $name; $attributes").toString())
            }

            override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
                captured.add(contentFormatter("EXCEPTION: ${exception.message}; $attributes").toString())
            }

            override fun end(ended: Ended) {
                captured.add(contentFormatter("END: $ended").toString())
            }

            override fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings): Renderer {
                return CapturingRenderer(Settings(contentFormatter = contentFormatter).run(customize), captured)
            }

            override fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer {
                return provider(Settings(contentFormatter = contentFormatter)) { captured.add(it.toString()) }
            }
        }

        private fun capture(block: OpenTelemetrySpan.() -> Unit): List<String> {
            val captured = mutableListOf<String>()
            OpenTelemetrySpan("root", renderer = CapturingRenderer(Settings(contentFormatter = { it }), captured), tracer = Tracer.NOOP).block()
            return captured
        }

        @Test
        fun `should render start`() {
            expecting { capture { event("custom-event") } } that {
                first().matchesCurlyPattern("START")
            }
        }

        @Test
        fun `should render event`() {
            expecting { capture { event("custom-event", "custom description", "custom-attribute" to "custom attribute") } } that {
                get(1).isEqualTo("EVENT: custom-event; {description=custom description, custom-attribute=custom attribute}")
            }
        }

        @Test
        fun `should render log`() {
            expecting { capture { log("custom description", "custom-attribute" to "custom attribute") } } that {
                get(1).isEqualTo("EVENT: custom-description; {description=custom description, custom-attribute=custom attribute}")
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

        private val initializedSpan = OpenTelemetrySpan("init-span", tracer = Tracer.NOOP)
        private val startedSpan = OpenTelemetrySpan("started-span", tracer = Tracer.NOOP).apply { start() }

        @TestFactory
        fun `should must not open`() = testSpans({ apply { toString() } }) { state.isA<Initialized>() }

        @Test
        fun `should contain name`() {
            expecting { initializedSpan } that { toStringContains("name = init-span") }
        }

        @TestFactory
        fun `should contain parent`() = testEach(
            OpenTelemetrySpan("root", tracer = Tracer.NOOP) to "parent = null",
            OpenTelemetrySpan("parent", tracer = Tracer.NOOP).let { OpenTelemetrySpan("child", it) to "parent = $it" },
        ) { (tracer, expected) ->
            expecting { tracer } that { toStringContains(expected.ansiRemoved) }
        }

        @Test
        fun `should contain state`() {
            expecting { initializedSpan } that { toStringMatchesCurlyPattern("{} state = Initialized({}) {}") }
        }

        @Test
        fun `should have trace id`() {
            expecting { startedSpan } that { toStringMatchesCurlyPattern("{} traceId = {}") }
        }

        @Test
        fun `should have span id`() {
            expecting { startedSpan } that { toStringMatchesCurlyPattern("{} spanId = {}") }
        }
    }

    private fun <R> testSpans(action: OpenTelemetrySpan.() -> R, assertions: Builder<R>.() -> Unit): List<DynamicContainer> =
        testEach(
            OpenTelemetrySpan("root", tracer = Tracer.NOOP),
            OpenTelemetrySpan("child", OpenTelemetrySpan("root", tracer = Tracer.NOOP)),
            OpenTelemetrySpan("child", OpenTelemetrySpan("parent", OpenTelemetrySpan("root", tracer = Tracer.NOOP))),
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

fun Builder<out OpenTelemetrySpan>.isLinked() =
    assert("is linked") {
        when (it.linked) {
            true -> pass()
            else -> fail("is not linked")
        }
    }

fun Builder<out OpenTelemetrySpan>.isNotLinked() =
    assert("is not linked") {
        when (it.linked) {
            false -> pass()
            else -> fail("is linked")
        }
    }

val Builder<out State>.timestamp
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
