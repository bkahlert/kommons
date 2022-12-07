package com.bkahlert.kommons

/** Platforms a program can run on. */
public expect enum class Platform {

    /** Browser platform */
    Browser,

    /** NodeJS platform */
    NodeJS,

    /** Java virtual machine */
    JVM,

    /** Native platform */
    Native,
    ;

    public companion object {
        /** The platform this program runs on. */
        public val Current: Platform
    }

    /** Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). */
    public val ansiSupport: AnsiSupport

    /** The separator used to separate path segments. */
    public val fileSeparator: String
}

/** Support level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). */
public enum class AnsiSupport {
    /** Denotes no support for ANSI escape codes. */
    NONE,

    /** Denotes support for [4-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit). */
    ANSI4,

    /** Denotes support for [8-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit). */
    ANSI8,

    /** Denotes support for [24-Bit ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code#24-bit). */
    ANSI24
}
