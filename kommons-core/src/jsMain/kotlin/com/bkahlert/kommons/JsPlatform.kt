package com.bkahlert.kommons

/** Platforms a program can run on. */
public actual enum class Platform {

    /** Browser platform */
    Browser,

    /** NodeJS platform */
    NodeJS,

    /** Java virtual machine */
    JVM,

    /** Native platform */
    Native,
    ;

    public actual companion object {
        /** The platform this program runs on. */
        public actual val Current: Platform by lazy {
            runCatching { kotlinx.browser.window }.fold({ Browser }, { NodeJS })
        }
    }

    /** Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). */
    public actual val ansiSupport: AnsiSupport = AnsiSupport.NONE

    /** The separator used to separate path segments. */
    public actual val fileSeparator: String by lazy {
        val lineWithFileSeparator = try {
            throw RuntimeException()
        } catch (ex: Throwable) {
            ex.stackTraceToString().removeSuffix("\n")
        }.lineSequence().first { it.contains("/") || it.contains('\\') }
        if (lineWithFileSeparator.contains('\\')) "\\" else "/"
    }
}
