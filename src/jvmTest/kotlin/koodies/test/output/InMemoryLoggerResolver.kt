package koodies.test.output

import koodies.collections.synchronizedMapOf
import koodies.io.ByteArrayOutputStream
import koodies.io.TeeOutputStream
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLoggingDsl
import koodies.logging.ReturnValue
import koodies.logging.SmartRenderingLogger
import koodies.logging.runLogging
import koodies.runtime.Program
import koodies.test.Verbosity.Companion.isVerbose
import koodies.test.testName
import koodies.text.ANSI.Formatter
import koodies.text.styling.wrapWithBorder
import koodies.unit.bytes
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ExtensionContext.Store
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
        extensionContext.logTestResult()
    }
}

object TestLogging {
    fun testLoggerFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): TestLogger =
        newLogger(extensionContext, parameterContext, extensionContext.testName, Border.NONE)

    fun loggerFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): TestLogger =
        newLogger(extensionContext, parameterContext, extensionContext.testName, null)

    fun factoryFor(extensionContext: ExtensionContext, parameterContext: ParameterContext): InMemoryLoggerFactory =
        TestLoggerFactory(extensionContext, parameterContext)


    private val streams = synchronizedMapOf<String, ByteArrayOutputStream>()

    init {
        Program.onExit {
            val count = streams.size
            val size = streams.values.sumBy { it.size() }.bytes
            println("$count tests logged a total of $size".wrapWithBorder())
        }
    }

    private fun newLogger(
        extensionContext: ExtensionContext,
        parameterContext: ParameterContext,
        caption: String,
        border: Border?,
    ): TestLogger {
        val isVerbose = extensionContext.isVerbose || parameterContext.isVerbose
        val stored = ByteArrayOutputStream()
        streams[extensionContext.uniqueId] = stored
        val outputStream = if (isVerbose) TeeOutputStream(stored, System.out) else stored
        return TestLogger(
            extensionContext,
            parameterContext,
            caption,
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

class TestLogger(
    private val extensionContext: ExtensionContext,
    parameterContext: ParameterContext,
    suffix: String?,
    border: Border,
    outputStream: OutputStream?,
) : InMemoryLogger(
    caption = extensionContext.testName + if (suffix != null) "::$suffix" else "",
    border = border,
    width = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(null),
    outputStream = outputStream,
) {
    init {
        withUnclosedWarningDisabled
        saveTo(extensionContext)
    }

    /**
     * Stores a reference to `this` logger in the given [extensionContext].
     */
    private fun saveTo(extensionContext: ExtensionContext): TestLogger =
        also { extensionContext.store().put(extensionContext.element, this) }

    @Suppress("UNCHECKED_CAST")
    override fun <R> logResult(block: () -> Result<R>): R =
        if (!closed) super.logResult(block) else Unit as R

    fun logTestResult(extensionContext: ExtensionContext) {
        check(this.extensionContext === extensionContext) {
            ::logTestResult.name + " must only be called after a test it is responsible for."
        }
        val result = this.extensionContext.executionException.map { Result.failure<Any>(it) }.orElseGet { Result.success(Unit) }
        if (result.exceptionOrNull() is AssertionError) return
        kotlin.runCatching { logResult { result } }
    }

    override fun toString(): String = toString(SUCCESSFUL_RETURN_VALUE, false)
}

/**
 * Returns a [ExtensionContext.Store] with a [InMemoryLoggerResolver] namespace.
 */
private fun ExtensionContext.store(): Store = getStore(create(InMemoryLoggerResolver::class.java))
fun ExtensionContext.logTestResult() {
    store().get(element, TestLogger::class.java)?.logTestResult(this)
}

/**
 * Contains the current logger currently stored in `this` [ExtensionContext] or `null` otherwise.
 */
public val ExtensionContext.logger: InMemoryLogger? get() = store().get(element, TestLogger::class.java)

/**
 * Logs the given [block] in a new span with the active test logger if any.
 */
@RenderingLoggingDsl
public fun <R> ExtensionContext.logging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = Border.DEFAULT,
    block: FixedWidthRenderingLogger.() -> R,
): R =
    SmartRenderingLogger(
        caption,
        { (logger ?: BACKGROUND).logText { it } },
        contentFormatter,
        decorationFormatter,
        returnValueFormatter,
        border,
        prefix = logger?.prefix ?: BACKGROUND.prefix,
    ).runLogging(block)
