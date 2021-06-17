package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.PassThrough
import koodies.text.LineSeparators
import koodies.text.takeUnlessEmpty

/**
 * A style is a simple generalization attempt
 * to improve separation of concern.
 *
 * This component assumes that what needs to be styled consists of
 * a start and end element and 0 or more content elements.
 */
public interface Style {
    public val indent: Int

    /**
     * Styles the introducing first element.
     *
     * The optional [decorationFormatter] will be applied on all
     * "decoration" added.
     */
    public fun start(
        element: CharSequence,
        decorationFormatter: Formatter = PassThrough,
    ): CharSequence?

    /**
     * Styles a content element.
     *
     * The optional [decorationFormatter] will be applied on all
     * "decoration" added.
     */
    public fun content(
        element: CharSequence,
        decorationFormatter: Formatter = PassThrough,
    ): CharSequence?

    /**
     * Styles an element to be inserted in its parent which
     * is necessary for nested layouts.
     *
     * The optional [decorationFormatter] will be applied on all
     * "decoration" added.
     */
    public fun parent(
        element: CharSequence,
        decorationFormatter: Formatter = PassThrough,
    ): CharSequence? = content(element, decorationFormatter)

    /**
     * Styles the finalizing last element.
     *
     * The optional [decorationFormatter] will be applied on all
     * "decoration" added.
     */
    public fun end(
        element: ReturnValue,
        resultValueFormatter: (ReturnValue) -> ReturnValue?,
        decorationFormatter: Formatter = PassThrough,
    ): CharSequence?

    public fun buildString(block: StringBuilder.() -> Unit): CharSequence? =
        StringBuilder().apply(block).takeUnlessEmpty()

    public fun StringBuilder.append(vararg text: CharSequence?): StringBuilder =
        apply { text.forEach { if (it != null) append(it) } }

    public fun StringBuilder.appendLine(vararg text: CharSequence?): StringBuilder =
        apply { append(*text, LineSeparators.DEFAULT) }
}
