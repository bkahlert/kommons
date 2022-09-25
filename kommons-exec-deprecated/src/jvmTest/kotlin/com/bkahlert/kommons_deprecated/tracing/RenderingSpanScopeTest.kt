package com.bkahlert.kommons_deprecated.tracing

import com.bkahlert.kommons_deprecated.exec.IOAttributes
import com.bkahlert.kommons.getOrException
import com.bkahlert.kommons.test.junit.DisplayName
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons_deprecated.tracing.TestSpanParameterResolver.Companion.registerAsTestSpan
import com.bkahlert.kommons_deprecated.tracing.rendering.RenderableAttributes
import com.bkahlert.kommons_deprecated.tracing.rendering.Renderer
import com.bkahlert.kommons_deprecated.tracing.rendering.Renderer.Companion.NOOP
import com.bkahlert.kommons_deprecated.tracing.rendering.RendererProvider
import com.bkahlert.kommons_deprecated.tracing.rendering.RenderingAttributes
import com.bkahlert.kommons_deprecated.tracing.rendering.Settings
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

@NoTestSpan
class RenderingSpanScopeTest {

    @TestFactory
    fun event(displayName: DisplayName) = testEach<RenderingSpanScope.(String) -> Unit>(
        { event(Event.of(it)) },
        { event(it) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpanScope(displayName) { op("event name") }

        TestTelemetry[traceId] should {
            it.forAll {
                TraceId(it.traceId).valid shouldBe true
                SpanId(it.spanId).valid shouldBe true
                it.duration.isPositive() shouldBe true
            }
            it.forAny {
                it.name shouldContain "RenderingSpanScopeTest ➜ event"
                it.events.forAny { it.name shouldBe "event name" }

            }
        }

        rendered shouldContain "EVENT: event name; 0"
    }

    @TestFactory
    fun exception(displayName: DisplayName) = testEach<RenderingSpanScope.(Throwable) -> Unit>(
        { exception(it) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpanScope(displayName) { op(RuntimeException("exception message")) }

        TestTelemetry[traceId] should {
            it.forAll {
                TraceId(it.traceId).valid shouldBe true
                SpanId(it.spanId).valid shouldBe true
                it.duration.isPositive() shouldBe true
            }
            it.forAny {
                it.name shouldContain "RenderingSpanScopeTest ➜ exception"
                it.events.forAny { it.name shouldBe "exception" }

            }
        }

        rendered.shouldContainExactly("EXCEPTION: exception message; 0")
    }

    @TestFactory
    fun end(displayName: DisplayName) = testEach<RenderingSpanScope.() -> Unit>(
        { end() },
        { end(Result.success(Unit)) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpanScope(displayName, invokeEnd = false) { op() }

        TestTelemetry[traceId] should {
            it.forAll {
                TraceId(it.traceId).valid shouldBe true
                SpanId(it.spanId).valid shouldBe true
                it.duration.isPositive() shouldBe true
            }
            it.forAny {
                it.name shouldContain "RenderingSpanScopeTest ➜ end"
                it.status.statusCode shouldBe OK

            }
        }

        rendered.shouldContainExactly("END: (kotlin.Unit, null)")
    }

    @TestFactory
    fun fail(displayName: DisplayName) = testEach<RenderingSpanScope.() -> Unit>(
        { end(Result.failure<Unit>(RuntimeException("test"))) },
    ) { op ->
        val (traceId, rendered) = withRenderingSpanScope(displayName, invokeEnd = false) { op() }

        TestTelemetry[traceId] should {
            it.forAll {
                TraceId(it.traceId).valid shouldBe true
                SpanId(it.spanId).valid shouldBe true
                it.duration.isPositive() shouldBe true
            }
            it.forAny {
                it.name shouldContain "RenderingSpanScopeTest ➜ fail"
                it.status.statusCode shouldBe ERROR
                it.status.description shouldBe "test"
            }
        }

        rendered.shouldContainExactly("END: (null, java.lang.RuntimeException: test)")
    }

    @Test
    fun `should override toString`() {
        RenderingSpanScope(Span.getInvalid(), NOOP).toString() shouldMatchGlob "RenderingSpanScope(span=*, renderer=*)"
    }

    private fun withRenderingSpanScope(displayName: DisplayName, invokeEnd: Boolean = true, block: RenderingSpanScope.() -> Unit): Pair<TraceId, List<String>> {
        val captured = mutableListOf<String>()
        val traceId = withRootSpanScope(displayName, invokeEnd) {
            RenderingSpanScope(Span.current(), CapturingRenderer(Settings(contentFormatter = { it.toString() }), captured)).block()
            TraceId.current
        }
        return traceId to captured
    }

    private fun <R> withRootSpanScope(displayName: DisplayName, invokeEnd: Boolean = true, block: () -> R): R {
        val parentSpanScope = RenderingSpanScope.of(displayName.composedDisplayName) { it }
        val scope = parentSpanScope.makeCurrent()
        parentSpanScope.registerAsTestSpan()
        val result = runCatching(block)
        scope.close()
        if (invokeEnd) parentSpanScope.end()
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

val SpanData.duration get() = (endEpochNanos - startEpochNanos).nanoseconds

val Builder<SpanData>.duration: Builder<Duration>
    get() = get("duration") { (endEpochNanos - startEpochNanos).nanoseconds }

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

fun Builder<SpanData>.isOkay() =
    statusCode.isEqualTo(OK)

operator fun Builder<Attributes>.get(key: String): Builder<String?> =
    get("key $key") { get(AttributeKey.stringKey(key)) }
