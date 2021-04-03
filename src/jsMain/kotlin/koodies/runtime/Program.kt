package koodies.runtime

import koodies.runtime.AnsiSupport.NONE

public actual object Program {

    /**
     * Whether this program is running an integrated development environment.
     */
    public actual val isDeveloping: Boolean
        get() = TODO("Not yet implemented")

    /**
     * Whether this program is running in debug mode.
     */
    public actual val isDebugging: Boolean
        get() = TODO("Not yet implemented")

    /**
     * Whether this program is running in test mode.
     */
    public actual val isTesting: Boolean by lazy { isDebugging }

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    public actual fun <T : OnExitHandler> onExit(handler: T): T {
        TODO("Not yet implemented")
    }

    /**
     * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    public actual val ansiSupport: AnsiSupport = NONE
}
