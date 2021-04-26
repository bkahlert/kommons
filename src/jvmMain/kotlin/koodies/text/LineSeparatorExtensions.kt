package koodies.text

import koodies.collections.toLinkedMap
import koodies.text.LineSeparators.LAST_LINE_PATTERN
import koodies.text.LineSeparators.SEPARATOR_PATTERN
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * [Regex] that matches strings ending with a line separator.
 */
public val LineSeparators.INTERMEDIARY_LINE_PATTERN: Regex by lazy { ".*?(?<separator>$SEPARATOR_PATTERN)".toRegex(DOT_MATCHES_ALL) }

/**
 * [Regex] that matches text lines, that is strings that either finish with a line separator or dont' contain any line separator at all.
 */
public val LineSeparators.Dict: Map<String, String> by lazy<Map<String, String>> {
    listOf(
        "CARRIAGE RETURN + LINE FEED" to LineSeparators.CRLF,
        "LINE FEED" to LineSeparators.LF,
        "CARRIAGE RETURN" to LineSeparators.CR,
        "LINE SEPARATOR" to LineSeparators.LS,
        "PARAGRAPH SEPARATOR" to LineSeparators.PS,
        "NEXT LINE" to LineSeparators.NEL,
    ).toLinkedMap()
}

public val LineSeparators.LINE_PATTERN: Regex by lazy { "${LineSeparators.INTERMEDIARY_LINE_PATTERN}|$LAST_LINE_PATTERN".toRegex() }
