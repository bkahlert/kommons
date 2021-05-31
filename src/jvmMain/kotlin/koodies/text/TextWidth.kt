package koodies.text

import koodies.text.ANSI.ansiRemoved
import java.awt.Canvas
import java.awt.Font
import java.awt.FontMetrics

/**
 * Text width calculation.
 */
internal actual object TextWidth {

    private val MONOSPACED_METRICS: FontMetrics by lazy {
        System.setProperty("java.awt.headless", "true")
        Canvas().getFontMetrics(Font(Font.MONOSPACED, Font.PLAIN, 10))
    }

    /**
     * The width of an monospaced letter `X`.
     */
    actual val X_WIDTH: Int by lazy { calculateWidth("X") }

    /**
     * Returns the width of the given [text].
     */
    actual fun calculateWidth(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        val sanitized = text.replace(LineSeparators.REGEX, "").ansiRemoved
        return MONOSPACED_METRICS.stringWidth(sanitized)
    }
}
