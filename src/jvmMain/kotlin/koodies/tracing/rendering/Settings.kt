package koodies.tracing.rendering

import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi

/**
 * Common settings used by implementations of [Renderer].
 */
public data class Settings(

    /**
     * Formatter to be applied to the name.
     */
    public val nameFormatter: FilteringFormatter = FilteringFormatter { it.toString().ansi.bold },

    /**
     * Formatter to be applied to content.
     */
    public val contentFormatter: FilteringFormatter = FilteringFormatter.ToCharSequence,

    /**
     * Formatter to be applied to decoration such as borders.
     */
    public val decorationFormatter: Formatter = Formatter.ToCharSequence,

    /**
     * Transformation to be applied to a [ReturnValue] before rendered.
     */
    public val returnValueTransform: (ReturnValue) -> ReturnValue? = run { { it } },

    /**
     * Layout to be used.
     */
    public val layout: ColumnsLayout = ColumnsLayout(),

    /**
     * (Accumulated) indent by which the effective layouts usable width is reduced.
     */
    public val indent: Int = 0,

    /**
     * Style to be applied by block-based / fixed-size renderers.
     */
    public val blockStyle: (ColumnsLayout, Int) -> BlockStyle = BlockStyles.DEFAULT,

    /**
     * Style to be applied by one-line renderers.
     */
    public val oneLineStyle: Style = OneLineStyles.DEFAULT,

    /**
     * Function to be used to print the actual rendered output.
     */
    public val printer: Printer = { println(it) },
)
