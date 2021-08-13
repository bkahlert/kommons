package koodies.text

import koodies.collections.toLinkedMap
import koodies.io.path.appendText
import koodies.text.LineSeparators.LAST_LINE_REGEX
import koodies.text.LineSeparators.REGEX
import koodies.text.LineSeparators.autoDetect
import koodies.text.LineSeparators.hasTrailingLineSeparator
import java.nio.file.Path
import kotlin.io.path.readText
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
        LineSeparators.NEL to "NEXT LINE",
        LineSeparators.PS to "PARAGRAPH SEPARATOR",
        LineSeparators.LS to "LINE SEPARATOR",
    ).toLinkedMap()
}

public val LineSeparators.LINE_PATTERN: Regex by lazy { "${LineSeparators.INTERMEDIARY_LINE_PATTERN}|$LAST_LINE_REGEX".toRegex() }

/**
 * If this [Path] is a file of what the contents does not end with one of the [LineSeparators]
 * the given [lineSeparator] (default: [autoDetect]) is appended.
 */
public fun Path.appendTrailingLineSeparatorIfMissing(lineSeparator: String? = null): Path =
    also {
        val content = readText()
        if (!content.hasTrailingLineSeparator) appendText(lineSeparator ?: autoDetect(content))
    }
