package com.bkahlert.kommons

import com.bkahlert.kommons.debug.StackTrace
import com.bkahlert.kommons.debug.get
import com.bkahlert.kommons.text.AnsiSupport

/** Platforms a program can run on. */
public actual enum class Platform {

    /** Browser platform */
    Browser,

    /** NodeJS platform */
    NodeJS,

    /** Java virtual machine */
    JVM;

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
        if (StackTrace.get().firstNotNullOf { it.file }.contains('\\')) "\\" else "/"
    }
}
