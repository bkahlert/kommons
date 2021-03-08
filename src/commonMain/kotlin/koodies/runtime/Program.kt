package koodies.runtime

/**
 * A handler that is called when this program is about to stop.
 */
public typealias OnExitHandler = () -> Unit

/**
 * Useful information and functions concerning this program.
 */
public expect object Program {

    /**
     * Whether this program is running in debug mode.
     */
    public val isDebugging: Boolean

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    public fun <T : OnExitHandler> onExit(handler: T): T

    /**
     * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    public val ansiSupport: AnsiSupport
}

public val x: String = "dsds"

/**
 * Support level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public enum class AnsiSupport {
    /**
     * Denotes no support for ANSI escape codes.
     */
    NONE,

    /**
     * Denotes support for [4-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit).
     */
    ANSI4,

    /**
     * Denotes support for [8-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit).
     */
    ANSI8,

    /**
     * Denotes support for [24-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#24-bit).
     */
    ANSI24
}
