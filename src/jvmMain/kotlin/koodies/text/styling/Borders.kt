package koodies.text.styling

import com.github.ajalt.mordant.AnsiCode
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.Grapheme.Companion.getGraphemeCount
import koodies.text.asCodePointSequence
import koodies.text.joinLinesToString
import koodies.text.repeat
import koodies.text.styling.Borders.Block
import koodies.text.styling.Borders.Double
import koodies.text.styling.Borders.Heavy
import koodies.text.styling.Borders.HeavyDotted
import koodies.text.styling.Borders.Light
import koodies.text.styling.Borders.LightDotted
import koodies.text.styling.Borders.Rounded
import koodies.text.styling.Borders.SpikedInward
import koodies.text.styling.Borders.SpikedOutward

/**
 * # Borders
 *
 * ## [Light]
 * ```
 *  ┌────────┐
 *  │ SAMPLE │
 *  └────────┘
 * ```
 *
 * ## [Heavy]
 * ```
 *  ┏━━━━━━━━┓
 *  ┃ SAMPLE ┃
 *  ┗━━━━━━━━┛
 * ```
 *
 * ## [Block]
 * ```
 *  ██████████
 *  █ SAMPLE █
 *  ██████████
 * ```
 *
 * ## [Double]
 * ```
 *  ╔════════╗
 *  ║ SAMPLE ║
 *  ╚════════╝
 * ```
 *
 * ## [Rounded]
 * ```
 *  ╭────────╮
 *  │ SAMPLE │
 *  ╰────────╯
 * ```
 *
 * ## [LightDotted]
 * ```
 *  ┌┄┄┄┄┄┄┄┄┐
 *  ┊ SAMPLE ┊
 *  └┈┄┄┄┄┄┄┄┘
 * ```
 *
 * ## [HeavyDotted]
 * ```
 *  ┏╍╍╍╍╍╍╍╍┓
 *  ┇ SAMPLE ┇
 *  ┗╍╍╍╍╍╍╍╍┛
 * ```
 *
 * ## [SpikedOutward]
 * ```
 *  △△△△△△△△
 * ◁ SAMPLE ▷
 *  ▽▽▽▽▽▽▽▽
 * ```
 *
 * ## [SpikedInward]
 * ```
 *  ◸▽▽▽▽▽▽▽▽◹
 *  ▷ SAMPLE ◁
 *  ◺△△△△△△△△◿
 * ```
 */
enum class Borders(val matrix: String) : CharSequence by matrix {

    /**
     * ```
     *  ┌────────┐
     *  │ SAMPLE │
     *  └────────┘
     * ```
     */
    Light("""
        ┌─┐
        │ │
        └─┘
    """.trimIndent()),

    /**
     * ```
     *  ┏━━━━━━━━┓
     *  ┃ SAMPLE ┃
     *  ┗━━━━━━━━┛
     * ```
     */
    Heavy("""
        ┏━┓
        ┃ ┃
        ┗━┛
    """.trimIndent()),

    /**
     * ```
     *  ██████████
     *  █ SAMPLE █
     *  ██████████
     * ```
     */
    Block("""
        ███
        █ █
        ███
    """.trimIndent()),

    /**
     * ```
     *  ╔════════╗
     *  ║ SAMPLE ║
     *  ╚════════╝
     * ```
     */
    Double("""
        ╔═╗
        ║ ║
        ╚═╝
    """.trimIndent()),

    /**
     * ```
     *  ╭────────╮
     *  │ SAMPLE │
     *  ╰────────╯
     * ```
     */
    Rounded("""
        ╭─╮
        │ │
        ╰─╯
    """.trimIndent()),

    /**
     * ```
     *  ┌┄┄┄┄┄┄┄┄┐
     *  ┊ SAMPLE ┊
     *  └┈┄┄┄┄┄┄┄┘
     * ```
     */
    LightDotted("""
        ┌┄┐
        ┊ ┊
        └┈┘
    """.trimIndent()),

    /**
     * ```
     *  ┏╍╍╍╍╍╍╍╍┓
     *  ┇ SAMPLE ┇
     *  ┗╍╍╍╍╍╍╍╍┛
     * ```
     */
    HeavyDotted("""
        ┏╍┓
        ┇ ┇
        ┗╍┛
    """.trimIndent()),

    /**
     * ```
     *  △△△△△△△△
     * ◁ SAMPLE ▷
     *  ▽▽▽▽▽▽▽▽
     * ```
     */
    SpikedOutward("""
         △ 
        ◁ ▷
         ▽ 
    """.trimIndent()),

    /**
     * ```
     *  ◸▽▽▽▽▽▽▽▽◹
     *  ▷ SAMPLE ◁
     *  ◺△△△△△△△△◿
     * ```
     */
    SpikedInward("""
        ◸▽◹
        ▷ ◁
        ◺△◿
    """.trimIndent()),
    ;

    init {
        val lines = matrix.lines()
        check(lines.size == 3) { "Matrix must have exactly 3 lines. Only ${lines.size} found." }
        lines.onEach { line ->
            check(line.getGraphemeCount() == 3) {
                "Each line of the matrix must consist of exactly 3 characters. Instead " +
                    line.asCodePointSequence().map { "$it" + ":" + it.string }.toList() +
                    " found in $line."
            }
        }
    }
}


/**
 * Centers this [CharSequence] and the specified [padding] and puts a [ansiCode] styled [border] (see [Borders] for predefined one) around it.
 * Furthermore a [margin] can be set to distance the bordered text.
 */
fun <T : CharSequence> T.wrapWithBorder(
    border: CharSequence = Rounded,
    padding: Int = 2,
    margin: Int = 1,
    ansiCode: AnsiCode = AnsiCode(emptyList()),
): String {
    val block = this.lines().center(border[5])
    if (block.isEmpty()) return this.toString()
    val width = block[0].removeEscapeSequences().length
    val height = block.size
    val bordered = "" +
        ansiCode("${border[0]}${border[1].repeat(width + padding * 2)}${border[2]}") + "\n" +
        (0 until padding / 2).joinToString("") {
            ansiCode("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}") + "\n"
        } +
        (0 until height).joinToString("") { y ->
            ansiCode("${border[4]}${border[5].repeat(padding)}") + block[y] + ansiCode("${border[5].repeat(padding)}${border[6]}") + "\n"
        } +
        (0 until padding / 2).joinToString("") {
            ansiCode("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}") + "\n"
        } +
        ansiCode("${border[8]}${border[9].repeat(width + padding * 2)}${border[10]}")
    return if (margin == 0) bordered
    else bordered.wrapWithBorder(border[5].repeat(11), padding = margin - 1, margin = 0, ansiCode = ansiCode)
}

/**
 * Centers this list of [CharSequence] and the specified [padding] and puts a [ansiCode] styled [border] (see [Borders] for predefined one) around it.
 * Furthermore a [margin] can be set to distance the bordered text.
 */
fun <T : CharSequence> Iterable<T>.wrapWithBorder(
    border: CharSequence = Borders.Rounded,
    padding: Int = 2,
    margin: Int = 1,
    ansiCode: AnsiCode = AnsiCode(emptyList()),
): String = joinLinesToString().wrapWithBorder(border, padding, margin, ansiCode)

class Draw(val text: CharSequence) {
    val border get() = Border()

    inner class Border {
        /**
         * ```
         *  ┌────────┐
         *  │ SAMPLE │
         *  └────────┘
         * ```
         */
        fun light(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = text.wrapWithBorder(Light, padding, margin, ansiCode)

        /**
         * ```
         *  ┏━━━━━━━━┓
         *  ┃ SAMPLE ┃
         *  ┗━━━━━━━━┛
         * ```
         */
        fun heavy(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = text.wrapWithBorder(Heavy, padding, margin, ansiCode)

        /**
         * ```
         *  ██████████
         *  █ SAMPLE █
         *  ██████████
         * ```
         */
        fun block(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = text.wrapWithBorder(Block, padding, margin, ansiCode)

        /**
         * ```
         *  ╔════════╗
         *  ║ SAMPLE ║
         *  ╚════════╝
         * ```
         */
        fun double(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(Double, padding, margin, ansiCode)

        /**
         * ```
         *  ╭────────╮
         *  │ SAMPLE │
         *  ╰────────╯
         * ```
         */
        fun rounded(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(Rounded, padding, margin, ansiCode)

        /**
         * ```
         *  ┌┄┄┄┄┄┄┄┄┐
         *  ┊ SAMPLE ┊
         *  └┈┄┄┄┄┄┄┄┘
         * ```
         */
        fun lightDotted(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(LightDotted, padding, margin, ansiCode)

        /**
         * ```
         *  ┏╍╍╍╍╍╍╍╍┓
         *  ┇ SAMPLE ┇
         *  ┗╍╍╍╍╍╍╍╍┛
         * ```
         */
        fun heavyDotted(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(HeavyDotted, padding, margin, ansiCode)

        /**
         * ```
         *   △△△△△△△△
         *  ◁ SAMPLE ▷
         *   ▽▽▽▽▽▽▽▽
         * ```
         */
        fun spikedOutward(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(SpikedOutward, padding, margin, ansiCode)


        /**
         * ```
         *  ◸▽▽▽▽▽▽▽▽◹
         *  ▷ SAMPLE ◁
         *  ◺△△△△△△△△◿
         * ```
         */
        fun spikedInward(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            text.wrapWithBorder(Borders.SpikedInward, padding, margin, ansiCode)
    }
}

val CharSequence.draw get() = Draw(this.toString())
val String.draw get() = Draw(this)
