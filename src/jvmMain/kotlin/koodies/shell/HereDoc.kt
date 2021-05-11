package koodies.shell

import koodies.regex.get
import koodies.text.CharRanges
import koodies.text.LineSeparators
import koodies.text.randomString
import koodies.text.singleQuoted

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [commands], a customizable [delimiter] and [lineSeparator].
 */
public class HereDoc(
    /**
     * Lines of text this here document is made of.
     */
    public vararg val commands: String,

    /**
     * Identifier to delimit this here document from the surrounding text.
     */
    public val delimiter: String = randomDelimiter(),

    /**
     * Whether the shell will process this heredoc like any other input,
     * in particular substitution will take place.
     *
     * If disabled, the content will be treated as is, i.e. `$HOME` will no be substituted.
     *
     * @see <a href="https://tldp.org/LDP/abs/html/here-docs.html">Advanced Bash-Scripting Guide: Chapter 19. Here Documents</a>
     */
    public val substituteParameters: Boolean = true,
) : CharSequence {
    /**
     * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [commands], a customizable [delimiter] and [lineSeparator].
     */
    public constructor(commands: Iterable<CharSequence>, delimiter: String = randomDelimiter(), substituteParameters: Boolean = true) :
        this(*commands.map { it.toString() }.toTypedArray<String>(), delimiter = delimiter, substituteParameters = substituteParameters)

    public companion object {

        /**
         * A [Regex] that can be used to extract [here document](https://en.wikipedia.org/wiki/Here_document) delimiters.
         */
        private val hereDocDelimiterRegex: Regex = Regex("<<(?<name>\\w[-\\w]*)\\s*")

        public fun findAllDelimiters(text: String): List<String> = hereDocDelimiterRegex.findAll(text).mapNotNull { it["name"] }.toList()

        /**
         * Returns a random—most likely unique—label to be used for a [HereDoc].
         */
        public fun randomDelimiter(): String = "HERE-" + randomString(8, CharRanges.UpperCaseAlphanumeric)

        /**
         * The line separator used by default to separate lines in a [HereDoc].
         */
        public const val DEFAULT_LINE_SEPARATOR: String = LineSeparators.LF
    }

    private val rendered = sequenceOf(
        "<<${delimiter.takeIf { substituteParameters } ?: delimiter.singleQuoted}",
        *commands,
        delimiter,
    ).joinToString(DEFAULT_LINE_SEPARATOR)

    override val length: Int = rendered.length
    override fun get(index: Int): Char = rendered[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = rendered.subSequence(startIndex, endIndex)
    override fun toString(): String = rendered
    override fun hashCode(): Int = rendered.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HereDoc
        if (rendered != other.rendered) return false
        return true
    }
}

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of this collection of commands
 * and the optionally configurable [delimiter].
 */
public fun Iterable<CharSequence>.toHereDoc(
    delimiter: String = HereDoc.randomDelimiter(),
): HereDoc = HereDoc(delimiter = delimiter, commands = this)

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of this array of commands
 * and the optionally configurable [delimiter].
 */
public fun Array<String>.toHereDoc(
    delimiter: String = HereDoc.randomDelimiter(),
): HereDoc = HereDoc(commands = this, delimiter = delimiter)
