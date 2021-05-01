package koodies.text

import koodies.collections.toLinkedMap
import koodies.text.LineSeparators.LAST_LINE_REGEX
import koodies.text.LineSeparators.REGEX
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * [Regex] that matches strings ending with a line separator.
 */
public val LineSeparators.INTERMEDIARY_LINE_PATTERN: Regex by lazy { ".*?(?<separator>$REGEX)".toRegex(DOT_MATCHES_ALL) }

/**
 * Mapping of line separators to their names.
 */
public val LineSeparators.Names: Map<String, String> by lazy<Map<String, String>> {
    listOf(
        LineSeparators.CRLF to "CARRIAGE RETURN + LINE FEED",
        LineSeparators.LF to "LINE FEED",
        LineSeparators.CR to "CARRIAGE RETURN",
        LineSeparators.LS to "LINE SEPARATOR",
        LineSeparators.PS to "PARAGRAPH SEPARATOR",
        LineSeparators.NEL to "NEXT LINE",
    ).toLinkedMap()
}

public val LineSeparators.LINE_PATTERN: Regex by lazy { "${LineSeparators.INTERMEDIARY_LINE_PATTERN}|$LAST_LINE_REGEX".toRegex() }
