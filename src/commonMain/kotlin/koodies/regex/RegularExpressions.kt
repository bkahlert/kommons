package koodies.regex

import koodies.text.Semantics.formattedAs
import koodies.text.toCodePointList


public object RegularExpressions {
    public val atLeastOneWhitespaceRegex: Regex = Regex("\\s+")
    public val urlRegex: Regex = Regex("(?<schema>https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    public val uriRegex: Regex = Regex("\\w+:(?:/?/?)[^\\s]+")

    public val versionRegex: Regex =
        Regex("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)") // kotlin.ByteArray.() -> koodies.math.BigInteger /* = java.math.BigInteger */

    public val camelCaseRegex: Regex = Regex("(?<lowerLeftChar>[a-z0-9]|(?=[A-Z]))(?<upperRightChar>[A-Z])")
    public val screamingSnakeCaseRegex: Regex = Regex("(?<leftChar>[A-Z0-9]?)_(?<rightChar>[A-Z0-9])")
    public val kebabCaseRegex: Regex = Regex("(?<leftChar>[a-z0-9]?)-(?<rightChar>[a-z0-9])")

    private val DOT: Regex = Regex("\\.")
    private val COMMA: Regex = Regex(",")
    private val SPACE: Regex = Regex("\\s")
    private val SLACK0 = SPACE.repeatUnlimited()
    private val SLACK1 = SPACE.repeatAtLeastOnce()
    private val ARROW:Regex = (SLACK1 +Regex.fromLiteral("->")+ SLACK1).grouped
    private val COLON:Regex = (SLACK0 +Regex.fromLiteral(":")+ SLACK0).grouped
    private val ASSIGNMENT:Regex = (SLACK0 +Regex.fromLiteral("=")+ SLACK0).grouped
    private val BRACKET_OPEN: Regex = Regex("\\(")
    private val BRACKET_CLOSE: Regex = Regex("\\)")
    private val COMMENT_START: Regex = Regex("/\\*")
    private val COMMENT_END: Regex = Regex("\\*/")
    private val OPTIONAL_OPTIONALITY: Regex = Regex("[?!]?")

    // TODO add regex comments
    public fun packageRegex(groupName: String?): Regex = Regex("(?:\\w+\\.)*(?:\\w+)").group(groupName)

    public fun fullyQualifiedClassRegex(groupNamePrefix: String?): Regex {
        val pkg = packageRegex(groupNamePrefix?.let { it + "Ipkg" })
        val className = Regex("[\\w\$ ]+").group(groupNamePrefix?.let { it + "Itype" }) + OPTIONAL_OPTIONALITY
        val fullyQualified = (pkg + DOT).optional() + className
        return fullyQualified.grouped
    } // package.Class

    public fun typeAliasCommentRegex(groupNamePrefix: String?): Regex {
        val prefix = groupNamePrefix?.let { it + "Ialias" }
        val fullyQualified = fullyQualifiedClassRegex(prefix)
        return (COMMENT_START +  ASSIGNMENT + fullyQualified + SPACE + COMMENT_END).group(prefix)
    } // /* = other.Klass */

    public fun classRegex(groupNamePrefix: String?): Regex {
        val fullyQualified = fullyQualifiedClassRegex(groupNamePrefix)
        val typeAliasComment = typeAliasCommentRegex(groupNamePrefix)
        val fullyQualifiedWithTypeAliasComment = fullyQualified + (SPACE + typeAliasComment).optional()
        return fullyQualifiedWithTypeAliasComment.grouped
    } // package.Class /* = other.Klass */

    public val paramNameRegex : Regex = Regex("\\w+[\\w0-9]*").grouped

    /**
     * Regex that more or less matches a parameter. Ideally it would be
     * a [lambdaRegex] but that would require recursion. Therefore
     * this regex only matches expressions of the form:
     * - `package.Type`
     * - `(…) -> package.Type`
     * - `receiver.Type.(…) -> package.Type`
     */
    public val paramRegex: Regex = (paramNameRegex + COLON).optional()+ run {
        val optionalReceiver = (classRegex(null) + DOT).optional()
        val optionalParameterWithArrow = (BRACKET_OPEN+Regex(".*?")+ BRACKET_CLOSE  + ARROW).optional()
        val returnType = classRegex(null).group(null)
        optionalReceiver + optionalParameterWithArrow + returnType
    }
    public fun paramListRegex(groupName:String?): Regex {
        val optionalParams = ((paramRegex + COMMA + SPACE).repeatUnlimited() + paramRegex).optional().group(groupName)
        return BRACKET_OPEN+ optionalParams +BRACKET_CLOSE
    }

    public fun lambdaRegex(groupNamePrefix: String?): Regex {
        val receiverName = groupNamePrefix?.let { it + "Ireceiver" }
        val receiver = classRegex(receiverName).group(receiverName)
        val optionalReceiver = (receiver + DOT).optional()

        val parameterList = paramListRegex(groupNamePrefix?.let { it + "Iparams" })

        val returnName = groupNamePrefix?.let { it + "Ireturn" }
        val returnType = classRegex(returnName).group(returnName)

        return optionalReceiver + parameterList + ARROW + returnType
    }
}

/**
 * Returns a [Regex] that optionally matches this [Regex].
 *
 * Example: `abc` becomes `(?:abc)?`
 */
public fun Regex.optional(): Regex = Regex("$grouped?")

/**
 * Returns a [Regex] that matches this regex followed by the
 * specified [other] regex.
 *
 * Example: `abc` becomes `(?:abc)?`
 */
public operator fun Regex.plus(other: Regex): Regex = Regex("$this$other")

/**
 * Returns a [Regex] that groups this [Regex].
 *
 * If a [name] is specified, a named group (e.g. `(?<name>abc)` is returned.
 *
 * If no [name] is specified **and** `this` regex is not already grouped,
 * an anonymous group (e.g. `(?:abc)`) is returned.
 *
 * In other words: No unnecessary brackets are added.
 */
public fun Regex.group(name: String? = null): Regex = name
    ?.requireValidGroupName()?.run { Regex("(?<$name>$pattern)") }
    ?: run { if(isGrouped) this else Regex("(?:$pattern)") }

/**
 * Returns this regular expression if it [isGrouped] already or
 * this regular expression as a anonymous group otherwise.
 *
 * @see group
 */
public val Regex.grouped: Regex get() = group(null)

/**
 * Returns [this@requireValidGroupName] if it is valid. Otherwise an [IllegalArgumentException] is thrown.
 */
private fun String.requireValidGroupName() : String = apply{require(all { it in 'a'..'z' || it in 'A'..'Z' }) {
    "Group name $this must only consist of letters ${"a..z".formattedAs.input} and ${"A..Z".formattedAs.input}."
}}

/**
 * Whether this regular expression is a group—no matter if named, regular or anonymous.
 */
public val Regex.isGrouped: Boolean
    get() {
        if (pattern.length < 2) return false
        val unescaped = pattern.replace(Regex("\\\\."), "X")
        if (unescaped[0] != '(' || unescaped[unescaped.lastIndex] != ')') return false

        var depth = 1
        for (cp in unescaped.toCodePointList().drop(1).dropLast(1)) {
            when (cp.char) {
                '(' -> depth++
                ')' -> depth--
            }
            if (depth <= 0) return false
        }
        return true
    }

public fun Regex.repeatUnlimited(): Regex = Regex("$grouped*")
public fun Regex.repeatAtLeastOnce(): Regex = Regex("$grouped+")

public fun Regex.repeat(min: Int? = 0, max: Int? = null): Regex {
    if (min == 0 && max == 1) return optional()
    if (min == 0 && max == null) return repeatUnlimited()
    if (min == 1 && max == null) return repeatAtLeastOnce()
    val minString = min?.toString() ?: ""
    val maxString = max?.toString() ?: ""
    return Regex("$grouped{$minString,$maxString}")
}

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
