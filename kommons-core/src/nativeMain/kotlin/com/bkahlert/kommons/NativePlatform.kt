package com.bkahlert.kommons

import com.bkahlert.kommons.AnsiSupport.ANSI24

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
        public actual val Current: Platform = Native
    }

    /** Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). */
    public actual val ansiSupport: AnsiSupport = ANSI24

    /** The separator used to separate path segments. */
    public actual val fileSeparator: String = "/"
}
