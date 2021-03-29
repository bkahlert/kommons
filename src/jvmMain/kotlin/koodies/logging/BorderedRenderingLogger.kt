package koodies.logging

import koodies.text.ANSI.Formatter

/**
 * Logger interface with the ability to render its log with a border.
 */
public abstract class BorderedRenderingLogger(
    caption: String,
    parent: BorderedRenderingLogger? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    bordered: Boolean? = null,
    width: Int? = null,
    public open val prefix: String = "",
) : RenderingLogger(caption, parent) {

    public val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    public val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    public val bordered: Boolean = bordered ?: false
    public val statusInformationColumn: Int = width ?: parent?.let { it.statusInformationColumn - prefix.length } ?: 100
    public val statusInformationPadding: Int = parent?.statusInformationPadding ?: 5
    public val statusInformationColumns: Int = parent?.let { it.statusInformationColumns - prefix.length } ?: 45

    init {
        require(statusInformationColumn > 0) { ::statusInformationColumn.name + " must be positive but was $statusInformationColumn" }
        require(statusInformationPadding > 0) { ::statusInformationPadding.name + " must be positive but was $statusInformationPadding" }
        require(statusInformationColumns > 0) { ::statusInformationColumns.name + " must be positive but was $statusInformationColumns" }
    }

    public val totalColumns: Int = statusInformationColumn + statusInformationPadding + statusInformationColumns

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public fun <R> blockLogging(
        caption: CharSequence,
        contentFormatter: Formatter? = null,
        decorationFormatter: Formatter? = null,
        bordered: Boolean = this.bordered,
        block: BorderedRenderingLogger.() -> R,
    ): R = BlockRenderingLogger(caption, this, contentFormatter, decorationFormatter, bordered).runLogging(block)

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public fun <R> compactLogging(
        caption: CharSequence,
        formatter: Formatter? = null,
        block: CompactRenderingLogger.() -> R,
    ): R = CompactRenderingLogger(caption, formatter, this).runLogging(block)

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     */
    @RenderingLoggingDsl
    public fun <R> logging(
        caption: CharSequence,
        contentFormatter: Formatter? = null,
        decorationFormatter: Formatter? = null,
        bordered: Boolean = this.bordered,
        block: BorderedRenderingLogger.() -> R,
    ): R = SmartRenderingLogger(caption, this, contentFormatter, decorationFormatter, bordered).runLogging(block)
}
