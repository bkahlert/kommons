package koodies.debug

import koodies.junit.isVerbose
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Parameter

/**
 * If a test is running because it is annotated with [Debug] (instead of being ignored by [DebugCondition]
 * can be obtained using this [ParameterResolver].
 *
 * The [Parameter] must be assignable by [Boolean].
 *
 * `true` will also be provided if the test is the only one in the current [TestPlan].
 */
class DebugParameterResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.isAssignableFrom(Boolean::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? =
        extensionContext.isVerbose || parameterContext.isVerbose
}
