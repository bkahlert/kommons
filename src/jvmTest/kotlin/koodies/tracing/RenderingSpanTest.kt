package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import koodies.exec.IOAttributes
import koodies.getOrException
import koodies.junit.TestName
import koodies.test.testEach
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import koodies.tracing.rendering.RenderableAttributes
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.Renderer.Companion.NOOP
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.rendering.RenderingAttributes
import koodies.tracing.rendering.Settings
import koodies.unit.nano
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.size
import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration

@NoTestSpan
class RenderingSpanTest {

    @TestFactory
    fun event(testName: TestName) = testEach<RenderingSpan.(String) -> Unit>(
        { event(Event.of(it)) },
        { event(it) },
        { addEvent(it) },
        { addEvent(it, 0L, SECONDS) },
        { addEvent(it, Instant.ofEpochSecond(0L)) },
        { addEvent(it, Attributes.empty()) },
        { addEvent(it, Attributes.empty(), 0L, SECONDS) },
        { addEvent(it, Attributes.empty(), Instant.ofEpochSecond(0L)) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpan(testName) { op("event name") }

        expecting("should record") { TestTelemetry[traceId] } that {
            all { isValid() }
            size.isEqualTo(1) and {
                get(0) and {
                    spanName.contains("RenderingSpanTest ➜ event")
                    events.hasSize(1) and {
                        get(0).eventName.isEqualTo("event name")
                    }
                }
            }
        }

        expecting("should render") { rendered } that {
            containsExactly("EVENT: event name; 0")
        }
    }

    @TestFactory
    fun exception(testName: TestName) = testEach<RenderingSpan.(Throwable) -> Unit>(
        { exception(it) },
        { recordException(it) },
        { recordException(it, Attributes.empty()) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpan(testName) { op(RuntimeException("exception message")) }

        expecting("should record") { TestTelemetry[traceId] } that {
            all { isValid() }
            size.isEqualTo(1) and {
                get(0) and {
                    spanName.contains("RenderingSpanTest ➜ exception")
                    events.hasSize(1) and {
                        get(0).eventName.isEqualTo("exception")
                    }
                }
            }
        }

        expecting("should render") { rendered } that {
            containsExactly("EXCEPTION: exception message; 0")
        }
    }

    @TestFactory
    fun end(testName: TestName) = testEach<RenderingSpan.() -> Unit>(
        { end() },
        { end(Result.success(Unit)) },
        { end(0L, SECONDS) },
        { end(Instant.ofEpochSecond(0L)) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpan(testName) { op() }

        expecting("should record") { TestTelemetry[traceId] } that {
            all { isValid() }
            size.isEqualTo(1) and {
                get(0) and {
                    spanName.contains("RenderingSpanTest ➜ end")
                    isOkay()
                    events.isEmpty()
                }
            }
        }

        expecting("should render") { rendered } that {
            containsExactly("END: (kotlin.Unit, null)")
        }
    }

    @TestFactory
    fun fail(testName: TestName) = testEach<RenderingSpan.() -> Unit>(
        { end(Result.failure<Unit>(RuntimeException("test"))) },
        { end(RuntimeException("test")) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpan(testName) { op() }

        expecting("should record") { TestTelemetry[traceId] } that {
            all { isValid() }
            size.isEqualTo(1) and {
                get(0) and {
                    spanName.contains("RenderingSpanTest ➜ fail")
                    isError("test")
                    events.isEmpty()
                }
            }
        }

        expecting("should render") { rendered } that {
            containsExactly("END: (null, java.lang.RuntimeException: test)")
        }
    }

    @Test
    fun `should override toString`() {
        expectThat(RenderingSpan(Span.getInvalid(), NOOP)).toStringMatchesCurlyPattern("RenderingSpan(span={}, renderer={})")
    }

    private fun withRenderingSpan(testName: TestName, block: RenderingSpan.() -> Unit): Pair<TraceId, List<String>> {
        val captured = mutableListOf<String>()
        val traceId = withRootSpan(testName) {
            RenderingSpan(Span.current(), CapturingRenderer(Settings(contentFormatter = { it.toString() }), captured)).block()
            TraceId.current
        }
        return traceId to captured
    }

    private fun <R> withRootSpan(testName: TestName, block: () -> R): R {
        val parentSpan = Tracer.spanBuilder(testName.value).startSpan().registerAsTestSpan()
        val scope = parentSpan.makeCurrent()
        val result = runCatching(block)
        scope.close()
        parentSpan.end()
        return result.getOrThrow()
    }

    private class CapturingRenderer(settings: Settings, private val captured: MutableList<String>) : Renderer {
        private val contentFormatter = settings.contentFormatter

        override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
            captured.add(contentFormatter("START").toString())
        }

        override fun event(name: CharSequence, attributes: RenderableAttributes) {
            captured.add(contentFormatter("EVENT: $name; ${attributes.size}").toString())
        }

        override fun exception(exception: Throwable, attributes: RenderableAttributes) {
            captured.add(contentFormatter("EXCEPTION: ${exception.message}; ${attributes.size}").toString())
        }

        override fun <R> end(result: Result<R>) {
            captured.add(contentFormatter("END: ${result.getOrException()}").toString())
        }

        override fun childRenderer(renderer: RendererProvider): Renderer =
            renderer(Settings(contentFormatter = contentFormatter, printer = ::printChild)) { CapturingRenderer(it, captured) }

        override fun printChild(text: CharSequence) {
            captured.add(text.toString())
        }
    }
}

internal fun Builder<out RenderingSpan>.isLinked() =
    assert("is linked") {
        when (it.rendererLinked) {
            true -> pass()
            else -> fail("is not linked")
        }
    }

internal fun Builder<out RenderingSpan>.isNotLinked() =
    assert("is not linked") {
        when (it.rendererLinked) {
            false -> pass()
            else -> fail("is linked")
        }
    }


val Builder<out Iterable<SpanData>>.spanNames: Builder<List<String>>
    get() = get("span names") { map { it.name } }

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

fun <T> Builder<SpanData>.hasSpanAttribute(key: AttributeKey<T>, value: T?): Builder<SpanData> =
    assert("has attribute $key=$value") {
        when (it.attributes.get(key)) {
            value -> pass()
            else -> fail()
        }
    }

val Builder<SpanData>.duration: Builder<Duration>
    get() = get("duration") { (endEpochNanos - startEpochNanos).nano.seconds }

val Builder<SpanData>.events: Builder<List<EventData>>
    get() = get("events") { events }

val Builder<EventData>.eventName: Builder<String>
    get() = get("name") { name }

val Builder<EventData>.attributes: Builder<Attributes>
    get() = get("attributes") { attributes }

val Builder<EventData>.eventDescription: Builder<String?>
    get() = get("description attribute") { attributes.get(RenderingAttributes.DESCRIPTION) }

val Builder<EventData>.eventText: Builder<String?>
    get() = get("text attribute") { attributes.get(IOAttributes.TEXT) }

fun Builder<EventData>.hasAttribute(key: String, value: String): Builder<EventData> =
    assert("has attribute $key=$value") {
        when (it.attributes.get(AttributeKey.stringKey(key))) {
            value -> pass()
            else -> fail()
        }
    }

val Builder<SpanData>.status: Builder<StatusData>
    get() = get("status") { status }

val Builder<SpanData>.statusCode: Builder<StatusCode>
    get() = get("code") { status.statusCode }

val Builder<SpanData>.statusDescription: Builder<String>
    get() = get("description") { status.description }

fun Builder<SpanData>.isOkay() =
    statusCode.isEqualTo(OK)

fun Builder<SpanData>.isError(expectedDescription: String) =
    with(this) {
        statusCode.isEqualTo(ERROR)
        statusDescription.isEqualTo(expectedDescription)
    }

operator fun Builder<Attributes>.get(key: String): Builder<String?> =
    get("key $key") { get(AttributeKey.stringKey(key)) }
