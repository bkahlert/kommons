package koodies.test.output

import io.opentelemetry.api.trace.Span
import koodies.collections.synchronizedMapOf
import koodies.io.ByteArrayOutputStream
import koodies.io.TeeOutputStream
import koodies.junit.TestName.Companion.testName
import koodies.junit.isVerbose
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.ReturnValue
import koodies.logging.SmartRenderingLogger
import koodies.logging.runLogging
import koodies.runtime.onExit
import koodies.test.executionResult
import koodies.test.storeForNamespace
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.styling.wrapWithBorder
import koodies.unit.bytes
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.OutputStream

/**
 * Annotated instances of [InMemoryLogger] are rendered border
 * depending on [value].
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Bordered(val value: Border)
class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.let {
            when {
                TestLogger::class.java.isAssignableFrom(it) -> true
                InMemoryLogger::class.java.isAssignableFrom(it) -> true
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> true
                else -> false
            }
        }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? =
        parameterContext.parameter.type.let {
            when {
                TestLogger::class.java.isAssignableFrom(it) -> TestLogging.testLoggerFor(extensionContext, parameterContext)
                InMemoryLogger::class.java.isAssignableFrom(it) -> TestLogging.loggerFor(extensionContext, parameterContext)
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> TestLogging.factoryFor(extensionContext, parameterContext)
                else -> error("Unsupported $parameterContext")
            }
        }

    override fun afterEach(extensionContext: ExtensionContext) {
        extensionContext.endSpanAndLogTestResult()
    }
}

object TestLogging {

    fun testLoggerFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): TestLogger =
        newLogger(extensionContext, parameterContext, extensionContext.testName, Border.NONE)

    fun loggerFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): TestLogger =
        newLogger(extensionContext, parameterContext, extensionContext.testName, null)

    fun factoryFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): InMemoryLoggerFactory =
        TestLoggerFactory(extensionContext, parameterContext)

    private val uniqueIdToOutputStreamMappings = synchronizedMapOf<String, ByteArrayOutputStream>()

    init {
        onExit {
            val count = uniqueIdToOutputStreamMappings.size
            val size = uniqueIdToOutputStreamMappings.values.sumOf { it.size() }.bytes
            println("$count tests logged a total of $size".wrapWithBorder())
        }
    }

    private fun newLogger(
        extensionContext: ExtensionContext,
        parameterContext: ParameterContext,
        name: String,
        border: Border?,
    ): TestLogger {
        val isVerbose = extensionContext.isVerbose || parameterContext.isVerbose
        val stored = ByteArrayOutputStream()
        uniqueIdToOutputStreamMappings[extensionContext.uniqueId] = stored
        val outputStream = if (isVerbose) TeeOutputStream(stored, System.out) else stored
        return TestLogger(
            extensionContext,
            parameterContext,
            name,
            border ?: parameterContext.findAnnotation(Bordered::class.java).map { it.value }.orElse(Border.DEFAULT),
            outputStream,
        )
    }

    class TestLoggerFactory(
        private val extensionContext: ExtensionContext,
        private val parameterContext: ParameterContext,
    ) : InMemoryLoggerFactory {
        override fun createLogger(customSuffix: String, border: Border?): InMemoryLogger =
            newLogger(extensionContext, parameterContext, "${extensionContext.testName}::$customSuffix", border)
    }
}

/**
 * A logger that—in contrast to [InMemoryLogger]—does not "beautify"
 * the logged content in any way.
 */
class TestLogger(
    private val extensionContext: ExtensionContext,
    parameterContext: ParameterContext,
    name: String,
    border: Border,
    outputStream: OutputStream?,
) : InMemoryLogger(
    name = name,
    parent = null,
    border = border,
    width = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(null),
    outputStream = outputStream,
) {
    init {
        withUnclosedWarningDisabled
        extensionContext.storeForNamespace<InMemoryLoggerResolver>().put(extensionContext.element, this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> logResult(result: Result<R>): R =
        if (!closed) {
            super.logResult(result)
        } else {
//            span.end(result)
            Unit as R
        }

    fun logTestResult(extensionContext: ExtensionContext) {
        check(this.extensionContext === extensionContext) {
            ::logTestResult.name + " must only be called after a test it is responsible for."
        }
        val result = extensionContext.executionResult
        if (result.exceptionOrNull() is AssertionError) return
        kotlin.runCatching { logResult(result) }
    }

    override fun toString(): String = toString(SUCCESSFUL_RETURN_VALUE, false)
}

/**
 * Ends the test [Span] and logs an eventually stored test result.
 */
fun ExtensionContext.endSpanAndLogTestResult() {
    storeForNamespace<InMemoryLoggerResolver>().get(element, TestLogger::class.java)?.logTestResult(this)
}

/**
 * Contains the logger currently stored in `this` [ExtensionContext]—which
 * is the case if an [InMemoryLogger] was requested by specifying
 * a test parameter of that type—or `null` otherwise.
 */
val ExtensionContext.testLocalLogger: InMemoryLogger? get() = storeForNamespace<InMemoryLoggerResolver>().get(element, TestLogger::class.java)

/**
 * Logs the given [block] in a new span with the active test logger if any.
 */
fun <R> ExtensionContext.logging(
    name: CharSequence,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = Border.DEFAULT,
    block: FixedWidthRenderingLogger.() -> R,
): R =
    SmartRenderingLogger(
        name,
        testLocalLogger,
        { (testLocalLogger ?: BACKGROUND).logText { it } },
        contentFormatter,
        decorationFormatter,
        returnValueFormatter,
        border,
        prefix = testLocalLogger?.prefix ?: BACKGROUND.prefix,
    ).runLogging(block)
