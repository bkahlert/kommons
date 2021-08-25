package com.bkahlert.kommons.text

import com.bkahlert.kommons.math.isEven
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.Semantics.formattedAs
import java.awt.Canvas
import java.awt.Font
import java.awt.FontMetrics
import java.awt.GraphicsEnvironment

/**
 * Text width calculation.
 */
internal actual object TextWidth {

    // For some reason, running tests using Gradle in iTerm on macOS uses
    // a monospaced(?) font where some one-column characters (i.e. em-dash) render
    // wider than two column characters. Therefore, trying to select a font explicitly
    // of which such issue is not known.
    private val fontNames = listOf("Courier", "Monaco", "Times New Roman", "Courier Prime")

    // TODO get tests running on github
    // TODO get courir running and remove courier prime later
    private val MONOSPACED_METRICS: FontMetrics by lazy {
        System.setProperty("java.awt.headless", "true")
        findSuitableFontsForMeasurement()
        val font = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
            .firstOrNull { fontNames.contains(it.name) }?.run { deriveFont(10f) }
            ?: Font(Font.MONOSPACED, Font.PLAIN, 10)
        "PICKED $font"
        Canvas().getFontMetrics(font)
    }

    /**
     * The width of an monospaced letter `X`.
     */
    actual val X_WIDTH: Int by lazy { MONOSPACED_METRICS.charWidth('X') }

    /**
     * The width by which one column can vary.
     */
    actual val COLUMN_SLACK: Int by lazy {
        (if (X_WIDTH.isEven) X_WIDTH - 1 else X_WIDTH) / 2
    }

    /**
     * Returns the width of the given [text].
     */
    actual fun calculateWidth(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        val sanitized: String = text.replace(LineSeparators.REGEX, "").ansiRemoved
        return MONOSPACED_METRICS.stringWidth(sanitized)
    }

    private fun findSuitableFontsForMeasurement() {
        System.setProperty("java.awt.headless", "true")
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.allFonts.forEach { baseFont ->
            val font = baseFont.deriveFont(10f)
            val metrics = Canvas().getFontMetrics(font)

            val oneColumnWidths = listOf('A', '—').maxOf { metrics.charWidth(it) } to listOf("A", "—", "‾͟͟͞", "—̳͟͞͞").maxOf { metrics.stringWidth(it) }
            val twoColumnWidths =
                listOf('한', '글', '⮕').minOf { metrics.charWidth(it) } to listOf("한", "글", "⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧").minOf { metrics.stringWidth(it) }
            val suitable = oneColumnWidths.first < twoColumnWidths.first && oneColumnWidths.second < twoColumnWidths.second

            if (suitable) println("$oneColumnWidths .. $twoColumnWidths << ${font.name.formattedAs.input}")
            else println("👎 $oneColumnWidths .. $twoColumnWidths << ${font.name.formattedAs.input}")
        }
    }
}
