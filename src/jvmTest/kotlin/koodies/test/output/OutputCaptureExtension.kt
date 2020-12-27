package koodies.test.output

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class OutputCaptureExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.isAssignableFrom(CapturedOutput::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? =
        extensionContext.getOutputCapture()

    override fun beforeAll(context: ExtensionContext) {
        context.getOutputCapture().push()
    }

    override fun afterAll(context: ExtensionContext) {
        context.getOutputCapture().pop()
    }

    override fun beforeEach(context: ExtensionContext) {
        context.getOutputCapture().push()
    }

    override fun afterEach(context: ExtensionContext) {
        context.getOutputCapture().pop()
    }

    companion object {
        fun ExtensionContext.isCapturingOutput(): Boolean = getOutputCapture().isCapturing
        private fun ExtensionContext.getOutputCapture() = getStore<OutputCaptureExtension>().getSingleton<OutputCapture>()

        inline fun <reified T : Any> ExtensionContext.Store.getSingleton() =
            getOrComputeIfAbsent(T::class.java)

        inline fun <reified T : Any> ExtensionContext.getStore(): ExtensionContext.Store =
            getStore(ExtensionContext.Namespace.create(T::class))
    }
}
