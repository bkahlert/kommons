package koodies.regex

public object RegularExpressions {
    public val atLeastOneWhitespaceRegex: Regex = Regex("\\s+")
    public val urlRegex: Regex = Regex("(?<schema>https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    public val uriRegex: Regex = Regex("\\w+:(?:/?/?)[^\\s]+")

    public val versionRegex: Regex = Regex("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)")

    public val camelCaseRegex: Regex = Regex("(?<lowerLeftChar>[a-z0-9]|(?=[A-Z]))(?<upperRightChar>[A-Z])")
    public val screamingSnakeCaseRegex: Regex = Regex("(?<leftChar>[A-Z0-9]?)_(?<rightChar>[A-Z0-9])")
    public val kebabCaseRegex: Regex = Regex("(?<leftChar>[a-z0-9]?)-(?<rightChar>[a-z0-9])")
}

/**
 * Returns a pattern that optionally matches this [Regex].
 *
 * Example: `abc` becomes `(?:abc)?`
 */
public fun Regex.optional(): String = "(?:$pattern)?"

/**
 * Returns a non capturing group with no name matching [pattern].
 *
 * This method is useful in situations where expressions should be grouped together without modifying the group order.
 */
public fun Regex.Companion.anonymousGroup(pattern: String): String = "(?:$pattern)"

/**
 * Returns a capturing group with no name matching [pattern].
 */
public fun Regex.Companion.group(pattern: String): String = "($pattern)"

/**
 * Returns a group with a [name] matching [pattern].
 *
 * Once matches the matching group can be retrieved using [MatchNamedGroupCollection] starting with Kotlin 1.1.
 * Otherwise [MatchResult.values] can be used.
 */
public fun Regex.Companion.namedGroup(name: String, pattern: String): String = "(?<$name>$pattern)"

/**
 * Returns all values that matched the groups with corresponding [groupNames].
 *
 * E.g. if `(?<a>...)(?<b>...)` matched, `result.values(listOf("b"))` returns a map with the [Map.Entry] `b` and its matched value.
 *
 * Important: In Kotlin 1.0 group names are not supported but in order to still work [groupNames] is expected to contain all used group names in the order as their appear in the regular expression (e.g. `listOf("a", "b")`).
 */
public fun MatchResult.values(groupNames: List<String>): Map<String, String?> =
    (groups as? MatchNamedGroupCollection)
        ?.let { groupNames.map { name -> name to it[name]?.value }.toMap() }
        ?: groupNames.mapIndexed { index, name -> name to groups[index + 1]?.value }.toMap()

/**
 * Provides access to [MatchResult.groups] as [MatchNamedGroupCollection] in order to access a [MatchGroup] by its name.
 */
public val MatchResult.namedGroups: MatchNamedGroupCollection
    get() = groups as MatchNamedGroupCollection

/**
 * Returns the matched [MatchGroup] by its [name].
 */
public fun MatchResult.group(name: String): MatchGroup? = namedGroups[name]

/**
 * Returns the matched [MatchGroup] by its [index].
 */
public fun MatchResult.group(index: Int): MatchGroup? = group(index)

/**
 * Returns the value of the matched [MatchGroup] with the provided [name].
 */
public fun MatchResult.groupValue(name: String): String? = group(name)?.value

/**
 * Returns the value of the matched [MatchGroup] with the provided [index].
 */
public fun MatchResult.groupValue(index: Int): String? = group(index)?.value

/**
 * Returns the value of the [MatchGroup] with the provided [name].
 */
public operator fun MatchResult.get(name: String): String? = groupValue(name)

/**
 * Returns the value of the matched [MatchGroup] with the provided [index].
 */
public operator fun MatchResult.get(index: Int): String? = groupValue(index)

/**
 * Returns a sequence of all occurrences of this regular expression within
 * the [input] string, beginning at the specified [startIndex].
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or
 *         greater than the length of the [input] char sequence.
 */
public fun Regex.findAllValues(input: CharSequence, startIndex: Int = 0): Sequence<String> =
    findAll(input, startIndex).map { it.value }

/**
 * Returns the number of all occurrences of this regular expression within
 * the [input] string, beginning at the specified [startIndex].
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or
 *         greater than the length of the [input] char sequence.
 */
public fun Regex.countMatches(input: CharSequence, startIndex: Int = 0): Int =
    findAll(input, startIndex).count()
