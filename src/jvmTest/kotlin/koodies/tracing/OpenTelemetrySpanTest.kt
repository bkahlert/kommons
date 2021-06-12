package koodies.tracing

import koodies.test.expecting
import koodies.test.string
import koodies.test.testEach
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.Span.State.Initializing
import koodies.tracing.Span.State.Started
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.length
import java.time.Instant

class OpenTelemetrySpanTest {

    private fun OpenTelemetrySpan.start(): OpenTelemetrySpan = apply { traceId }

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
        fun `should be initializing`() = testLoggers({ this }) { state.isA<Initializing>() }

        @TestFactory
        fun `should be started when span is accessed`() = testLoggers({ start() }) { state.isA<Started>() }

        @TestFactory
        fun `should be ended on immediate end`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should be ended on delayed end`() = testLoggers({ apply { start().end(Result.success(true)) } }) { state.isA<Ended>() }

        @TestFactory
        fun `should be succeeded on success`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should be failed on failure`() = testLoggers({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }

        @TestFactory
        fun `should not throw on multiple ends`() = testLoggers({ apply { start().end(Result.success(true)) } }) { }

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
        fun `should have trace id`() = testLoggers({ traceId }) { string.length.isGreaterThanOrEqualTo(32) }

        @TestFactory
        fun `should have span id`() = testLoggers({ spanId }) { string.length.isGreaterThanOrEqualTo(16) }

        @TestFactory
        fun `should end successfully`() = testLoggers({ apply { end(Result.success(true)) } }) { state.isA<Succeeded>() }

        @TestFactory
        fun `should end failed`() = testLoggers({ apply { end<Any?>(Result.failure(RuntimeException())) } }) { state.isA<Failed>() }
    }

    @Nested
    inner class ToString {

        @TestFactory
        fun `should must not open`() = testLoggers({ apply { toString() } }) { state.isA<Initializing>() }

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
            expecting { OpenTelemetrySpan("tracer") } that { toStringMatchesCurlyPattern("{} state = Initializing({}) {}") }
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
