package koodies.text

/**
 * Text width calculation.
 */
internal actual object TextWidth {

    /**
     * The width of an monospaced letter `X`.
     */
    actual val X_WIDTH: Int = 1

    /**
     * Returns the width of the given [text].
     */
    actual fun calculateWidth(text: CharSequence): Int = text.length
}
