package com.bkahlert.kommons.text.styling

import com.bkahlert.kommons.math.isEven
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.lineSequence
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons.text.maxLength
import com.bkahlert.kommons.text.repeat
import com.bkahlert.kommons.text.styling.Boxes.FAIL
import com.bkahlert.kommons.text.styling.Boxes.PILLARS
import com.bkahlert.kommons.text.styling.Boxes.SINGLE_LINE_SPHERICAL
import com.bkahlert.kommons.text.styling.Boxes.SPHERICAL
import com.bkahlert.kommons.text.styling.Boxes.WIDE_PILLARS

/**
 * # Boxes
 *
 * ## [FAIL]
 * ```
 * ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
 * ████▌▄▌▄▐▐▌█████
 * ████▌▄▌▄▐▐▌▀████
 * ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
 * ```
 *
 * ## [SPHERICAL]
 * ```
 *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
 * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏   SPHERE BOX    ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
 *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
 * ```
 *
 * ## [SINGLE_LINE_SPHERICAL]
 * ```
 * ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎
 * ```
 *
 * ## [WIDE_PILLARS]
 * ```
 * █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
 * ```
 *
 * ## [PILLARS]
 * ```
 * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
 * ```
 */
public enum class Boxes(private var render: (String) -> String) {
    /**
     * ```
     * ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * ████▌▄▌▄▐▐▌█████
     * ████▌▄▌▄▐▐▌▀████
     * ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
     * ```
     */
    FAIL({
        """
        ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
        ████▌▄▌▄▐▐▌█████
        ████▌▄▌▄▐▐▌▀████
        ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
        """.trimIndent()
    }),

    /**
     * ```
     *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
     * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏   SPHERE BOX    ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
     *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
     * ```
     */
    SPHERICAL({ text ->
        val fillCount = if (text.maxLength().isEven) 14 else 15
        val paddedText = text.center(Unicode.NBSP, fillCount)
        val fill = ' '.repeat((paddedText.maxLength() - fillCount).coerceAtLeast(0))

        StringBuilder().apply {
            append("${sphericalLeft}$fill${sphericalRight}$LF")
            paddedText.lineSequence().joinToString(separator = "") {
                append("${sphericalMiddleLeft}$it${sphericalMiddleRight}$LF")
            }
            append("${sphericalLeft}$fill${sphericalRight}")
        }.toString()
    }),

    /**
     * ```
     * ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎
     * ```
     */
    SINGLE_LINE_SPHERICAL({ text ->
        text.center().mapLines {
            " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  $it  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  "
        }
    }),

    /**
     * ```
     * █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
     * ```
     */
    WIDE_PILLARS({ text ->
        text.center().mapLines {
            "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ $it  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █"
        }
    }),

    /**
     * ```
     * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
     * ```
     */
    PILLARS({ text ->
        text.center().mapLines {
            "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ $it  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"
        }
    }),
    ;

    public companion object {
        private const val sphericalLeft = """  █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏ """
        private const val sphericalRight = """ ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █"""
        private const val sphericalMiddleLeft = """█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ """
        private const val sphericalMiddleRight = """  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"""

        /**
         * Centers this [CharSequence] and puts a styled [box] (see [Boxes]) around it.
         */
        public fun <T : CharSequence> T.wrapWithBox(
            box: Boxes = SPHERICAL,
        ): String = box(this)
    }

    public operator fun invoke(text: CharSequence): String = render("$text")

    override fun toString(): String = render(name)
}
