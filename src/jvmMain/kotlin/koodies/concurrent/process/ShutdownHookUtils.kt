package koodies.concurrent.process

import java.security.AccessControlException
import kotlin.concurrent.thread

public object ShutdownHookUtils {

    public fun <T : () -> Unit> addShutDownHook(hook: T): T = hook.also { addShutDownHook(thread(start = false) { hook() }) }
    public fun addShutDownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().addShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    public fun removeShutdownHook(hook: Thread): Any =
        kotlin.runCatching { Runtime.getRuntime().removeShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }

    private fun Throwable.rethrowIfUnexpected(): Any = if (this !is IllegalStateException && this !is AccessControlException) throw this else Unit
}
