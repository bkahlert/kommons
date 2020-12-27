package koodies.shell

import koodies.text.CharRanges
import koodies.text.LineSeparators
import koodies.text.randomString

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
class HereDoc(
    /**
     * Identifier to delimit this here document from the surrounding text.
     */
    val label: String = randomLabel(),
    /**
     * Separator used to separator the lines of this here document.
     */
    val lineSeparator: String = DEFAULT_LINE_SEPARATOR,
    /**
     * Lines of text this here document is made of.
     */
    val lines: Array<String>,
) : CharSequence {
    /**
     * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
     */
    constructor(label: String = randomLabel(), lineSeparator: String = DEFAULT_LINE_SEPARATOR, lines: Iterable<CharSequence>) :
        this(label = label, lineSeparator = lineSeparator, lines = lines.map { "$it" }.toTypedArray())

    /**
     * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
     */
    constructor(vararg lines: CharSequence, label: String = randomLabel(), lineSeparator: String = DEFAULT_LINE_SEPARATOR) :
        this(label = label, lineSeparator = lineSeparator, lines = lines.map { "$it" })

    companion object {
        /**
         * Returns a random—most likely unique—label to be used for a [HereDoc].
         */
        fun randomLabel(): String = "HERE-" + randomString(8, CharRanges.UpperCaseAlphanumeric)

        /**
         * The line separator used by default to separate lines in a [HereDoc].
         */
        const val DEFAULT_LINE_SEPARATOR: String = LineSeparators.LF
    }

    private val rendered = sequenceOf("<<$label", *lines, label).joinToString(separator = lineSeparator)

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
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> List<T>.toHereDoc(
    label: String = HereDoc.randomLabel(),
    lineSeparator: String = HereDoc.DEFAULT_LINE_SEPARATOR,
): HereDoc = HereDoc(label = label, lineSeparator = lineSeparator, lines = this)

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> Iterable<T>.toHereDoc(
    label: String = HereDoc.randomLabel(),
    lineSeparator: String = HereDoc.DEFAULT_LINE_SEPARATOR,
): HereDoc = HereDoc(label = label, lineSeparator = lineSeparator, lines = this)

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> Array<T>.toHereDoc(
    label: String = HereDoc.randomLabel(),
    lineSeparator: String = HereDoc.DEFAULT_LINE_SEPARATOR,
): HereDoc = HereDoc(label = label, lineSeparator = lineSeparator, lines = this)
