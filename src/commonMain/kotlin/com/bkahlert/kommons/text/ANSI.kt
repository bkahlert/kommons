package com.bkahlert.kommons.text

import com.bkahlert.kommons.math.mod
import com.bkahlert.kommons.regex.namedGroups
import com.bkahlert.kommons.runtime.AnsiSupport
import com.bkahlert.kommons.runtime.AnsiSupport.ANSI4
import com.bkahlert.kommons.runtime.AnsiSupport.NONE
import com.bkahlert.kommons.runtime.ansiSupport
import com.bkahlert.kommons.runtime.isDebugging
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.ColoredText
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.AnsiCode.Companion.REGEX
import com.bkahlert.kommons.text.AnsiCodeHelper.closingControlSequence
import com.bkahlert.kommons.text.AnsiCodeHelper.controlSequence
import com.bkahlert.kommons.text.AnsiCodeHelper.parseAnsiCodes
import com.bkahlert.kommons.text.AnsiCodeHelper.unclosedCodes
import com.bkahlert.kommons.text.AnsiString.Companion.tokenize
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.Semantics.formattedAs
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextDouble
import com.bkahlert.kommons.text.Unicode.CONTROL_SEQUENCE_INTRODUCER as c
import com.bkahlert.kommons.text.Unicode.ESCAPE as e
import kotlin.text.contains as containsNonAnsiAware

/**
 * All around [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public object ANSI {

    private val level by lazy { if (isDebugging && false) NONE else ansiSupport }

    /**
     * Contains `this` character sequence with all [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
     */
    public val CharSequence.ansiRemoved: String
        get() = if (this is AnsiString) toString(removeAnsi = true) else REGEX.replace(this, "")

    /**
     * Whether this character sequence contains [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    public val CharSequence.containsAnsi: Boolean
        get() = if (this is AnsiString) containsAnsi else REGEX.containsMatchIn(this)

    /**
     * Returns this character sequence as a string with all lines terminated with
     * an reset escape sequence. This is a hackish way of fixing ANSI escape based graphics that bleed
     * because of mal-formed or mal-interpreted escape sequences.
     *
     * **Example: "Bleeding" logo**
     * ```
     * BBBBBBBBBBQxvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBB&xvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBZzvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBZuvvvvvvvvvvvvvvvvvvvvvv▗▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * BBZTvvvvvvvvvvvvvvvvvvvvvv\.▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * R3vvvvvvvvvvvvvvvvvvvvvv\.   .▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * ```
     *
     * **Example: "Repaired" logo with reset lines**
     * ```
     * BBBBBBBBBBQxvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBB&xvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBZzvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBZuvvvvvvvvvvvvvvvvvvvvvv▗▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * BBZTvvvvvvvvvvvvvvvvvvvvvv\.▝▜MMMMMMMMMMMMMMMMMMMM
     * R3vvvvvvvvvvvvvvvvvvvvvv\.   .▝▜MMMMMMMMMMMMMMMMMM
     * ```
     */
    @Suppress("SpellCheckingInspection")
    public fun CharSequence.resetLines(): String {
        val reset = reset(ANSI4)
        return toString().mapLines { "$it$reset" }
    }

    public fun interface FilteringFormatter<in T> {
        public operator fun invoke(value: T): CharSequence?
        public operator fun plus(other: FilteringFormatter<CharSequence>): FilteringFormatter<T> = FilteringFormatter { invoke(it)?.let(other::invoke) }

        public companion object {

            /**
             * Returns a new formatter that is provided with the initial text freed
             * from any previous formatting and wrapped in a [ANSI.Text] for convenient
             * customizations.
             */
            public fun fromScratch(transform: Text.() -> CharSequence?): FilteringFormatter<Any> =
                FilteringFormatter { it.toString().ansiRemoved.ansi.transform() }

            /**
             * A formatter that applies [Any.toString] to its text if it's not already [CharSequence].
             */
            public val ToCharSequence: FilteringFormatter<Any> = FilteringFormatter { text -> (text as? CharSequence) ?: text.toString() }
        }
    }

    public fun interface Formatter<in T> : FilteringFormatter<T> {
        override operator fun invoke(value: T): CharSequence
        public operator fun plus(other: Formatter<CharSequence>): Formatter<T> = Formatter { invoke(it).let(other::invoke) }

        public companion object {

            /**
             * Returns a new formatter that is provided with the initial text freed
             * from any previous formatting and wrapped in a [ANSI.Text] for convenient
             * customizations.
             */
            public fun fromScratch(transform: Text.() -> CharSequence): Formatter<Any> = Formatter { it.toString().ansiRemoved.ansi.transform() }

            /**
             * A formatter that applies [Any.toString] to its text if it's not already [CharSequence].
             */
            public val ToCharSequence: Formatter<Any> = Formatter { text -> (text as? CharSequence) ?: text.toString() }
        }
    }

    public fun CharSequence.colorize(): String = mapCharacters { Colors.random()(it) }

    public interface Colorizer : Formatter<CharSequence> {
        public val bg: Formatter<CharSequence>
        public fun on(backgroundColorizer: Colorizer): Formatter<CharSequence>
    }

    private open class AnsiCodeFormatter(private val ansiCode: AnsiCode) : Formatter<CharSequence> {
        override fun invoke(value: CharSequence): String = ansiCode.format(value.toString())
        override operator fun plus(other: Formatter<CharSequence>): Formatter<CharSequence> =
            (other as? AnsiCodeFormatter)?.ansiCode?.plus(ansiCode)?.let { AnsiCodeFormatter(it) } ?: super.plus(other)

        override operator fun plus(other: FilteringFormatter<CharSequence>): FilteringFormatter<CharSequence> =
            (other as? AnsiCodeFormatter)?.ansiCode?.plus(ansiCode)?.let { AnsiCodeFormatter(it) } ?: super.plus(other)
    }

    private class AnsiColorCodeFormatter(ansiCode: AnsiColorCode) : Colorizer, AnsiCodeFormatter(ansiCode) {
        override val bg: Formatter<CharSequence> = AnsiCodeFormatter(ansiCode.bg)
        override fun on(backgroundColorizer: Colorizer): Formatter<CharSequence> = this + (backgroundColorizer.bg)
    }

    private fun reset(level: AnsiSupport) = if (level == NONE) DisabledAnsiCode else AnsiCode(0, 0)
    private val RESET: AnsiCode by lazy { reset(level) }

    public object Colors {

        public val black: Colorizer get() = AnsiColorCodeFormatter(ansi16(30))
        public val red: Colorizer get() = AnsiColorCodeFormatter(ansi16(31))
        public val green: Colorizer get() = AnsiColorCodeFormatter(ansi16(32))
        public val yellow: Colorizer get() = AnsiColorCodeFormatter(ansi16(33))
        public val blue: Colorizer get() = AnsiColorCodeFormatter(ansi16(34))
        public val magenta: Colorizer get() = AnsiColorCodeFormatter(ansi16(35))
        public val cyan: Colorizer get() = AnsiColorCodeFormatter(ansi16(36))
        public val white: Colorizer get() = AnsiColorCodeFormatter(ansi16(37))
        public val gray: Colorizer get() = AnsiColorCodeFormatter(ansi16(90))

        public val brightRed: Colorizer get() = AnsiColorCodeFormatter(ansi16(91))
        public val brightGreen: Colorizer get() = AnsiColorCodeFormatter(ansi16(92))
        public val brightYellow: Colorizer get() = AnsiColorCodeFormatter(ansi16(93))
        public val brightBlue: Colorizer get() = AnsiColorCodeFormatter(ansi16(94))
        public val brightMagenta: Colorizer get() = AnsiColorCodeFormatter(ansi16(95))
        public val brightCyan: Colorizer get() = AnsiColorCodeFormatter(ansi16(96))
        public val brightWhite: Colorizer get() = AnsiColorCodeFormatter(ansi16(97))

        /**
         * Creates a [Colorizer] for a random color from the given [hue] and [variance]
         * with a fixed saturation and value.
         *
         * - A variance of 0 will always return the same color.
         * - A variance of 180 will return every possible color.
         *
         * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSL and HSV</a>
         */
        public fun random(hue: Int, variance: Double = 60.0): Colorizer =
            random(hue.toDouble(), variance)

        /**
         * Creates a [Colorizer] for a random color from the given [hue] and [variance]
         * with a fixed saturation and value.
         *
         * - A variance of 0 will always return the same color.
         * - A variance of 180 will return every possible color.
         *
         * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSL and HSV</a>
         */
        public fun random(hue: Double, variance: Double = 60.0): Colorizer =
            random((hue - variance)..(hue + variance))

        /**
         * Creates a [Colorizer] for a random color in the specified [hueRange].
         *
         * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSL and HSV</a>
         */
        public fun random(hueRange: ClosedRange<Double> = 0.0..360.0): Colorizer =
            hsv(nextDouble(hueRange.start, hueRange.endInclusive))

        /**
         * Creates a [Colorizer] for a gray color with the specified [brightness] (0..1).
         *
         * - A brightness of 0.0 will return black.
         * - A brightness of 1.0 will return white.
         *
         * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSL and HSV</a>
         */
        public fun gray(brightness: Double): Colorizer =
            hsv(180.0, 0.0, brightness)

        /**
         * Creates a [Colorizer] for an HSV color for the given [hue] (0..360),
         * [saturation] (0..1) and [value] (0..1).
         *
         * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV">HSL and HSV</a>
         */
        public fun hsv(hue: Double, saturation: Double = .82, value: Double = .89): Colorizer =
            AnsiColorCodeFormatter(hsv(hue.mod(360.0).toInt(), (saturation * 100.0).mod(100.0).toInt(), (value * 100.0).mod(100.0).toInt()))


        private fun Colorizer.format(bg: Colorizer?, text: CharSequence): CharSequence =
            if (bg == null) this(text) else this.on(bg)(text)

        public fun CharSequence.black(backgroundColor: Colorizer? = null): CharSequence = black.format(backgroundColor, this)
        public fun CharSequence.red(backgroundColor: Colorizer? = null): CharSequence = red.format(backgroundColor, this)
        public fun CharSequence.green(backgroundColor: Colorizer? = null): CharSequence = green.format(backgroundColor, this)
        public fun CharSequence.yellow(backgroundColor: Colorizer? = null): CharSequence = yellow.format(backgroundColor, this)
        public fun CharSequence.blue(backgroundColor: Colorizer? = null): CharSequence = blue.format(backgroundColor, this)
        public fun CharSequence.magenta(backgroundColor: Colorizer? = null): CharSequence = magenta.format(backgroundColor, this)
        public fun CharSequence.cyan(backgroundColor: Colorizer? = null): CharSequence = cyan.format(backgroundColor, this)
        public fun CharSequence.white(backgroundColor: Colorizer? = null): CharSequence = white.format(backgroundColor, this)
        public fun CharSequence.gray(backgroundColor: Colorizer? = null): CharSequence = gray.format(backgroundColor, this)

        public fun CharSequence.brightRed(backgroundColor: Colorizer? = null): CharSequence = brightRed.format(backgroundColor, this)
        public fun CharSequence.brightGreen(backgroundColor: Colorizer? = null): CharSequence = brightGreen.format(backgroundColor, this)
        public fun CharSequence.brightYellow(backgroundColor: Colorizer? = null): CharSequence = brightYellow.format(backgroundColor, this)
        public fun CharSequence.brightBlue(backgroundColor: Colorizer? = null): CharSequence = brightBlue.format(backgroundColor, this)
        public fun CharSequence.brightMagenta(backgroundColor: Colorizer? = null): CharSequence = brightMagenta.format(backgroundColor, this)
        public fun CharSequence.brightCyan(backgroundColor: Colorizer? = null): CharSequence = brightCyan.format(backgroundColor, this)
        public fun CharSequence.brightWhite(backgroundColor: Colorizer? = null): CharSequence = brightWhite.format(backgroundColor, this)

        /** @param hex An rgb hex string in the form "#ffffff" or "ffffff" */
        private fun rgb(hex: String): AnsiColorCode = color(RGB(hex))

        /**
         * Create a color code from an RGB color.
         *
         * @param r The red amount, in the range \[0, 255]
         * @param g The green amount, in the range \[0, 255]
         * @param b The blue amount, in the range \[0, 255]
         */
        private fun rgb(r: Int, g: Int, b: Int): AnsiColorCode = color(RGB(r, g, b))

        /**
         * Create a color code from an HSV color.
         *
         * @param h The hue, in the range \[0, 360]
         * @param s The saturation, in the range \[0,100]
         * @param v The value, in the range \[0,100]
         */
        private fun hsv(h: Int, s: Int, v: Int): AnsiColorCode = color(HSV(h, s, v))

        private fun ansi16(code: Int): AnsiColorCode =
            if (level == NONE) DisabledAnsiColorCode else Ansi16ColorCode(code)

        /**
         * Create a color from an existing [Color].
         *
         * It's usually easier to use a function like [rgb] or [hsv] instead.
         */
        private fun color(color: Color): AnsiColorCode = when (level) {
            NONE -> DisabledAnsiColorCode
            ANSI4 -> Ansi16ColorCode(color.toAnsi16().code)
            AnsiSupport.ANSI8 ->
                if (color is Ansi16) Ansi16ColorCode(color.code)
                else Ansi256ColorCode(color.toAnsi256().code)
            AnsiSupport.ANSI24 -> when (color) {
                is Ansi16 -> Ansi16ColorCode(color.code)
                is Ansi256 -> Ansi256ColorCode(color.code)
                else -> color.toRGB().run { AnsiRGBColorCode(r, g, b) }
            }
        }
    }

    public object Style {

        public val bold: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(1, 22))
        public val dim: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(2, 22))
        public val italic: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(3, 23))
        public val underline: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(4, 24))
        public val inverse: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(7, 27))
        public val hidden: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(8, 28))
        public val strikethrough: Formatter<CharSequence> get() = AnsiCodeFormatter(ansi(9, 29))

        public fun CharSequence.bold(): CharSequence = bold(this)
        public fun CharSequence.dim(): CharSequence = dim(this)
        public fun CharSequence.italic(): CharSequence = italic(this)

        private fun ansi(open: Int, close: Int) =
            if (level == NONE) DisabledAnsiCode else AnsiCode(open, close)
    }

    public open class Preview(
        protected val text: CharSequence,
        protected open val formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence,
        public val done: String = formatter(text).toString(),
    ) : CharSequence by done {
        override fun toString(): String = done
    }

    public interface Colorable<T : CharSequence> {
        public fun color(colorizer: Colorizer): T

        public val black: T get() = color(Colors.black)
        public val red: T get() = color(Colors.red)
        public val green: T get() = color(Colors.green)
        public val yellow: T get() = color(Colors.yellow)
        public val blue: T get() = color(Colors.blue)
        public val magenta: T get() = color(Colors.magenta)
        public val cyan: T get() = color(Colors.cyan)
        public val white: T get() = color(Colors.white)
        public val gray: T get() = color(Colors.gray)

        public val brightRed: T get() = color(Colors.brightRed)
        public val brightGreen: T get() = color(Colors.brightGreen)
        public val brightYellow: T get() = color(Colors.brightYellow)
        public val brightBlue: T get() = color(Colors.brightBlue)
        public val brightMagenta: T get() = color(Colors.brightMagenta)
        public val brightCyan: T get() = color(Colors.brightCyan)
        public val brightWhite: T get() = color(Colors.brightWhite)

        public val random: T get() = random()
        public fun random(hue: Int, variance: Double = 60.0): T = color(Colors.random(hue, variance))
        public fun random(hue: Double, variance: Double = 60.0): T = color(Colors.random(hue, variance))
        public fun random(range: ClosedRange<Double> = 0.0..360.0): T = color(Colors.random(range))
        public fun gray(brightness: Double): T = color(Colors.gray(brightness))
        public fun hsv(hue: Double, saturation: Double = .82, value: Double = .89): T = color(Colors.hsv(hue, saturation, value))
    }

    public interface Styleable<T : CharSequence> {
        public fun style(formatter: Formatter<CharSequence>): T
        public fun style(formatter: FilteringFormatter<CharSequence>): T?

        public val bold: T get() = style(Style.bold)
        public val dim: T get() = style(Style.dim)
        public val italic: T get() = style(Style.italic)
        public val underline: T get() = style(Style.underline)
        public val inverse: T get() = style(Style.inverse)
        public val hidden: T get() = style(Style.hidden)
        public val strikethrough: T get() = style(Style.strikethrough)
    }

    public class Text private constructor(text: CharSequence, formatter: FilteringFormatter<CharSequence> = FilteringFormatter.ToCharSequence) :
        Preview(text, formatter),
        Colorable<ColoredText>,
        Styleable<Text> {
        override fun color(colorizer: Colorizer): ColoredText = ColoredText(text, colorizer)
        override fun style(formatter: Formatter<CharSequence>): Text = Text(formatter(text))
        override fun style(formatter: FilteringFormatter<CharSequence>): Text? = formatter(text)?.let(::Text)

        public class ColoredText(text: CharSequence, private val colorizer: Colorizer) : Preview(text, colorizer),
            Styleable<Text> {
            override fun style(formatter: Formatter<CharSequence>): Text {
                val formatter1: Formatter<CharSequence> = colorizer + formatter
                return Text(text, formatter1)
            }

            override fun style(formatter: FilteringFormatter<CharSequence>): Text = Text(text, colorizer + formatter)
            public val bg: Text get() = Text(text, colorizer.bg)
            public val on: ForegroundColoredText get() = ForegroundColoredText(text, colorizer)
        }

        public class ForegroundColoredText(private val text: CharSequence, private val fg: Colorizer) : Colorable<Text> {
            override fun color(colorizer: Colorizer): Text = Text(text, fg.on(colorizer))
        }

        public companion object {
            public val CharSequence.ansi: Text get() = Text(this)
        }
    }

    public class Controls {

        /**
         * Create an ANSI code to move the cursor up [count] cells.
         *
         * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
         * If [count] is negative, the cursor will be moved down instead.
         */
        public fun cursorUp(count: Int): String = moveCursor(if (count < 0) "B" else "A", abs(count))

        /**
         * Create an ANSI code to move the cursor down [count] cells.
         *
         * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
         * If [count] is negative, the cursor will be moved up instead.
         */
        public fun cursorDown(count: Int): String = moveCursor(if (count < 0) "A" else "B", abs(count))

        /**
         * Create an ANSI code to move the cursor left [count] cells.
         *
         * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
         * If [count] is negative, the cursor will be moved right instead.
         */
        public fun cursorLeft(count: Int): String = moveCursor(if (count < 0) "C" else "D", abs(count))

        /**
         * Create an ANSI code to move the cursor right [count] cells.
         *
         * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
         * If [count] is negative, the cursor will be moved left instead.
         */
        public fun cursorRight(count: Int): String = moveCursor(if (count < 0) "D" else "C", abs(count))

        /**
         * Create an ANSI code to hide the cursor.
         *
         * If ANSI codes are not supported, an empty string is returned.
         */
        public val hideCursor: String get() = if (level == NONE) "" else "$c?25l"

        /**
         * Create an ANSI code to show the cursor.
         *
         * If ANSI codes are not supported, an empty string is returned.
         */
        public val showCursor: String get() = if (level == NONE) "" else "$c?25h"

        private fun moveCursor(dir: String, count: Int): String {
            return if (count == 0 || level == NONE) ""
            else "$c$count$dir"
        }
    }
}

private val ansiCloseRe = Regex("""$e\[((?:\d{1,3};?)+)m""")

/**
 * A class representing one or more numeric ANSI codes.
 *
 * @property codes A list of pairs, with each pair being the list of opening codes and a closing code.
 */
internal open class AnsiCode(val codes: List<Pair<List<Int>, Int>>) {
    constructor(openCodes: List<Int>, closeCode: Int) : this(listOf(openCodes to closeCode))
    constructor(openCode: Int, closeCode: Int) : this(listOf(openCode), closeCode)

    val open: String get() = tag(codes.flatMap { it.first })
    val close: String get() = tag(codes.map { it.second })

    override fun toString() = open

    fun format(text: CharSequence): String = if (text.isEmpty()) "" else open + nest(text) + close

    open operator fun plus(other: AnsiCode) = AnsiCode(codes + other.codes)

    private fun nest(text: CharSequence): String = ansiCloseRe.replace(text) {
        // Replace instances of our close codes with their corresponding opening codes. If the close
        // code is at the end of the text, omit it instead so that we don't open and immediately
        // close a command.
        val openCodesByCloseCode = HashMap<Int, List<Int>>()
        for ((o, c) in codes) openCodesByCloseCode[c] = o
        val atEnd = it.range.endInclusive == text.lastIndex
        val codes: Sequence<Int> = it.groupValues[1].splitToSequence(';').flatMap {
            it.toIntOrNull().let {
                if (it == null || (atEnd && it in openCodesByCloseCode)) emptySequence()
                else (openCodesByCloseCode[it]?.asSequence() ?: sequenceOf(it))
            }
        }

        tag(codes.toList())
    }

    private fun tag(c: List<Int>) = if (c.isEmpty()) "" else "$e[${c.joinToString(";")}m"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as AnsiCode
        return codes == other.codes
    }

    override fun hashCode() = codes.hashCode()

    companion object {

        /**
         * [Regex] that matches an [AnsiCode].
         */
        val REGEX: Regex = Regex("(?<CSI>$c\\[|$e\\[)(?<parameterBytes>[0-?]*)(?<intermediateBytes>[ -/]*)(?<finalByte>[@-~])")
    }
}

private object DisabledAnsiCode : AnsiCode(emptyList()) {
    override fun plus(other: AnsiCode): AnsiCode = this
}

/**
 * A class representing one or more ANSI color codes.
 *
 * @property codes A list of pairs, with each pair being the list of opening codes and a closing code.
 */
internal abstract class AnsiColorCode(codes: List<Pair<List<Int>, Int>>) : AnsiCode(codes) {
    constructor(openCodes: List<Int>, closeCode: Int) : this(listOf(openCodes to closeCode))
    constructor(openCode: Int, closeCode: Int) : this(listOf(openCode), closeCode)

    /**
     * Get a color for background only.
     *
     * Note that if you want to specify both a background and foreground color, use [on] instead of
     * this property.
     */
    val bg: AnsiCode get() = AnsiCode(bgCodes)

    open infix fun on(bg: AnsiColorCode): AnsiCode {
        return AnsiCode(codes + bg.bgCodes)
    }

    protected abstract val bgCodes: List<Pair<List<Int>, Int>>
}

private object EmptyAnsiColorCode : AnsiColorCode(emptyList()) {
    override val bgCodes: List<Pair<List<Int>, Int>> get() = emptyList()
    override fun plus(other: AnsiCode): AnsiCode = other
    override fun on(bg: AnsiColorCode): AnsiCode = bg
}

private object DisabledAnsiColorCode : AnsiColorCode(emptyList()) {
    override val bgCodes: List<Pair<List<Int>, Int>> get() = emptyList()
    override fun plus(other: AnsiCode): AnsiCode = this
    override fun on(bg: AnsiColorCode): AnsiCode = DisabledAnsiCode
}

internal class Ansi16ColorCode(code: Int) : AnsiColorCode(code, 39) {
    override val bgCodes get() = codes.map { listOf(it.first[0] + 10) to 49 }
}

internal class Ansi256ColorCode(code: Int) : AnsiColorCode(listOf(38, 5, code), 39) {
    override val bgCodes get() = codes.map { listOf(48, 5, it.first[2]) to 49 }
}

internal class AnsiRGBColorCode(r: Int, g: Int, b: Int) : AnsiColorCode(listOf(38, 2, r, g, b), 39) {
    override val bgCodes get() = codes.map { (o, _) -> listOf(48, 2, o[2], o[3], o[4]) to 49 }
}


/**
 * An ANSI-16 color code
 */
private data class Ansi16(val code: Int) : Color {
    init {
        require(code in 30..37 || code in 40..47 ||
            code in 90..97 || code in 100..107) {
            "code not valid: $code"
        }
    }

    companion object {
        val black: Ansi16 get() = Ansi16(30)
        val red: Ansi16 get() = Ansi16(31)
        val green: Ansi16 get() = Ansi16(32)
        val yellow: Ansi16 get() = Ansi16(33)
        val blue: Ansi16 get() = Ansi16(34)
        val purple: Ansi16 get() = Ansi16(35)
        val cyan: Ansi16 get() = Ansi16(36)
        val white: Ansi16 get() = Ansi16(37)

        val brightBlack: Ansi16 get() = Ansi16(90)
        val brightRed: Ansi16 get() = Ansi16(91)
        val brightGreen: Ansi16 get() = Ansi16(92)
        val brightYellow: Ansi16 get() = Ansi16(93)
        val brightBlue: Ansi16 get() = Ansi16(94)
        val brightPurple: Ansi16 get() = Ansi16(95)
        val brightCyan: Ansi16 get() = Ansi16(96)
        val brightWhite: Ansi16 get() = Ansi16(97)
    }

    override fun toRGB(): RGB {
        val color = code % 10

        // grayscale
        if (color == 0 || color == 7) {
            val c: Double =
                if (code > 50) color + 3.5
                else color.toDouble()

            val v = (c / 10.5 * 255).roundToInt()

            return RGB(v, v, v)
        }

        // color
        val mul = if (code > 50) 1.0 else 0.5
        val r = ((color % 2) * mul)
        val g = (((color / 2) % 2) * mul)
        val b = (((color / 4) % 2) * mul)

        return RGB(r, g, b)
    }

    override fun toAnsi16() = this
    override fun toAnsi256() = when {
        code >= 90 -> Ansi256(code - 90 + 8)
        else -> Ansi256(code - 30)
    }
}

/**
 * An ANSI-256 color code
 */
private data class Ansi256(val code: Int) : Color {
    init {
        check(code in 0..255) { "code must be in range [0,255]: $code" }
    }

    override fun toRGB(): RGB {
        // ansi16 colors
        if (code < 16) return toAnsi16().toRGB()

        // grayscale
        if (code >= 232) {
            val c = (code - 232) * 10 + 8
            return RGB(c, c, c)
        }

        // color
        val c = code - 16
        val rem = c % 36
        val r = floor(c / 36.0) / 5.0
        val g = floor(rem / 6.0) / 5.0
        val b = (rem % 6) / 5.0
        return RGB(r, g, b)
    }

    override fun toAnsi256() = this

    // 0-7 are standard ansi16 colors
    // 8-15 are bright ansi16 colors
    override fun toAnsi16() = when {
        code < 8 -> Ansi16(code + 30)
        code < 16 -> Ansi16(code - 8 + 90)
        else -> toRGB().toAnsi16()
    }
}


/**
 * A color that can be converted to other representations.
 *
 * The conversion functions can return the object they're called on if it is already in the
 * correct format.
 *
 * Note that there is not a direct conversion between every pair of representations. In those cases,
 * the values may be converted through one or more intermediate representations. This may cause a
 * loss of precision.
 *
 * All colors have an [alpha] value, which is the opacity of the color. If a colorspace doesn't
 * support an alpha channel, the value 1 (fully opaque) is used.
 */
private interface Color {
    /** The opacity of this color, in the range `[0, 1]`. */
    val alpha: Float get() = 1f

    /** Convert this color to Red-Green-Blue (using sRGB color space) */
    fun toRGB(): RGB

    /**
     * Convert this color to an RGB hex string.
     *
     * If [renderAlpha] is `ALWAYS`, the [alpha] value will be added e.g. the `aa` in `#ffffffaa`.
     * If it's `NEVER`, the [alpha] will be omitted. If it's `NEVER`, then the [alpha] will be added
     * if it's less than 1.
     *
     * @return A string in the form `"#ffffff"` if [withNumberSign] is true,
     *     or in the form `"ffffff"` otherwise.
     */
    fun toHex(
        withNumberSign: Boolean = true,
        renderAlpha: RenderCondition = RenderCondition.AUTO,
    ): String = toRGB().toHex(withNumberSign)

    /** Convert this color to Hue-Saturation-Value */
    fun toHSV(): HSV = toRGB().toHSV()

    /** Convert this color to a 16-color ANSI code */
    fun toAnsi16(): Ansi16 = toRGB().toAnsi16()

    /** Convert this color to a 256-color ANSI code */
    fun toAnsi256(): Ansi256 = toRGB().toAnsi256()

    companion object
}

private enum class RenderCondition {
    /** Always show the value */
    ALWAYS,

    /** Never show the value */
    NEVER,

    /** Only show the value if it differs from its default */
    AUTO
}

/**
 * A color in the sRGB color space
 *
 * @property r The red channel, a value in the range `[0, 255]`
 * @property g The green channel, a value in the range `[0, 255]`
 * @property b The blue channel, a value in the range `[0, 255]`
 * @property a The alpha channel, a value in the range `[0f, 1f]`
 */
private data class RGB(val r: Int, val g: Int, val b: Int, val a: Float = 1f) : Color {
    companion object {
        /**
         * Create an [RGB] instance from a packed (a)rgb integer, such as those returned from
         * `android.graphics.Color.argb` or `java.awt.image.BufferedImage.getRGB`.
         */
        fun fromInt(argb: Int): RGB = RGB(
            r = (argb ushr 16) and 0xff,
            g = (argb ushr 8) and 0xff,
            b = (argb) and 0xff,
            a = ((argb ushr 24) and 0xff) / 255f)
    }

    init {
        require(r in 0..255) { "r must be in range [0, 255] in $this" }
        require(g in 0..255) { "g must be in range [0, 255] in $this" }
        require(b in 0..255) { "b must be in range [0, 255] in $this" }
        require(a in 0f..1f) { "a must be in range [0, 1] in $this" }
    }

    /**
     * Construct an RGB instance from a hex string with optional alpha channel.
     *
     * @param hex An rgb hex string in the form "#ffffff" or "ffffff", or an rgba hex string in the
     *   form "#ffffffaa", or "ffffaa"
     */
    constructor(hex: String) : this(
        r = hex.validateHex().parseHex(0),
        g = hex.parseHex(2),
        b = hex.parseHex(4),
        a = if (hex.length < 8) 1f else hex.parseHex(6) / 255f
    )

    /**
     * Construct an RGB instance from [Byte] values.
     *
     * The signed byte values will be translated into the range `[0, 255]`
     */
    constructor(r: Byte, g: Byte, b: Byte) : this(r + 128, g + 128, b + 128)

    /**
     * Construct an RGB instance from Float values in the range `[0, 1]`.
     */
    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(
        r = (r * 255).roundToInt(),
        g = (g * 255).roundToInt(),
        b = (b * 255).roundToInt(),
        a = a
    )

    /**
     * Construct an RGB instance from Double values in the range `[0, 1]`.
     */
    constructor(r: Double, g: Double, b: Double, a: Double = 1.0) : this(
        r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat()
    )

    override val alpha: Float get() = a

    /**
     * Return this color as a packed ARGB integer, such as those returned from
     * `android.graphics.Color.argb` or `java.awt.image.BufferedImage.getRGB`.
     */
    fun toPackedInt(): Int {
        return (a * 0xff).roundToInt() shl 24 or (r shl 16) or (g shl 8) or b
    }

    override fun toHex(withNumberSign: Boolean, renderAlpha: RenderCondition): String = buildString(9) {
        if (withNumberSign) append('#')
        append(r.renderHex()).append(g.renderHex()).append(b.renderHex())
        if (renderAlpha == RenderCondition.ALWAYS || renderAlpha == RenderCondition.AUTO && a < 1) {
            append((a * 255).roundToInt().renderHex())
        }
    }

    override fun toHSV(): HSV {
        val r = this.r.toDouble()
        val g = this.g.toDouble()
        val b = this.b.toDouble()
        val min = minOf(r, g, b)
        val max = maxOf(r, g, b)
        val delta = max - min

        val s = when (max) {
            0.0 -> 0.0
            else -> (delta / max * 1000) / 10
        }

        var h = when {
            max == min -> 0.0
            r == max -> (g - b) / delta
            g == max -> 2 + (b - r) / delta
            b == max -> 4 + (r - g) / delta
            else -> 0.0
        }

        h = minOf(h * 60, 360.0)

        if (h < 0) {
            h += 360
        }

        val v = ((max / 255) * 1000) / 10

        return HSV(h.roundToInt(), s.roundToInt(), v.roundToInt(), alpha)
    }

    override fun toAnsi16(): Ansi16 = toAnsi16(toHSV().v)

    private fun toAnsi16(value: Int): Ansi16 {
        if (value == 30) return Ansi16(30)
        val v = (value / 50.0).roundToInt()

        val ansi = 30 +
            ((b / 255.0).roundToInt() * 4
                or ((g / 255.0).roundToInt() * 2)
                or (r / 255.0).roundToInt())
        return Ansi16(if (v == 2) ansi + 60 else ansi)
    }

    override fun toAnsi256(): Ansi256 {
        // grayscale
        val code = if (r == g && g == b) {
            when {
                r < 8 -> 16
                r > 248 -> 231
                else -> (((r - 8) / 247.0) * 24.0).roundToInt() + 232
            }
        } else {
            16 + (36 * (r / 255.0 * 5).roundToInt()) +
                (6 * (g / 255.0 * 5).roundToInt()) +
                (b / 255.0 * 5).roundToInt()
        }
        return Ansi256(code)
    }

    override fun toRGB() = this
}

private fun Int.renderHex() = toString(16).padStart(2, '0')
private fun String.validateHex() = apply {
    require(if (startsWith('#')) length == 7 || length == 9 else length == 6 || length == 8) {
        "Hex string must be in the format \"#ffffff\" or \"ffffff\""
    }
}

private fun String.parseHex(startIndex: Int): Int {
    val i = if (this[0] == '#') startIndex + 1 else startIndex
    return slice(i..i + 1).toInt(16)
}

/**
 * A color in the Hue-Saturation-Value color space.
 *
 * @property h The hue, as degrees in the range `[0, 360]`
 * @property s The saturation, as a percent in the range `[0, 100]`
 * @property v The value, as a percent in the range `[0, 100]`
 * @property a The alpha, as a fraction in the range `[0, 1]`
 */
private data class HSV(override val h: Int, val s: Int, val v: Int, val a: Float = 1f) : Color, HueColor {
    init {
        require(h in 0..360) { "h must be in range [0, 360] in $this" }
        require(s in 0..100) { "s must be in range [0, 100] in $this" }
        require(v in 0..100) { "v must be in range [0, 100] in $this" }
        require(a in 0f..1f) { "a must be in range [0, 1] in $this" }
    }

    override val alpha: Float get() = a

    override fun toRGB(): RGB {
        val h = h.toDouble() / 60
        val s = s.toDouble() / 100
        var v = v.toDouble() / 100
        val hi = floor(h) % 6

        val f = h - floor(h)
        val p = 255 * v * (1 - s)
        val q = 255 * v * (1 - (s * f))
        val t = 255 * v * (1 - (s * (1 - f)))
        v *= 255

        val (r, g, b) = when (hi.roundToInt()) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return RGB(r.roundToInt(), g.roundToInt(), b.roundToInt(), alpha)
    }

    override fun toHSV() = this
}

private interface HueColor {
    /** The hue, as degrees in the range `[0, 360]` */
    val h: Int
}

/** Convert this color's hue to gradians (360° == 400 gradians) */
private fun HueColor.hueAsGrad(): Float = h * 200 / 180f

/** Convert this color's hue to radians (360° == 2π radians) */
private fun HueColor.hueAsRad(): Float = (h * PI / 180).toFloat()

/** Convert this color's hue to turns (360° == 1 turn) */
private fun HueColor.hueAsTurns(): Float = h / 360f


private object AnsiStringCache {
    fun getOrPut(charSequence: CharSequence?): AnsiString = when {
        charSequence is AnsiString -> charSequence
        charSequence.isNullOrEmpty() -> AnsiString.EMPTY
        else -> charSequence.tokenize()
    }
}

@JvmInline
public value class Token private constructor(private val token: Pair<CharSequence, Int>) {
    public val content: CharSequence get() = token.first
    public val logicalLength: Int get() = token.second
    public val isEscapeSequence: Boolean get() = logicalLength == 0

    public operator fun component1(): CharSequence = content
    public operator fun component2(): Int = logicalLength

    public companion object {
        public fun escapeSequence(text: CharSequence): Token = Token(text to 0)
        public fun text(text: CharSequence): Token = Token(text to text.length)
    }
}

private object TokenizationCache {
    private val cache = mutableMapOf<Int, List<AnsiString>>()
    fun getOrPut(text: CharSequence, block: () -> AnsiString): AnsiString {
        val hashCode = text.hashCode()
        val matches = cache.getOrElse(hashCode) { emptyList() }
        val renderedText = text.toString()
        return matches.find { match -> match.toString() == renderedText }
            ?: block().also { cache[hashCode] = matches + it }
    }
}

/**
 * A character sequence which is [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) aware
 * and which does not break any sequence.
 *
 * The behaviour is as follows:
 * - escape sequences have length 0, that is, an [AnsiString] has the same length as its [String] counterpart
 * - [get] returns the unformatted char at the specified index
 * - [subSequence] returns the same character sequence as an unformatted [String] would do—but with the formatting ANSI escape sequences intact.
 * the sub sequence. Also escape sequences are ignored from [length].
 */
public open class AnsiString(internal val tokens: Array<out Token> = emptyArray()) : CharSequence {

    public companion object {
        public val EMPTY: AnsiString = AnsiString()

        public fun Any?.toAnsiString(): AnsiString = AnsiStringCache.getOrPut((this as? CharSequence?) ?: toString())

        public fun CharSequence.tokenize(): AnsiString = TokenizationCache.getOrPut(this) {
            val tokens = mutableListOf<Token>()
            val codes = mutableListOf<IntArray>()
            var consumed = 0
            while (consumed < length) {
                val match = REGEX.find(this, consumed)
                val range = match?.range
                if (range?.first == consumed) {
                    val escapeSequence = this.subSequence(consumed, match.range.last + 1).also {
                        val currentCodes = AnsiCodeHelper.parseAnsiCode(match)
                        codes.addAll(currentCodes)
                        consumed += it.length
                    }
                    if (escapeSequence.isNotEmpty()) tokens.add(Token.escapeSequence(escapeSequence))
                } else {
                    val first: Int? = range?.first
                    val ansiAhead = if (first != null) {
                        first < length
                    } else false
                    tokens.add(Token.text(subSequence(consumed, if (ansiAhead) first!! else length).also {
                        consumed += it.length
                    }))
                }
            }
            AnsiString(tokens.toTypedArray())
        }
    }

    public val containsAnsi: Boolean = tokens.any { it.isEscapeSequence }

    public val ansiLength: Int get():Int = tokens.sumOf { it.logicalLength }

    /**
     * Contains this [string] with all ANSI escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    public val ansiRemoved: String by lazy { tokens.filter { !it.isEscapeSequence }.joinToString("") { it.content } }

    /**
     * Returns the logical length of this string. That is, the same length as the unformatted [String] would return.
     */
    override val length: Int by lazy { ansiRemoved.length }

    /**
     * Returns the plain char at the specified [index].
     *
     * Due to the limitation of a [Char] to two byte no formatted [Char] can be returned.
     */
    override fun get(index: Int): Char = ansiRemoved[index]

    /**
     * Returns the same character sequence as an unformatted [String.subSequence] would do.
     *
     * Sole difference: The formatting ANSI escape sequences are kept.
     * Eventually open sequences will be closes at the of the sub sequence.
     */
    override fun subSequence(startIndex: Int, endIndex: Int): AnsiString {
        val firstToEnd: String = ansiSubSequence(endIndex).first
        val x = if (startIndex > 0) {
            val (prefix, unclosedCodes) = ansiSubSequence(startIndex)
            val controlSequence: String = controlSequence(unclosedCodes.flatMap { it.toList() })
            val startIndex1 = prefix.length - closingControlSequence(unclosedCodes).length
            controlSequence + firstToEnd.subSequence(startIndex1, firstToEnd.length)
        } else {
            firstToEnd
        }
        return x.toAnsiString()
    }

    private fun ansiSubSequence(endIndex: Int): Pair<String, List<IntArray>> {
        if (endIndex == 0) return "" to emptyList()
        if (endIndex > ansiLength) throw IndexOutOfBoundsException("$endIndex must not be greater than $ansiLength")
        var read = 0
        val codes = mutableListOf<IntArray>()
        val sb = StringBuilder()

        tokens.forEach { (content, logicalLength) ->
            val needed = endIndex - read
            if (needed > 0 && logicalLength == 0) {
                sb.append(content)
                codes.addAll(content.parseAnsiCodes())
            } else {
                if (needed <= logicalLength) {
                    sb.append(content.subSequence(0, needed))
                    return@ansiSubSequence "$sb" + closingControlSequence(codes) to unclosedCodes(codes)
                }
                sb.append(content)
                read += logicalLength
            }
        }
        error("must not happen")
    }

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is blank (≝ is empty or consists of nothing but whitespaces).
     */
    public fun isBlank(): Boolean = ansiRemoved.isBlank()

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is not blank (≝ is not empty and consists of at least one non-whitespace).
     */
    public fun isNotBlank(): Boolean = ansiRemoved.isNotBlank()

    public fun toString(removeAnsi: Boolean = false): String =
        if (!removeAnsi) tokens.joinToString("") { it.content }
        else tokens.filter { !it.isEscapeSequence }.joinToString("") { it.content }

    override fun toString(): String = toString(false)


    /**
     * Returns a ANSI string with content of this ANSI string padded at the beginning
     * to the specified [length] with the specified character or space.
     *
     * @param length the desired string length.
     * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
     * @return Returns an ANSI string of length at least [length] consisting of `this` ANSI string prepended with [padChar] as many times
     * as are necessary to reach that length.
     */
    public fun CharSequence.padStart(length: Int, padChar: Char = ' '): CharSequence {
        require(length >= 0) { "Desired length $length is less than zero." }
        return if (length <= this.length) this.subSequence(0, this.length)
        else "$padChar".repeat(length - this.length) + this
    }

    /**
     * Returns an ANSI string with content of this ANSI string padded at the end
     * to the specified [length] with the specified character or space.
     *
     * @param length the desired string length.
     * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
     * @return Returns an ANSI string of length at least [length] consisting of `this` ANSI string appended with [padChar] as many times
     * as are necessary to reach that length.
     */
    public fun padEnd(length: Int, padChar: Char = ' '): AnsiString {
        require(length >= 0) { "Desired length $length is less than zero." }
        return if (length <= this.length) this.subSequence(0, this.length)
        else this + "$padChar".repeat(length - this.length)
    }

    /**
     * Returns a subsequence of this ANSI sequence with the first [n] characters removed.
     *
     * @throws IllegalArgumentException if [n] is negative.
     */
    public fun drop(n: Int): AnsiString {
        require(n >= 0) { "Requested character count ${n.formattedAs.input} is less than zero." }
        return subSequence(n.coerceAtMost(length), length)
    }

    /**
     * Returns a sequence of strings of which each but the last has [size].
     */
    public fun chunkedSequence(size: Int): Sequence<AnsiString> {
        require(size > 0) { "Requested size ${size.formattedAs.input} must be positive." }
        var processed = 0
        var unprocessed = length
        return generateSequence {
            if (unprocessed <= 0) {
                null
            } else {
                val take = size.coerceAtMost(unprocessed)
                subSequence(processed, processed + take).also {
                    processed += take
                    unprocessed -= take
                }
            }
        }
    }

    public fun chunkedByColumnsSequence(columns: Int): Sequence<AnsiString> =
        chunkedByColumnsSequence(columns) { it.toAnsiString() }

    public operator fun plus(other: CharSequence): AnsiString {
        val otherTokens = if (other is AnsiString) other.tokens else other.toString().tokenize().tokens
        return AnsiString(arrayOf(*tokens, *otherTokens))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AnsiString

        if (!tokens.contentEquals(other.tokens)) return false

        return true
    }

    override fun hashCode(): Int = tokens.hashCode()
}

/**
 * Returns if this character sequence contains the specified [other] [CharSequence] as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 * @param ignoreAnsi ANSI formatting / escapes are ignored by default. Use `false` consider escape codes as well
 */
public fun <T : CharSequence> T.contains(
    other: CharSequence,
    ignoreCase: Boolean = false,
    ignoreAnsi: Boolean = false,
): Boolean =
    if (ignoreAnsi) ansiRemoved.containsNonAnsiAware(other.ansiRemoved, ignoreCase)
    else containsNonAnsiAware(other, ignoreCase)

private object AnsiCodeHelper {

    const val splitCodeMarker: String = "👈 ansi code splitter 👉"

    /**
     * Returns the control sequence needed to close all [codes] that are
     * not already closed in the list.
     *
     * Think of it as a bunch of HTML tags of which a few have not been closed,
     * whereas this function returns the string that renders the HTML valid again.
     */
    fun closingControlSequence(codes: List<IntArray>): String = controlSequence(closingCodes(unclosedCodes(codes)))

    /**
     * Iterates through the codes and returns the ones that have no closing counterpart.
     *
     * Think of it as a bunch of HTML tags of which a few have not been closed,
     * whereas this function returns those tags that render the HTML invalid.
     */
    fun unclosedCodes(codes: List<IntArray>): List<IntArray> {
        val unclosedCodes = mutableListOf<IntArray>()
        codes.forEach { code: IntArray ->
            closingToOpeningCodes[code.first()]?.let { openingCodes ->
                unclosedCodes.removeAll { unclosedCode ->
                    openingCodes.any { it == unclosedCode.first() }
                }
            } ?: openingToClosingCodes[code.first()]?.let { closingCodes ->
                if (code.first() !in closingCodes) {
                    unclosedCodes.add(code)
                }
            }
        }
        return unclosedCodes
    }

    /**
     * Returns the codes needed to close the given ones.
     */
    fun closingCodes(codes: List<IntArray>): List<Int> =
        codes.flatMap { openCode: IntArray -> openingToClosingCodes[openCode.first()]?.toList() ?: emptyList() }

    /**
     * Returns the rendered control sequence for the given codes.
     */
    fun controlSequence(codes: List<Int>): String =
        AnsiCode(codes, 0).format(splitCodeMarker).split(splitCodeMarker)[0]

    /**
     * A map that maps the open and close codes of all supported instances of [AnsiCodeHelper]
     * to their respective [AnsiCodeHelper].
     */
    val openingToClosingCodes: Map<Int, IntArray> = mapOf(
        0 to intArrayOf(0),
        1 to intArrayOf(22), // sic!
        2 to intArrayOf(22),
        3 to intArrayOf(23),
        4 to intArrayOf(24),
        5 to intArrayOf(25),
        6 to intArrayOf(25),
        7 to intArrayOf(27),
        8 to intArrayOf(28),
        9 to intArrayOf(29),
        10 to intArrayOf(10),
        11 to intArrayOf(10),
        12 to intArrayOf(10),
        13 to intArrayOf(10),
        14 to intArrayOf(10),
        15 to intArrayOf(10),
        16 to intArrayOf(10),
        17 to intArrayOf(10),
        18 to intArrayOf(10),
        19 to intArrayOf(10),
        20 to intArrayOf(0),
        21 to intArrayOf(0),
        22 to intArrayOf(22),
        23 to intArrayOf(23),
        24 to intArrayOf(24),
        25 to intArrayOf(25),
        26 to intArrayOf(26),
        27 to intArrayOf(27),
        28 to intArrayOf(28),
        29 to intArrayOf(29),
        30 to intArrayOf(39), // 4-bit foreground color: black
        31 to intArrayOf(39), // 4-bit foreground color: red
        32 to intArrayOf(39), // 4-bit foreground color: green
        33 to intArrayOf(39), // 4-bit foreground color: yellow
        34 to intArrayOf(39), // 4-bit foreground color: blue
        35 to intArrayOf(39), // 4-bit foreground color: purple
        36 to intArrayOf(39), // 4-bit foreground color: cyan
        37 to intArrayOf(39), // 4-bit foreground color: white
        38 to intArrayOf(39), // 8-bit / 24-bit foreground colors
        39 to intArrayOf(39), // foreground color reset
        40 to intArrayOf(49), // 4-bit background color: black
        41 to intArrayOf(49), // 4-bit background color: red
        42 to intArrayOf(49), // 4-bit background color: green
        43 to intArrayOf(49), // 4-bit background color: yellow
        44 to intArrayOf(49), // 4-bit background color: blue
        45 to intArrayOf(49), // 4-bit background color: purple
        46 to intArrayOf(49), // 4-bit background color: cyan
        47 to intArrayOf(49), // 4-bit background color: white
        48 to intArrayOf(49), // 8-bit / 24-bit background colors
        49 to intArrayOf(49), // background color reset
        50 to intArrayOf(50),
        51 to intArrayOf(54),
        52 to intArrayOf(54),
        53 to intArrayOf(55),
        54 to intArrayOf(54),
        55 to intArrayOf(55),
        55 to intArrayOf(55),
        58 to intArrayOf(59),
        59 to intArrayOf(59),
        60 to intArrayOf(65),
        61 to intArrayOf(65),
        62 to intArrayOf(65),
        63 to intArrayOf(65),
        64 to intArrayOf(65),
        65 to intArrayOf(65),
        73 to intArrayOf(75),
        74 to intArrayOf(75),
        75 to intArrayOf(75),
        75 to intArrayOf(75),
        90 to intArrayOf(39), // 4-bit foreground color: bright black
        91 to intArrayOf(39), // 4-bit foreground color: bright red
        92 to intArrayOf(39), // 4-bit foreground color: bright green
        93 to intArrayOf(39), // 4-bit foreground color: bright yellow
        94 to intArrayOf(39), // 4-bit foreground color: bright blue
        95 to intArrayOf(39), // 4-bit foreground color: bright purple
        96 to intArrayOf(39), // 4-bit foreground color: bright cyan
        97 to intArrayOf(39), // 4-bit foreground color: bright white
        100 to intArrayOf(49), // 4-bit background color: bright black
        101 to intArrayOf(49), // 4-bit background color: bright red
        102 to intArrayOf(49), // 4-bit background color: bright green
        103 to intArrayOf(49), // 4-bit background color: bright yellow
        104 to intArrayOf(49), // 4-bit background color: bright blue
        105 to intArrayOf(49), // 4-bit background color: bright purple
        106 to intArrayOf(49), // 4-bit background color: bright cyan
        107 to intArrayOf(49), // 4-bit background color: bright white
    )

    val closingToOpeningCodes: Map<Int, IntArray> = with(mutableMapOf<Int, MutableList<Int>>()) {
        openingToClosingCodes.forEach { (openingCode, closingCodes) ->
            closingCodes.forEach { closingCode -> getOrPut(closingCode) { mutableListOf() }.add(openingCode) }
        }
        map { (closingCode, openingCodes) -> closingCode to openingCodes.toIntArray() }.toMap()
    }

    /**
     * Searches this character sequence for [AnsiCodeHelper] and returns a stream of codes.
     *
     * ***Note:** This method makes no difference between opening and closing codes.*
     */
    fun CharSequence.parseAnsiCodes(): List<IntArray> =
        REGEX.findAll(this).singleOrNull()?.let { parseAnsiCode(it) } ?: emptyList()

    /**
     * Given a [matchResult] resulting from [ansiCodeRegex] all found ANSI codes are returned.
     *
     * ***Note:** This method makes no difference between opening and closing codes.*
     */
    fun parseAnsiCode(matchResult: MatchResult): List<IntArray> {
        val intermediateBytes: String? = matchResult.namedGroups["intermediateBytes"]?.value
        val lastByte = matchResult.namedGroups["finalByte"]?.value
        return if (intermediateBytes.isNullOrBlank() && lastByte == "m") {
            matchResult.namedGroups["parameterBytes"]
                ?.value
                ?.split(";")
                ?.mapNotNull { it.toIntOrNull() }
                ?.group() ?: emptyList()
        } else emptyList()
    }

    /**
     * Creates groups from the given codes.
     *
     * Each codes forms one group with the exception of one-byte and three-byte colors,
     * which form groups of two (38/48 + color) or four bytes (38/48 + r + g + b).
     */
    private fun List<Int>.group(): List<IntArray> {
        val groups = mutableListOf<IntArray>()

        var checkingColorType = false
        var groupMissingCodes = 0
        var group: MutableList<Int>? = null
        forEach {
            if (!checkingColorType) {
                if (groupMissingCodes == 0) {
                    if (it != 38 /* foreground color */ && it != 48 /* background color */ && it != 58 /* underline color */) {
                        groups.add(intArrayOf(it))
                    } else {
                        checkingColorType = true
                        group = mutableListOf(it)
                    }
                } else {
                    with(checkNotNull(group)) {
                        add(it)
                        groupMissingCodes--
                        if (groupMissingCodes == 0) {
                            groups.add(toIntArray())
                            group = null
                        }
                    }
                }
            } else {
                with(checkNotNull(group)) {
                    when (it) {
                        5 -> {
                            groupMissingCodes = 1 // 1x color byte}
                            add(it)
                            checkingColorType = false
                        }
                        2 -> {
                            groupMissingCodes = 3 // 1x color byte
                            add(it)
                            checkingColorType = false
                        }
                        else -> error("unknown color type $it")
                    }
                }
            }
        }
        return groups
    }
}
