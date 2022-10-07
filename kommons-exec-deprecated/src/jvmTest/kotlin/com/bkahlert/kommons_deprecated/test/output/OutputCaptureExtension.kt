package com.bkahlert.kommons_deprecated.test.output

import com.bkahlert.kommons.test.junit.getStore
import com.bkahlert.kommons_deprecated.test.CapturedOutput
import com.bkahlert.kommons_deprecated.test.isAnnotated
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver

// TODO port
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
        try {
            context.pushCapture(!context.isAnnotated<Silent>())
            invocation.proceed()
        } finally {
            context.popCapture(!context.isAnnotated<Silent>())
        }
    }

    annotation class Silent

    companion object {
        private val ExtensionContext.store get() = getStore<OutputCaptureExtension>()
        private fun ExtensionContext.getOutputCapture(print: Boolean): OutputCapture =
            store.getOrComputeIfAbsent(
                OutputCapture::class.java,
                { OutputCapture(print) },
                OutputCapture::class.java
            )

        private fun ExtensionContext.pushCapture(print: Boolean): Unit = getOutputCapture(print).push()
        private fun ExtensionContext.popCapture(print: Boolean): Unit = getOutputCapture(print).pop()
        fun ExtensionContext.isCapturingOutput(): Boolean = getOutputCapture(false).isCapturing
    }
}