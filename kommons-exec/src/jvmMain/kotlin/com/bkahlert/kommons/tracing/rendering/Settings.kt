package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi

/**
 * Common settings used by implementations of [Renderer].
 */
public data class Settings(

    /**
     * Formatter to be applied to the name.
     */
    public val nameFormatter: FilteringFormatter<CharSequence> = FilteringFormatter { it.ansi.bold },

    /**
     * Formatter to be applied to content.
     */
    public val contentFormatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence,

    /**
     * Formatter to be applied to decoration such as borders.
     */
    public val decorationFormatter: Formatter<CharSequence> = Formatter.ToCharSequence,

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
    public val style: (ColumnsLayout, Int) -> Style = Styles.DEFAULT,

    /**
     * Function to be used to print the actual rendered output.
     */
    public val printer: Printer = { println(it) },
)
