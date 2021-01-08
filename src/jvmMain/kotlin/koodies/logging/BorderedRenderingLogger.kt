package koodies.logging

/**
 * Logger interface with the ability to render its log with a border.
 */
interface BorderedRenderingLogger : RenderingLogger {

    val bordered: Boolean
    val statusInformationColumn: Int
    val statusInformationPadding: Int
    val statusInformationColumns: Int
    val prefix: String

}
