package com.bkahlert.kommons

import com.bkahlert.kommons.text.AnsiSupport

/** Platforms a program can run on. */
public expect enum class Platform {

    /** Browser platform */
    Browser,

    /** NodeJS platform */
    NodeJS,

    /** Java virtual machine */
    JVM;

    public companion object {
        /** The platform this program runs on. */
        public val Current: Platform
    }

    /** Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). */
    public val ansiSupport: AnsiSupport

    /** The separator used to separate path segments. */
    public val fileSeparator: String
}
