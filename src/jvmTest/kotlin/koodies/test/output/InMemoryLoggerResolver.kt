package koodies.test.output

import koodies.logging.InMemoryLogger
import koodies.test.Verbosity.Companion.isVerbose
import koodies.test.testName
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean = parameterContext.parameter.type.let {
        when {
            InMemoryLogger::class.java.isAssignableFrom(it) -> true
            InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> true
            else -> false
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? = parameterContext.parameter.type.let {
        when {
            InMemoryLogger::class.java.isAssignableFrom(it) -> extensionContext.createLogger(borderedOutput = true, parameterContext = parameterContext)
            InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> object : InMemoryLoggerFactory {
                override fun createLogger(customSuffix: String, borderedOutput: Boolean): InMemoryLogger =
                    extensionContext.createLogger(customSuffix, borderedOutput, parameterContext)
            }
            else -> error("Unsupported $parameterContext")
        }
    }

    private fun ExtensionContext.createLogger(suffix: String? = null, borderedOutput: Boolean, parameterContext: ParameterContext): InMemoryLogger =
        object : InMemoryLogger(
            caption = testName + if (suffix != null) "::$suffix" else "",
            borderedOutput = borderedOutput,
            statusInformationColumn = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(-1),
            outputStreams = if (isVerbose || parameterContext.isVerbose) listOf(System.out) else emptyList(),
        ) {
            override fun <R> logResult(block: () -> Result<R>): R {
                @Suppress("UNCHECKED_CAST")
                return if (!resultLogged) {
                    super.logResult(block).also { resultLogged = true }
                } else Unit as R
            }
        }.also { store().put(element, it) }

    override fun afterEach(extensionContext: ExtensionContext) {
        val logger: InMemoryLogger? = extensionContext.store().get(extensionContext.element, InMemoryLogger::class.java)
        if (logger != null) {
            val result = extensionContext.executionException.map { Result.failure<Any>(it) }.orElseGet { Result.success(Unit) }
            if (result.exceptionOrNull() is AssertionError) return
            kotlin.runCatching {
                logger.logResult { result }
            }
        }
    }

    private fun ExtensionContext.store(): ExtensionContext.Store = getStore(create(InMemoryLoggerResolver::class.java))
}
