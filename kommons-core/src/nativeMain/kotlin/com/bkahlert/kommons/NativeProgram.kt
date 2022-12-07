package com.bkahlert.kommons

import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.atexit
import platform.posix.signal

/** The running program. */
public actual object Program {

    /** Whether this program is running in debug mode. */
    public actual val isDebugging: Boolean
        get() = false

    /** Registers the specified [handler] as a new virtual-machine shutdown hook. */
    public actual fun onExit(handler: () -> Unit): Unit {
        onExitHandlers.add(handler)
    }

    init {
        atexit(staticExitHandler)
        listOf(SIGINT, SIGTERM).forEach { signal(it, staticSignalHandler) }
    }
}

private val onExitHandlers = mutableListOf<() -> Unit>()
private fun invokeOnExitHandlers() = onExitHandlers.forEach { runCatching(it) }
private val staticExitHandler = staticCFunction<Unit> { invokeOnExitHandlers() }
private val staticSignalHandler = staticCFunction<Int, Unit> { invokeOnExitHandlers() }
