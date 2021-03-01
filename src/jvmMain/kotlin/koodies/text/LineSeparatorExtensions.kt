package koodies.text

import koodies.collections.toLinkedMap

/**
 * [Regex] that matches strings ending with a line separator.
 */
val LineSeparators.INTERMEDIARY_LINE_PATTERN: Regex by lazy { ".*?(?<separator>${LineSeparators.SEPARATOR_PATTERN.pattern})".toRegex(RegexOption.DOT_MATCHES_ALL) }

/**
 * [Regex] that matches text lines, that is strings that either finish with a line separator or dont' contain any line separator at all.
 */
val LineSeparators.Dict: Map<String, String> by lazy<Map<String, String>> {
    listOf(
        "CARRIAGE RETURN + LINE FEED" to LineSeparators.CRLF,
        "LINE FEED" to LineSeparators.LF,
        "CARRIAGE RETURN" to LineSeparators.CR,
        "LINE SEPARATOR" to LineSeparators.LS,
        "PARAGRAPH SEPARATOR" to LineSeparators.PS,
        "NEXT LINE" to LineSeparators.NEL,
    ).toLinkedMap()
}

val LineSeparators.LINE_PATTERN: Regex by lazy { "${LineSeparators.INTERMEDIARY_LINE_PATTERN.pattern}|${LineSeparators.LAST_LINE_PATTERN.pattern}".toRegex() }
