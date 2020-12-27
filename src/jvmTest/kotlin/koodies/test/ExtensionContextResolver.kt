package koodies.test

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import java.lang.reflect.Method

class ExtensionContextResolver : TypeBasedParameterResolver<ExtensionContext>(), InvocationInterceptor {

    companion object {
        lateinit var INSTANCE: ExtensionContextResolver
    }

    init {
        INSTANCE = this
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): ExtensionContext =
        extensionContext

    private var _context: ThreadLocal<ExtensionContext?> = ThreadLocal()
    var context: ExtensionContext?
        get() = _context.get()
        set(value) = _context.set(value)

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.also { context = extensionContext }.proceed().also { context = null }
    }

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        invocation.also { context = extensionContext }.proceed().also { context = null }
    }

    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        extensionContext: ExtensionContext,
    ) {
        invocation.also { context = extensionContext }.proceed().also { context = null }
    }
}

val extensionContext get() = ExtensionContextResolver.INSTANCE.context
