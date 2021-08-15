package com.bkahlert.kommons.test.output

import com.bkahlert.kommons.runWrapping
import com.bkahlert.kommons.test.CapturedOutput
import com.bkahlert.kommons.test.isAnnotated
import com.bkahlert.kommons.test.storeForNamespace
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

class OutputCaptureExtension : TypeBasedParameterResolver<CapturedOutput>(),
    BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback,
    InvocationInterceptor {

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): CapturedOutput =
        context.getOutputCapture(!context.isAnnotated<Silent>())

    override fun beforeAll(context: ExtensionContext) {
        context.pushCapture(!context.isAnnotated<Silent>())
    }

    override fun afterAll(context: ExtensionContext) {
        context.popCapture(!context.isAnnotated<Silent>())
    }

    override fun beforeEach(context: ExtensionContext) {
        context.pushCapture(!context.isAnnotated<Silent>())
    }

    override fun afterEach(context: ExtensionContext) {
        context.popCapture(!context.isAnnotated<Silent>())
    }

    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        context: ExtensionContext,
    ) {
        invocation.runWrapping(
            before = { context.pushCapture(!context.isAnnotated<Silent>()) },
            block = { proceed() },
            after = { context.popCapture(!context.isAnnotated<Silent>()) },
        )
    }

    annotation class Silent

    companion object {
        private val store by storeForNamespace()
        private fun ExtensionContext.getOutputCapture(print: Boolean): OutputCapture =
            store().getOrComputeIfAbsent(
                OutputCapture::class.java,
                { OutputCapture(print) },
                OutputCapture::class.java)

        private fun ExtensionContext.pushCapture(print: Boolean): Unit = getOutputCapture(print).push()
        private fun ExtensionContext.popCapture(print: Boolean): Unit = getOutputCapture(print).pop()
        fun ExtensionContext.isCapturingOutput(): Boolean = getOutputCapture(false).isCapturing
    }
}
