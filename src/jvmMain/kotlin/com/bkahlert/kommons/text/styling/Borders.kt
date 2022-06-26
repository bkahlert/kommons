package com.bkahlert.kommons.text.styling

import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.repeat
import com.bkahlert.kommons.text.styling.Borders.Block
import com.bkahlert.kommons.text.styling.Borders.Double
import com.bkahlert.kommons.text.styling.Borders.Heavy
import com.bkahlert.kommons.text.styling.Borders.HeavyDotted
import com.bkahlert.kommons.text.styling.Borders.Light
import com.bkahlert.kommons.text.styling.Borders.LightDotted
import com.bkahlert.kommons.text.styling.Borders.Rounded
import com.bkahlert.kommons.text.styling.Borders.SpikedInward
import com.bkahlert.kommons.text.styling.Borders.SpikedOutward

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
public enum class Borders(
    /** Top left corner string */
    public val tl: String,
    /** Top center string */
    public val tc: String,
    /** Top right corner string */
    public val tr: String,
    /** Center left string */
    public val cl: String,
    /** Center right string */
    public val cr: String,
    /** Bottom left corner string */
    public val bl: String,
    /** Bottom center string */
    public val bc: String,
    /** Bottom right corner string */
    public val br: String,
) : CharSequence by "$tl$tc$tr$LF$cl $cr$LF$bl$bc$br" {

    /**
     * ```
     *  ┌────────┐
     *  │ SAMPLE │
     *  └────────┘
     * ```
     */
    Light("┌", "─", "┐", "│", "│", "└", "─", "┘"),

    /**
     * ```
     *  ┏━━━━━━━━┓
     *  ┃ SAMPLE ┃
     *  ┗━━━━━━━━┛
     * ```
     */
    Heavy("┏", "━", "┓", "┃", "┃", "┗", "━", "┛"),

    /**
     * ```
     *  ██████████
     *  █ SAMPLE █
     *  ██████████
     * ```
     */
    Block("█", "█", "█", "█", "█", "█", "█", "█"),

    /**
     * ```
     *  ╔════════╗
     *  ║ SAMPLE ║
     *  ╚════════╝
     * ```
     */
    Double("╔", "═", "╗", "║", "║", "╚", "═", "╝"),

    /**
     * ```
     *  ╭────────╮
     *  │ SAMPLE │
     *  ╰────────╯
     * ```
     */
    Rounded("╭", "─", "╮", "│", "│", "╰", "─", "╯"),

    /**
     * ```
     *  ┌┄┄┄┄┄┄┄┄┐
     *  ┊ SAMPLE ┊
     *  └┈┄┄┄┄┄┄┄┘
     * ```
     */
    LightDotted("┌", "┄", "┐", "┊", "┊", "└", "┈", "┘"),

    /**
     * ```
     *  ┏╍╍╍╍╍╍╍╍┓
     *  ┇ SAMPLE ┇
     *  ┗╍╍╍╍╍╍╍╍┛
     * ```
     */
    HeavyDotted("┏", "╍", "┓", "┇", "┇", "┗", "╍", "┛"),

    /**
     * ```
     *  △△△△△△△△
     * ◁ SAMPLE ▷
     *  ▽▽▽▽▽▽▽▽
     * ```
     */
    SpikedOutward(" ", "△", " ", "◁", "▷", " ", "▽", " "),

    /**
     * ```
     *  ◸▽▽▽▽▽▽▽▽◹
     *  ▷ SAMPLE ◁
     *  ◺△△△△△△△△◿
     * ```
     */
    SpikedInward("◸", "▽", "◹", "▷", "◁", "◺", "△", "◿"),
    ;
}


/**
 * Centers this [CharSequence] and the specified [padding] and puts a [formatter] styled [border] (see [Borders] for predefined one) around it.
 * Furthermore, a [margin] can be set to distance the bordered text.
 */
public fun <T : CharSequence> T.wrapWithBorder(
    border: CharSequence = Rounded,
    padding: Int = 2,
    margin: Int = 1,
    formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence,
): String {
    val block = this.lines().center(border[5])
    if (block.isEmpty()) return toString()
    val width = block[0].ansiRemoved.length
    val height = block.size
    val bordered = "" +
        formatter("${border[0]}${border[1].repeat(width + padding * 2)}${border[2]}") + LF +
        (0 until padding / 2).joinToString("") {
            formatter("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}").toString() + LF
        } +
        (0 until height).joinToString("") { y ->
            formatter("${border[4]}${border[5].repeat(padding)}").toString() + block[y] + formatter("${border[5].repeat(padding)}${border[6]}") + LF
        } +
        (0 until padding / 2).joinToString("") {
            formatter("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}").toString() + LF
        } +
        formatter("${border[8]}${border[9].repeat(width + padding * 2)}${border[10]}")
    return if (margin == 0) bordered
    else bordered.wrapWithBorder(border[5].repeat(11), padding = margin - 1, margin = 0, formatter = formatter)
}

/**
 * Centers this list of [CharSequence] and the specified [padding] and puts a [formatter] styled [border] (see [Borders] for predefined one) around it.
 * Furthermore a [margin] can be set to distance the bordered text.
 */
public fun <T : CharSequence> Iterable<T>.wrapWithBorder(
    border: CharSequence = Rounded,
    padding: Int = 2,
    margin: Int = 1,
    formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence,
): String = joinToString(LF).wrapWithBorder(border, padding, margin, formatter)

public class Draw(public val text: CharSequence) {
    public val border: Border get() = Border()

    public inner class Border {
        /**
         * ```
         *  ┌────────┐
         *  │ SAMPLE │
         *  └────────┘
         * ```
         */
        public fun light(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(Light, padding, margin, formatter)

        /**
         * ```
         *  ┏━━━━━━━━┓
         *  ┃ SAMPLE ┃
         *  ┗━━━━━━━━┛
         * ```
         */
        public fun heavy(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(Heavy, padding, margin, formatter)

        /**
         * ```
         *  ██████████
         *  █ SAMPLE █
         *  ██████████
         * ```
         */
        public fun block(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(Block, padding, margin, formatter)

        /**
         * ```
         *  ╔════════╗
         *  ║ SAMPLE ║
         *  ╚════════╝
         * ```
         */
        public fun double(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(Double, padding, margin, formatter)

        /**
         * ```
         *  ╭────────╮
         *  │ SAMPLE │
         *  ╰────────╯
         * ```
         */
        public fun rounded(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(Rounded, padding, margin, formatter)

        /**
         * ```
         *  ┌┄┄┄┄┄┄┄┄┐
         *  ┊ SAMPLE ┊
         *  └┈┄┄┄┄┄┄┄┘
         * ```
         */
        public fun lightDotted(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(LightDotted, padding, margin, formatter)

        /**
         * ```
         *  ┏╍╍╍╍╍╍╍╍┓
         *  ┇ SAMPLE ┇
         *  ┗╍╍╍╍╍╍╍╍┛
         * ```
         */
        public fun heavyDotted(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(HeavyDotted, padding, margin, formatter)

        /**
         * ```
         *   △△△△△△△△
         *  ◁ SAMPLE ▷
         *   ▽▽▽▽▽▽▽▽
         * ```
         */
        public fun spikedOutward(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(SpikedOutward, padding, margin, formatter)


        /**
         * ```
         *  ◸▽▽▽▽▽▽▽▽◹
         *  ▷ SAMPLE ◁
         *  ◺△△△△△△△△◿
         * ```
         */
        public fun spikedInward(padding: Int = 0, margin: Int = 0, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence): String =
            text.wrapWithBorder(SpikedInward, padding, margin, formatter)
    }
}

public val CharSequence.draw: Draw get() = Draw(this.toString())
