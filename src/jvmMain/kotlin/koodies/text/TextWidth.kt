package koodies.text

import koodies.debug.debug
import koodies.math.ceilDiv
import koodies.text.ANSI.ansiRemoved
import java.awt.Canvas
import java.awt.Font
import java.awt.FontMetrics
import kotlin.system.exitProcess

/**
 * Text width calculation.
 */
internal actual object TextWidth {

    private val MONOSPACED_METRICS: FontMetrics by lazy {
        System.setProperty("java.awt.headless", "true")
        val monospacedFont = Font(Font.MONOSPACED, Font.PLAIN, 10)
        Canvas().getFontMetrics(monospacedFont)
    }

    /**
     * The width of an monospaced letter `X`.
     */
    actual val X_WIDTH: Int by lazy { MONOSPACED_METRICS.charWidth('X') }

    private var prevText = ""
    private var prevCount = 0

    /**
     * CALCULATED WIDTH OF  (❬⦀0❭) — 2 M
    CALCULATED WIDTH OF M (❬␛M⦀2❭) — 0
    CALCULATED WIDTH OF M (❬␛M⦀2❭) — 0 M
    CALCULATED WIDTH OF M (❬␛M⦀2❭) — 1 M
     */
    /**
     * Returns the width of the given [text].
     */
    actual fun calculateWidth(text: CharSequence): Int {
        println("CALCULATED WIDTH OF $text (${text.debug}) — $prevCount $prevText")
        if (prevText == text.toString()) prevCount++
        else {
            prevText = text.toString()
            prevCount = 0
        }
        if (prevCount > 20) {
            println("CALCULATED WIDTH OF $text (${text.debug})")
            try {
                throw RuntimeException()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            exitProcess(1)
        }
        if (text.isEmpty()) return 0
        val sanitized = text.replace(LineSeparators.REGEX, "").ansiRemoved
        val codePoints = sanitized.toCodePointList()
        var width = 0
        var offset = 0
        while (offset < codePoints.size) {
            val (graphemeWidth, graphemeCodePoints) = codePoints.graphemeWidth(offset)
            width += graphemeWidth.toMultiplesOfX()
            offset += graphemeCodePoints
        }
        if (text == Unicode.escape + "M") {
            println("CALCULATED WIDTH OF $text (${sanitized.debug}; ${codePoints.size}): $width")
            try {
                throw RuntimeException()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            exitProcess(1)
        }
        return width
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.toMultiplesOfX() = (this ceilDiv X_WIDTH) * X_WIDTH

    /**
     * Returns a description of the grapheme and the given [offset].
     * The [Pair.first] component describes the width of the found grapheme.
     * The [Pair.second] component is the number of code points this grapheme consists of.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun List<CodePoint>.graphemeWidth(offset: Int): Pair<Int, Int> {
        val codePoints = drop(offset)
        val codePoint = codePoints.first()
        if (codePoint.codePoint == 0) return 0 to 1
        if (codePoint.codePoint < 32 || (codePoint.codePoint in 0x7f..0x9f)) return -X_WIDTH to 1

        val grapheme = StringBuilder(codePoint.string)
        val graphemeWidth = MONOSPACED_METRICS.stringWidth(grapheme.toString())
        var graphemeCodePoints = 1
        codePoints.drop(graphemeCodePoints).takeWhile {
            grapheme.append(it.string)
            val sameColumns = MONOSPACED_METRICS.stringWidth(grapheme.toString()) == graphemeWidth
            if (sameColumns) graphemeCodePoints++
            sameColumns
        }
        return graphemeWidth to graphemeCodePoints
    }
}
