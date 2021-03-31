package koodies.test.output

import koodies.logging.BlockRenderingLogger
import koodies.logging.BorderedRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLoggingDsl
import koodies.logging.SmartRenderingLogger
import koodies.logging.runLogging
import koodies.test.Verbosity.Companion.isVerbose
import koodies.test.testName
import koodies.text.ANSI.Formatter
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.let {
            when {
                InMemoryLogger::class.java.isAssignableFrom(it) -> true
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> true
                else -> false
            }
        }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? =
        parameterContext.parameter.type.let {
            when {
                InMemoryLogger::class.java.isAssignableFrom(it) -> TestLogger.single(extensionContext, parameterContext)
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> TestLogger.factory(extensionContext, parameterContext)
                else -> error("Unsupported $parameterContext")
            }
        }

    override fun afterEach(extensionContext: ExtensionContext) {
        extensionContext.logTestResult()
    }
}


class TestLogger(
    private val extensionContext: ExtensionContext,
    parameterContext: ParameterContext,
    suffix: String? = null,
    bordered: Boolean = parameterContext.findAnnotation(Bordered::class.java).map { it.value }.orElse(true),
) : InMemoryLogger(
    caption = extensionContext.testName + if (suffix != null) "::$suffix" else "",
    bordered = bordered,
    width = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(null),
    outputStreams = if (extensionContext.isVerbose || parameterContext.isVerbose) arrayOf(System.out) else emptyArray(),
) {
    init {
        withUnclosedWarningDisabled
        saveTo(extensionContext)
    }

    /**
     * Stores a reference to `this` logger in the given [extensionContext].
     */
    private fun TestLogger.saveTo(extensionContext: ExtensionContext): TestLogger =
        also { extensionContext.store().put(extensionContext.element, this) }

    companion object {
        fun single(extensionContext: ExtensionContext, parameterContext: ParameterContext): TestLogger =
            TestLogger(extensionContext, parameterContext)

        fun factory(extensionContext: ExtensionContext, parameterContext: ParameterContext): InMemoryLoggerFactory =
            object : InMemoryLoggerFactory {
                override fun createLogger(customSuffix: String, bordered: Boolean): InMemoryLogger =
                    TestLogger(extensionContext, parameterContext, customSuffix, bordered)
            }
    }

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
    bordered: Boolean = BlockRenderingLogger.BORDERED_BY_DEFAULT,
    block: BorderedRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption, logger, contentFormatter, decorationFormatter, bordered).runLogging(block)
