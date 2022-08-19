package org.springframework.boot.test.system

import com.bkahlert.logging.logback.CapturedOutput2
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedOutput
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.util.ReflectionUtils

/**
 * An extended version of Spring Boot's [OutputCaptureExtension] that allows
 * not only [CapturedOutput] but [SmartCapturedOutput] to be
 * injected in a test.
 *
 * @author Björn Kahlert
 * @see SmartCapturedOutput
 */
class SmartOutputCaptureExtension() : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver, OutputCaptureExtension() {
    val outputCaptureExtension: OutputCaptureExtension = ReflectionUtils.getDeclaredConstructor(OutputCaptureExtension::class.java).run {
        isAccessible = true
        newInstance() as OutputCaptureExtension
    }

    override fun beforeAll(context: ExtensionContext?) = outputCaptureExtension.beforeAll(context)
    override fun afterAll(context: ExtensionContext?) = outputCaptureExtension.afterAll(context)
    override fun beforeEach(context: ExtensionContext?) = outputCaptureExtension.beforeEach(context)
    override fun afterEach(context: ExtensionContext?) = outputCaptureExtension.afterEach(context)

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Boolean =
        CapturedOutput::class.java.isAssignableFrom(parameterContext.parameter.type) ||
            SmartCapturedLog::class.java.isAssignableFrom(parameterContext.parameter.type)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Any? {
        return if (SmartCapturedLog::class.java.isAssignableFrom(parameterContext.parameter.type)) {
            SmartCapturedLog.loggingFromNow()
        } else outputCaptureExtension.resolveParameter(parameterContext, extensionContext)
            ?.takeIf { CapturedOutput::class.java.isInstance(it) }
            ?.let { CapturedOutput::class.java.cast(it) }
            ?.let {
                if (parameterContext.parameter.type == CapturedOutput::class.java) {
                    it
                } else if (parameterContext.parameter.type == CapturedOutput2::class.java) {
                    object : CapturedOutput2, CapturedOutput by it {
                        override fun toString(): String {
                            return it.toString()
                        }
                    }
                } else {
                    SmartCapturedOutput.enrich(it)
                }
            }
    }
}
