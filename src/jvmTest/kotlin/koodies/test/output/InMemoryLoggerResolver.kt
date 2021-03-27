package koodies.test.output

import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.Verbosity.Companion.isVerbose
import koodies.test.testName
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
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
                InMemoryLogger::class.java.isAssignableFrom(it) -> extensionContext.createLogger(
                    parameterContext = parameterContext,
                )
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> object : InMemoryLoggerFactory {
                    override fun createLogger(customSuffix: String, bordered: Boolean): InMemoryLogger =
                        extensionContext.createLogger(customSuffix, parameterContext, bordered)
                }
                else -> error("Unsupported $parameterContext")
            }
        }

    private fun ExtensionContext.createLogger(
        suffix: String? = null,
        parameterContext: ParameterContext,
        bordered: Boolean = parameterContext.findAnnotation(Bordered::class.java).map { it.value }.orElse(true),
    ): InMemoryLogger =
        object : InMemoryLogger(
            caption = testName + if (suffix != null) "::$suffix" else "",
            bordered = bordered,
            statusInformationColumn = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(-1),
            outputStreams = if (isVerbose || parameterContext.isVerbose) arrayOf(System.out) else emptyArray(),
        ) {
            @Suppress("UNCHECKED_CAST")
            override fun <R> logResult(block: () -> Result<R>): R =
                if (!closed) super.logResult(block) else Unit as R

            override fun toString(): String = toString(SUCCESSFUL_RETURN_VALUE, false)
        }.withUnclosedWarningDisabled.also { store().put(element, it) }

    override fun afterEach(extensionContext: ExtensionContext) {
        val logger: InMemoryLogger? = extensionContext.store().get(extensionContext.element, InMemoryLogger::class.java)
        if (logger != null) {
            val result =
                extensionContext.executionException.map { Result.failure<Any>(it) }.orElseGet { Result.success(Unit) }
            if (result.exceptionOrNull() is AssertionError) return
            kotlin.runCatching {
                logger.logResult { result }
            }
        }
    }

    private fun ExtensionContext.store(): ExtensionContext.Store = getStore(create(InMemoryLoggerResolver::class.java))
}
