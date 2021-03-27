package koodies.logging

import koodies.text.ANSI.Formatter

/**
 * Logger interface with the ability to render its log with a border.
 */
public abstract class BorderedRenderingLogger(
    caption: String,
    parent: RenderingLogger? = null,
    log: (String) -> Unit = { output: String -> print(output) },
) : RenderingLogger(caption, parent, log) {
    public abstract val contentFormatter: Formatter?
    public abstract val decorationFormatter: Formatter?
    public abstract val bordered: Boolean
    public abstract val statusInformationColumn: Int
    public abstract val statusInformationPadding: Int
    public abstract val statusInformationColumns: Int
    public abstract val prefix: String
}
