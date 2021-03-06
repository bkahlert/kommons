@file:Suppress("ClassName")

package koodies.text

import koodies.regex.group

/**
 * Matches SLF4J / Logback style [curlyPattern] with this char sequence and returns a map of matches.
 *
 * Results consist of what was matches using a named placeholder (e.g. `{my custom name}`).
 */
public fun <T : CharSequence> T.match(curlyPattern: String, placeholder: String = "{}"): List<String> {
    val regex = Regex(curlyPattern.split(placeholder).joinToString(Regex.group(".*")) { Regex.escape(it) })
    return regex.find(this)?.groupValues?.drop(1) ?: emptyList()
}
