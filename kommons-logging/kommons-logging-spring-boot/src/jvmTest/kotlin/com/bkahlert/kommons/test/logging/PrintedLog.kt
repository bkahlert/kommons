package com.bkahlert.kommons.test.logging

@JvmInline
value class PrintedLog private constructor(private val entries: List<PrintedLogEntry>) : List<PrintedLogEntry> by entries {
    constructor(
        content: CharSequence,
        isLogEntryStart: (CharSequence) -> Boolean = DefaultEntryStartMatcher,
    ) : this(sequence {
        val logLines = mutableListOf<String>()
        content.lineSequence().forEach { line ->
            if (isLogEntryStart(line)) {
                if (logLines.isNotEmpty()) yield(PrintedLogEntry(logLines))
                logLines.clear()
            }
            logLines.add(line)
        }
        if (logLines.isNotEmpty()) yield(PrintedLogEntry(logLines))
    }.filterNot { it.isEmpty() }.toList())

    override fun toString(): String = joinToString("\n")

    companion object {

        /**
         * Returns a new [PrintedLog] instance that contains the log entries
         * in the moment of invocation of the specified [content].
         */
        fun from(content: CharSequence): PrintedLog = PrintedLog(content)

        /** Predicate that returns `true` if the start of a [PrintedLogEntry] is detected, or `false` otherwise. */
        val DefaultEntryStartMatcher: (CharSequence) -> Boolean = {
            springPresetRegex.matches(it) || minimalPresetRegex.matches(it) || jsonPresetRegex.matches(it)
        }

        /** Regular expression that matches log lines that were like created with [LoggingPreset.Spring] applied. */
        val springPresetRegex = Regex(
            """
            ^\d{4}-\d{2}-\d{2}.*
            """.trimIndent()
        )

        /** Regular expression that matches log lines that were like created with [LoggingPreset.Minimal] applied. */
        val minimalPresetRegex = Regex(
            """
            ^\d{2}:\d{2}.\d{3}.*
            """.trimIndent()
        )

        /** Regular expression that matches log lines that were like created with [LoggingPreset.Json] applied. */
        val jsonPresetRegex = Regex(
            """
            ^\{"@timestamp":.*
            """.trimIndent()
        )
    }
}
