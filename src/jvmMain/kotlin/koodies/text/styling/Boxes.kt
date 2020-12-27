package koodies.text.styling

import koodies.text.repeat
import koodies.number.isEven
import koodies.text.styling.Boxes.FAIL
import koodies.text.styling.Boxes.PILLARS
import koodies.text.styling.Boxes.SINGLE_LINE_SPHERICAL
import koodies.text.styling.Boxes.SPHERICAL
import koodies.text.styling.Boxes.WIDE_PILLARS


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
enum class Boxes(private var render: (String) -> String) {
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
        val fillCount = if (text.length.isEven) 14 else 15
        val paddedText = (text + '\n' + ('X'.repeat(fillCount))).center().lines().first().padEnd(fillCount)
        val fill = ' '.repeat((paddedText.length - fillCount).coerceAtLeast(0))

        """
        $sphericalLeft$fill$sphericalRight
        $sphericalMiddleLeft$paddedText$sphericalMiddleRight
        $sphericalLeft$fill$sphericalRight
        """.trimIndent()
    }),

    /**
     * ```
     * ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎
     * ```
     */
    SINGLE_LINE_SPHERICAL({ text ->
        " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  $text  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  "
    }),

    /**
     * ```
     * █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
     * ```
     */
    WIDE_PILLARS({ text ->
        "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ $text  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █"
    }),

    /**
     * ```
     * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
     * ```
     */
    PILLARS({ text ->
        "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ $text  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"
    }),
    ;

    companion object {
        private const val sphericalLeft = """  █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏ """
        private const val sphericalRight = """ ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █"""
        private const val sphericalMiddleLeft = """█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ """
        private const val sphericalMiddleRight = """  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"""

        /**
         * Centers this [CharSequence] and puts a styled [box] (see [Boxes]) around it.
         */
        fun <T : CharSequence> T.wrapWithBox(
            box: Boxes = SPHERICAL,
        ): String = box(this)
    }

    operator fun invoke(text: CharSequence): String = render("$text")

    override fun toString(): String = render(name)
}
