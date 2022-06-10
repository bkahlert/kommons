package com.bkahlert.kommons.shell

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.builder.Builder
import com.bkahlert.kommons.builder.Init
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.regex.get
import com.bkahlert.kommons.shell.HereDoc.Companion.HereDocContext
import com.bkahlert.kommons.text.singleQuoted

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [commands] and a customizable [delimiter].
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
     * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [commands] and a customizable [delimiter].
     */
    public constructor(commands: Iterable<CharSequence>, delimiter: String = randomDelimiter(), substituteParameters: Boolean = true) :
        this(*commands.map { it.toString() }.toTypedArray<String>(), delimiter = delimiter, substituteParameters = substituteParameters)

    private val rendered = sequenceOf(
        "<<${delimiter.takeIf { substituteParameters } ?: delimiter.singleQuoted}",
        *commands,
        delimiter,
    ).joinToString(LineSeparators.Default)

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

    public companion object : Builder<Init<HereDocContext>, HereDoc> {

        override fun invoke(init: Init<HereDocContext>): HereDoc =
            StringBuilder().let { commands ->
                val (delimiter, substituteParameters) = HereDocContext(commands).run { init(); delimiter to substituteParameters }
                HereDoc(commands.toString(), delimiter = delimiter, substituteParameters = substituteParameters)
            }

        public class HereDocContext(private val commands: StringBuilder) {

            /**
             * Identifier to delimit this here document from the surrounding text.
             */
            public var delimiter: String = randomDelimiter()

            /**
             * Whether the shell will process this heredoc like any other input,
             * in particular substitution will take place.
             *
             * If disabled, the content will be treated as is, i.e. `$HOME` will no be substituted.
             *
             * @see <a href="https://tldp.org/LDP/abs/html/here-docs.html">Advanced Bash-Scripting Guide: Chapter 19. Here Documents</a>
             */
            public var substituteParameters: Boolean = true

            public fun add(element: CharSequence) {
                if (!commands.isEmpty()) commands.appendLine()
                commands.append(element)
            }
        }

        /**
         * A [Regex] that can be used to extract [here document](https://en.wikipedia.org/wiki/Here_document) delimiters.
         */
        private val hereDocDelimiterRegex: Regex = Regex("<<(?<name>\\w[-\\w]*)\\s*")

        public fun findAllDelimiters(text: String): List<String> = hereDocDelimiterRegex.findAll(text).mapNotNull { it["name"] }.toList()

        /**
         * Returns a random—most likely unique—label to be used for a [HereDoc].
         */
        public fun randomDelimiter(): String = "HERE-" + randomString(8).uppercase()
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
