package com.bkahlert.kommons.exec

/** I/O types of a [Process]. */
public enum class IOType(
    /** Short lower-case name of the I/O type. */
    public val shortName: String,
) {

    /** Standard output of a [Process]. */
    Output("out"),

    /** Standard error of a [Process]. */
    Error("err"),
}


/** I/O of a [Process]. */
public sealed interface IO {

    /** Bytes of this I/O unit. */
    public val bytes: ByteArray

    /** UTF-8 representation. */
    public val text: String get() = bytes.decodeToString()

    /** I/O produced by the standard output of a [Process]. */
    @JvmInline
    public value class Output(override val bytes: ByteArray) : IO

    /** I/O produced by the standard error of a [Process]. */
    @JvmInline
    public value class Error(override val bytes: ByteArray) : IO
}

// TODO test
