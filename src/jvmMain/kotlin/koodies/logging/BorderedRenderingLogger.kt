package koodies.logging

/**
 * Logger interface with the ability to render its log with a border.
 */
public interface BorderedRenderingLogger : RenderingLogger {

    public val bordered: Boolean
    public val statusInformationColumn: Int
    public val statusInformationPadding: Int
    public val statusInformationColumns: Int
    public val prefix: String

}
