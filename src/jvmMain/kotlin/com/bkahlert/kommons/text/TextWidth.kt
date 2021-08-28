package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.synchronizedListOf
import com.bkahlert.kommons.math.isEven
import com.bkahlert.kommons.runtime.contextClassLoader
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.Semantics.formattedAs
import java.awt.Canvas
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.FontMetrics
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import java.awt.Label
import java.awt.Panel
import java.awt.font.TextAttribute
import javax.swing.JFrame
import javax.swing.JLabel


/**
 * Text width calculation.
 */
internal actual object TextWidth {

    private val MONOSPACED_METRICS: FontMetrics by lazy {
        System.setProperty("java.awt.headless", "true")
//        findSuitableFontsForMeasurement()
        // explicit font to create stable measure which is not the case for monospace
        val font = Font.createFonts(contextClassLoader.getResourceAsStream("courier.ttf")).first().deriveFont(mapOf(
            TextAttribute.WIDTH to null,
            TextAttribute.TRANSFORM to null,
            TextAttribute.TRACKING to null,
            TextAttribute.SIZE to 10,
            TextAttribute.POSTURE to null,
            TextAttribute.FAMILY to "Courier",
            TextAttribute.SUPERSCRIPT to null,
            TextAttribute.WEIGHT to null,
        ))
//        preview(font, font2)
        Canvas().getFontMetrics(font)
    }

    /**
     * The width of a monospaced letter `X`.
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
//        preview(sanitized)
        return MONOSPACED_METRICS.stringWidth(sanitized)
    }

    private fun findSuitableFontsForMeasurement() {
        System.setProperty("java.awt.headless", "true")
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.allFonts.forEach {
            println(it.name)
        }
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

    private var labels: MutableList<JLabel> = synchronizedListOf()

    private fun preview(vararg fonts: Font) {
        val frame = JFrame().apply {
            title = "Preview"
            layout = FlowLayout(FlowLayout.LEFT);
        }

        Panel().apply {
            layout = GridLayout(fonts.size, 1)
            fonts.forEach { font ->
                add(JLabel(font.toString(), Label.LEFT))
            }
        }.also { frame.add(it) }

        Panel().apply {
            layout = GridLayout(fonts.size, 1)
            fonts.forEach {
                add(JLabel("", Label.RIGHT)
                    .apply { font = it }
                    .also { labels.add(it) })
            }
        }.also { frame.add(it) }

        frame.pack()
        frame.size = Dimension(frame.width * 2, frame.height)
        frame.isVisible = true
    }

    private fun preview(text: String) {
        labels.forEach { it.text = text }
    }
}
