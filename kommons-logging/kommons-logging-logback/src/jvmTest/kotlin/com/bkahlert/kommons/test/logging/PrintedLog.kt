package com.bkahlert.kommons.test.logging

import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LogLevel.OFF

@JvmInline
value class PrintedLog private constructor(private val entries: List<PrintedLogEntry>) : List<PrintedLogEntry> by entries {
    constructor(
        content: CharSequence,
        entryStartMarker: List<String> = buildList {
            LogLevel.values().filterNot { it == OFF }.forEach { add(it.name) }
            add(" --- ")
            add(" TestLogger ")
        },
    ) : this(sequence {
        val logLines = mutableListOf<String>()
        content.lineSequence().forEach { line ->
            if (entryStartMarker.any { line.contains(it) }) {
                if (logLines.isNotEmpty()) yield(PrintedLogEntry(logLines))
                logLines.clear()
            }
            logLines.add(line)
        }
        if (logLines.isNotEmpty()) yield(PrintedLogEntry(logLines))
    }.filterNot { it.isEmpty() }.toList())

    override fun toString(): String = joinToString("\n")
}
