package com.bkahlert.kommons.test.logging

@JvmInline
value class PrintedLogEntry private constructor(
    val log: String,
) : CharSequence by log {
    constructor(lines: List<String>) : this(lines.joinToString("\n").removeSuffix("\n"))

    fun lines(count: Int) = log.lineSequence().take(count).joinToString("\n")
    override fun toString(): String = log
}
