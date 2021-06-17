package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter

public data class Settings(
    public val contentFormatter: FilteringFormatter = FilteringFormatter.PassThrough,
    public val decorationFormatter: Formatter = Formatter.PassThrough,
    public val returnValueFormatter: (ReturnValue) -> ReturnValue? = run { { it } },
    public val layout: ColumnsLayout = ColumnsLayout(),

    public val blockStyle: Style = BlockStyles.DEFAULT,
    public val oneLineStyle: Style = OneLineStyles.DEFAULT,
)
