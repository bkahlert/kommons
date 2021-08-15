package com.bkahlert.kommons.tracing

import io.opentelemetry.api.trace.Span
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.math.floorDiv
import com.bkahlert.kommons.runtime.currentThread
import com.bkahlert.kommons.runtime.orNull
import com.bkahlert.kommons.test.get
import com.bkahlert.kommons.test.isAnnotated
import com.bkahlert.kommons.test.junit.TestName.Companion.testName
import com.bkahlert.kommons.test.junit.Verbosity
import com.bkahlert.kommons.test.put
import com.bkahlert.kommons.test.storeForNamespaceAndTest
import com.bkahlert.kommons.text.ANSI.Colors
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.padStartFixedLength
import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.time.minutes
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.tracing.TestPrinter.TestIO
import com.bkahlert.kommons.tracing.rendering.BackgroundPrinter
import com.bkahlert.kommons.tracing.rendering.CompactRenderer
import com.bkahlert.kommons.tracing.rendering.InMemoryPrinter
import com.bkahlert.kommons.tracing.rendering.Printer
import com.bkahlert.kommons.tracing.rendering.Renderable
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes
import com.bkahlert.kommons.tracing.rendering.Renderer
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes
import com.bkahlert.kommons.tracing.rendering.Settings
import com.bkahlert.kommons.tracing.rendering.TeePrinter
import com.bkahlert.kommons.tracing.rendering.ThreadSafePrinter
import com.bkahlert.kommons.unit.milli
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.opentest4j.AssertionFailedError
import org.opentest4j.IncompleteExecutionException
import org.opentest4j.MultipleFailuresError
import org.opentest4j.TestAbortedException
import org.opentest4j.TestSkippedException
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import java.util.concurrent.locks.ReentrantLock
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.concurrent.withLock
import kotlin.time.Duration

class TestSpanScope(
    span: SpanScope,
    private val rendered: (ignoreAnsi: Boolean) -> String,
) : SpanScope by span {

    /**
     * Returns a [Builder] to run assertions on what was rendered.
     */
    fun expectThatRendered(ignoreAnsi: Boolean = true) =
        expectThat(rendered(ignoreAnsi))

    /**
     * Runs the specified [assertions] on what was rendered.
     */
    fun expectThatRendered(ignoreAnsi: Boolean = true, assertions: Builder<String>.() -> Unit) =
        expectThat(rendered(ignoreAnsi), assertions)
}

/**
 * Suppresses the provision of a [TestSpanScope].
 */
@Target(FUNCTION, CLASS)
annotation class NoTestSpan

/**
 * Resolves a new [TestSpanScope] with rendering capabilities.
 *
 * @see TestTelemetry
 */
class TestSpanParameterResolver : TypeBasedParameterResolver<TestSpanScope>(), BeforeEachCallback, AfterEachCallback {
    private val store: ExtensionContext.() -> Store by storeForNamespaceAndTest()

    private data class CleanUp(val job: () -> Unit)

    override fun beforeEach(extensionContext: ExtensionContext) {
        if (extensionContext.isAnnotated<NoTestSpan>()) return
        val name = extensionContext.testName
        val printToConsole = Verbosity.isVerbose
        val rendered = InMemoryPrinter()
        val spanScope: RenderingSpanScope = RenderingSpanScope.of(name) { TestRenderer(rendered, printToConsole) }
        val scope = spanScope.makeCurrent()
        spanScope.registerAsTestSpan()
        extensionContext.store().put(CleanUp {
            scope.close()
            spanScope.end(extensionContext.executionException.orNull()?.let { Result.failure(it) } ?: Result.success(Unit))
        })
        extensionContext.store().put(TestSpanScope(spanScope) { ignoreAnsi: Boolean ->
            rendered.toString().lineSequence()
                .filterNot { it.startsWith(IO.ERASE_MARKER) }
                .joinLinesToString()
                .let { if (ignoreAnsi) it.ansiRemoved else it }
        })
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): TestSpanScope {
        return extensionContext.store().get<TestSpanScope>()
            ?: if (extensionContext.isAnnotated<NoTestSpan>()) error("Unable to resolve $TestSpanScopeString due to existing $NoSpanString") else error("Failed to load $TestSpanScopeString")
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        extensionContext.store().get<CleanUp>()?.run { job() }
    }

    companion object {
        private val tracesLock = ReentrantLock()
        private val traces = mutableSetOf<TraceId>()

        /** Register this span as a test span that was created on purpose. */
        fun Span.registerAsTestSpan(): Span = apply { tracesLock.withLock { traces.add(traceId) } }

        /** Register this span scope as a test span scope that was created on purpose. */
        fun SpanScope.registerAsTestSpan(): SpanScope = apply { tracesLock.withLock { traces.add(TraceId.current) } }
        val TraceId.testTrace get() = tracesLock.withLock { traces.contains(this) }

        private val NoSpanString = NoTestSpan::class.simpleName.formattedAs.input
        private val TestSpanScopeString = TestSpanScope::class.simpleName.formattedAs.input
    }
}


/**
 * Renders a [TestSpanScope] by passing all events to a [TestPrinter].
 * An event stream filtered to the ones actually triggered by the client
 * are passed to the given [consoleAndUpstreamPrinter].
 */
class TestRenderer(
    upstreamPrinter: Printer,
    printToConsole: Boolean,
) : Renderer {

    private lateinit var backgroundPrinterBackup: Printer
    private val consolePrinter: Printer = if (printToConsole) ThreadSafePrinter(TestPrinter()) else run { {} }
    private val consoleAndUpstreamPrinter: Printer = ThreadSafePrinter(TeePrinter(consolePrinter, upstreamPrinter))

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        backgroundPrinterBackup = BackgroundPrinter.printer
        BackgroundPrinter.printer = consoleAndUpstreamPrinter
        consolePrinter(TestIO.Start(traceId, spanId, name))
    }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        attributes[RenderingAttributes.DESCRIPTION]?.render(null, null)?.let(consoleAndUpstreamPrinter)
            ?: consoleAndUpstreamPrinter("$name: $attributes")
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        consoleAndUpstreamPrinter("$exception: $attributes")
    }

    override fun <R> end(result: Result<R>) {
        when (val exception = result.exceptionOrNull()) {
            null -> consolePrinter(TestIO.Pass)
            else -> consolePrinter(TestIO.Fail(exception))
        }
        BackgroundPrinter.printer = backgroundPrinterBackup
    }

    override fun childRenderer(renderer: RendererProvider): Renderer =
        renderer(Settings(printer = ::printChild)) { CompactRenderer(it) }

    override fun printChild(text: CharSequence) {
        consoleAndUpstreamPrinter(text)
    }
}


/**
 * Printer that prepends each line with runtime information.
 */
class TestPrinter : Printer {

    private val start: Long by lazy { Now.millis }

    private val breakPoints = mutableListOf(
        100.milli.seconds,
        120.milli.seconds,
        130.milli.seconds,
        200.milli.seconds,
        500.milli.seconds,
        1.seconds,
        5.seconds,
        10.seconds,
        30.seconds,
        1.minutes,
        2.minutes,
        5.minutes,
        10.minutes,
        15.minutes,
        30.minutes,
        45.minutes,
        60.minutes,
    )

    override fun invoke(text: CharSequence) {
        val timePassed = Now.passedSince(start)
        when (text) {
            is TestIO.Start -> {
                println()
                print("TraceID ".meta)
                print(text.traceId.ansi.gray)
                print("   ")
                println(text)
                println(headerLine)
            }
            is TestIO.Pass, is TestIO.Fail -> {
                println(footerLine)
                print(resultPrefix)
                print(timePassed.toString().padStartFixedLength(7).formattedAs.meta)
                print("   ")
                println(text)
            }
            else -> {
                val thread = currentThread.name.padStartFixedLength(31)
                val time = timePassed.format()
                val prefix = "$thread  $time │ ".meta
                text.toAnsiString().lineSequence().forEach { println("$prefix$it") }
            }
        }
    }

    private fun Duration.format(): CharSequence {
        val formatted = toString().padStartFixedLength(7)
        return if (breakPoints.takeWhile { it < this }.also { breakPoints.removeAll(it) }.isNotEmpty()) {
            formatted.ansi.gray.bold
        } else {
            formatted
        }
    }

    sealed class TestIO(private val string: String) : CharSequence by string {
        class Start(val traceId: TraceId, val spanId: SpanId, name: CharSequence) : TestIO(Renderable.of(name).render(COLUMNS, 1))
        object Pass : TestIO("Pass".formattedAs.success)
        class Fail(val exception: Throwable) : TestIO(
            when (exception) {
                is AssertionFailedError -> "Fail"
                is IncompleteExecutionException -> "Incomplete"
                is MultipleFailuresError -> "Fail (Multi)"
                is TestAbortedException -> "Abort"
                is TestSkippedException -> "Skip"
                else -> "Crash"
            }.formattedAs.failure
        )

        override fun toString(): String = string
    }

    private companion object {
        private const val COLUMNS = 80

        private val formatter = Formatter<CharSequence> { it.ansi.color(Colors.gray(.45)) }
        val CharSequence.meta: CharSequence get() = formatter.invoke(this)
        val headerLine = StringBuilder().apply {
            append("─".repeat((COLUMNS floorDiv 2) + 1))
            append("┬".repeat(1))
            append("─".repeat(COLUMNS + 1))
        }.meta
        val footerLine = StringBuilder().apply {
            append("─".repeat((COLUMNS floorDiv 2) + 1))
            append("┴".repeat(1))
            append("─".repeat(COLUMNS + 1))
        }.meta
        val resultPrefix = StringBuilder().apply {
            append(" ".repeat(31))
            append(" ".repeat(2))
        }.meta
    }
}
