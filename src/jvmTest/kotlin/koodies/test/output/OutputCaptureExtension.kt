package koodies.test.output

import koodies.debug.CapturedOutput
import koodies.runWrapping
import koodies.test.storeForNamespace
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

class OutputCaptureExtension :
    TypeBasedParameterResolver<CapturedOutput>(),
    BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback,
    InvocationInterceptor {


    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): CapturedOutput =
        context.outputCapture

    override fun beforeAll(context: ExtensionContext) {
        context.pushCapture()
    }

    override fun afterAll(context: ExtensionContext) {
        context.popCapture()
    }

    override fun beforeEach(context: ExtensionContext) {
        context.pushCapture()
    }

    override fun afterEach(context: ExtensionContext) {
        context.popCapture()
    }

    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        context: ExtensionContext,
    ) {
        invocation.runWrapping(
            before = { context.pushCapture() },
            block = { proceed() },
            after = { context.popCapture() },
        )
    }

    companion object {
        fun ExtensionContext.isCapturingOutput(): Boolean = outputCapture.isCapturing

        private inline fun <reified T : Any> ExtensionContext.Store.getSingleton(): T =
            getOrComputeIfAbsent(T::class.java)

        private val ExtensionContext.outputCapture get() = storeForNamespace<OutputCaptureExtension>().getSingleton<OutputCapture>()
        private fun ExtensionContext.pushCapture() = outputCapture.push()
        private fun ExtensionContext.popCapture() = outputCapture.pop()
    }
}
