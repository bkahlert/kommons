package koodies.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method

class ExtensionContextResolver : InvocationInterceptor {

    companion object {
        lateinit var INSTANCE: ExtensionContextResolver
    }

    init {
        INSTANCE = this
    }

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
