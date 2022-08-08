package com.bkahlert.kommons.text

/**
 * [Regex] that matches only strings that contain no line separators, e.g. the last line of a multi-line text.
 */
public val LAST_LINE_REGEX: Regex = ".+$".toRegex()

/**
 * [Regex] that matches strings ending with a line separator.
 */
public val INTERMEDIARY_LINE_PATTERN: Regex by lazy {
    "${Regex.AnyCharacterRegex.pattern}*?(?<separator>${LineSeparators.CommonRegex.pattern})".toRegex()
}


public val LINE_PATTERN: Regex by lazy { "${INTERMEDIARY_LINE_PATTERN}|$LAST_LINE_REGEX".toRegex() }
