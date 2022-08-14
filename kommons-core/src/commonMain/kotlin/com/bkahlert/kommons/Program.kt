package com.bkahlert.kommons

/** The running program. */
public expect object Program {

    /** Whether this program is running in debug mode. */
    public val isDebugging: Boolean

    /** Registers the specified [handler] as to be called when this program is about to stop. */
    public fun onExit(handler: () -> Unit)
}
