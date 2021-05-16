@file:Suppress("ClassName")

package koodies.text

import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.withoutLeadingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator

/**
 * Returns true if this character sequence matches the given SLF4J / Logback style [curlyPattern], like `I {} you have to {}`.
 *
 * @sample Samples.singleLineMatches
 * @sample Samples.multiLineMatches
 */
public fun CharSequence.matchesCurlyPattern(curlyPattern: String, placeholder: String = "{}", multilinePlaceholder: String = "{{}}"): Boolean {
    val regex = curlyPattern.mapLines { it.mapToRegexPlaceholders(multilinePlaceholder, placeholder) }
        .replace("\\Q\\E", "")
        .supportMultiLinePlaceholderOnSeparateLines()
        .toRegex()
    return LineSeparators.unify(this).matches(regex)
}

private fun CharSequence.mapToRegexPlaceholders(multilinePlaceholder: String, placeholder: String) =
    split(multilinePlaceholder).joinToString(matchReallyAll) { it.protectAllButPlaceHolder(placeholder) }

private fun String.protectAllButPlaceHolder(placeholder: String) = split(placeholder).joinToString(".*") { Regex.escape(it) }

/**
 * A curly pattern of the form
 * ```
 * {{}}
 * text
 * {{}}
 * ```
 * would normally not match `text` because there is a line separator between the text
 * and the placeholder. Instead `{{}}text{{}}` (or just `text` in this example) would
 * have to be used.
 *
 * This method returns a modified pattern that allows the placeholder be also
 * placed on separate line.
 */
private fun String.supportMultiLinePlaceholderOnSeparateLines(): String {
    var pattern = this
    if (pattern.startsWith(matchReallyAll)) {
        pattern = pattern.withoutPrefix(matchReallyAll)
            .withoutLeadingLineSeparator
            .let { "$matchReallyAll$it" }
    }
    if (pattern.endsWith(matchReallyAll)) {
        pattern = pattern.withoutSuffix(matchReallyAll)
            .withoutTrailingLineSeparator
            .let { "$it$matchReallyAll" }
    }
    return pattern
}

/**
 * Not matches all like the dot `.`
 * but really all inclusive line separators.
 */
private val matchReallyAll = "[\\s\\S]*"


private object Samples {
    val singleLineMatches = "this is a test".matchesCurlyPattern("this is a {}")
    val multiLineMatches =
        """
        Executing [sh, -c, >&1 echo "test output"
        >&2 echo "test error"] in /Users/bkahlert/Development/com.imgcstmzr.
        Started Process(pid=72692, exitValue=0)
        Process(pid=72692, exitValue=0) stopped with exit code 0
        """.trimIndent()
            .matchesCurlyPattern(
                """
                Executing [sh, -c, >&1 echo "test output"
                >&2 echo "test error"] in {}
                Started Process(pid={}, exitValue={})
                Process(pid={}, exitValue={}) stopped with exit code {}
                """.trimIndent())
}
