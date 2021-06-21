package koodies.text

import koodies.math.mod
import koodies.regex.namedGroups
import koodies.runtime.AnsiSupport
import koodies.runtime.AnsiSupport.ANSI4
import koodies.runtime.AnsiSupport.NONE
import koodies.runtime.ansiSupport
import koodies.runtime.isDebugging
import koodies.runtime.isDeveloping
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.ColoredText
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.AnsiCode.Companion.REGEX
import koodies.text.AnsiCodeHelper.closingControlSequence
import koodies.text.AnsiCodeHelper.controlSequence
import koodies.text.AnsiCodeHelper.parseAnsiCodesAsSequence
import koodies.text.AnsiCodeHelper.unclosedCodes
import koodies.text.AnsiString.Companion.tokenize
import koodies.text.LineSeparators.mapLines
import koodies.text.Semantics.formattedAs
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextDouble
import koodies.text.Unicode.controlSequenceIntroducer as c
import koodies.text.Unicode.escape as e
import kotlin.text.contains as containsNonAnsiAware

/**
 * All around [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public object ANSI {

    private val level by lazy { if (isDebugging) NONE else ansiSupport }

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
     * **Example: "Bleeding" Koodies logo**
     * ```
     * &kyTTTTTTTTTTTTTTTTTTTTuvvvvvvvvvvvvvvvvvvvvvvvv\.
     * RR&kyTTTTTTTTTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBRR&kyTTTTTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBRR&kyTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBRR&kyTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBRR&kyTx}vvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBBBRZT}vvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBBBQxvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBB&xvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBZzvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBZuvvvvvvvvvvvvvvvvvvvvvv▗▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * BBZTvvvvvvvvvvvvvvvvvvvvvv\.▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * R3vvvvvvvvvvvvvvvvvvvvvv\.   .▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * vvvvvvvvvvvvvvvvvvvvvv\.       .▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * vvvvvvvvvvvvvvvvvvvv\.           .▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * uxvvvvvvvvvvvvvvvvz3x_              ▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * ▁3uxvvvvvvvvvvvv▁▅&▆▂gx`              ▝▜MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * Z▅▁3uxvvvvvvvvz▆WWRZ&▆▂gv.             `▀WMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * WR&▄▁3uxvvvvvuk▀BWWWRZ&▆▂gv.         .\vvz▀WMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * WWWRZ▅▁3ux▁▂Zg33k▀BWWWRZ&▆▂g}.     .\vvvvvvz▀WMM0WWWWWWWWWWWWWWWWWWWWWWWWWWWWWW
     * 000WWRZ▅▃▆MM▆▂Zg33k▀BWWWRZ&▆▂g}. .\vvvvvvvvvvx▀BBRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR
     * 00000WMMMMMMMM▆▂Zg33k▀BWWWRZ&▆▂yxxvvvvvvvvvvvvvx▝▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
     * 0000MMMMMMMMMMMM▆▂Zg33k▀BWWWRZ▆▆▂gTxvvvvvvvvvvvvvxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * 00MMMMMMMMMMMMMMMM▆▂Zg33k▀BWWWRZ&▆▂gTxvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     * MMMMMMMMMMMMMMMMMMMM▆▂Zg33g▀BWWWRZ&▆▂gTxvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     * ```
     *
     * **Example: "Repaired" Koodies logo with reset lines**
     * ```
     * &kyTTTTTTTTTTTTTTTTTTTTuvvvvvvvvvvvvvvvvvvvvvvvv\.
     * RR&kyTTTTTTTTTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBRR&kyTTTTTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBRR&kyTTTTTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBRR&kyTTTTTvvvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBRR&kyTx}vvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBBBRZT}vvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBBBBQxvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBBB&xvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBBBZzvvvvvvvvvvvvvvvvvvvvvv\.
     * BBBBZuvvvvvvvvvvvvvvvvvvvvvv▗▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * BBZTvvvvvvvvvvvvvvvvvvvvvv\.▝▜MMMMMMMMMMMMMMMMMMMM
     * R3vvvvvvvvvvvvvvvvvvvvvv\.   .▝▜MMMMMMMMMMMMMMMMMM
     * vvvvvvvvvvvvvvvvvvvvvv\.       .▝▜MMMMMMMMMMMMMMMM
     * vvvvvvvvvvvvvvvvvvvv\.           .▝▜MMMMMMMMMMMMMM
     * uxvvvvvvvvvvvvvvvvz3x_              ▝▜MMMMMMMMMMMM
     * ▁3uxvvvvvvvvvvvv▁▅&▆▂gx`              ▝▜MMMMMMMMMM
     * Z▅▁3uxvvvvvvvvz▆WWRZ&▆▂gv.             `▀WMMMMMMMM
     * WR&▄▁3uxvvvvvuk▀BWWWRZ&▆▂gv.         .\vvz▀WMMMMMM
     * WWWRZ▅▁3ux▁▂Zg33k▀BWWWRZ&▆▂g}.     .\vvvvvvz▀WMM0W
     * 000WWRZ▅▃▆MM▆▂Zg33k▀BWWWRZ&▆▂g}. .\vvvvvvvvvvx▀BBR
     * 00000WMMMMMMMM▆▂Zg33k▀BWWWRZ&▆▂yxxvvvvvvvvvvvvvx▝▀
     * 0000MMMMMMMMMMMM▆▂Zg33k▀BWWWRZ▆▆▂gTxvvvvvvvvvvvvvx
     * 00MMMMMMMMMMMMMMMM▆▂Zg33k▀BWWWRZ&▆▂gTxvvvvvvvvvvvv
     * MMMMMMMMMMMMMMMMMMMM▆▂Zg33g▀BWWWRZ&▆▂gTxvvvvvvvvvv
     * ```
     */
    @Suppress("SpellCheckingInspection")
    public fun CharSequence.resetLines(): String {
        val reset = reset(ANSI4)
        return toString().mapLines { "$it$reset" }
    }

    public fun interface FilteringFormatter {
        public operator fun invoke(value: Any): CharSequence?
        public operator fun plus(other: FilteringFormatter): FilteringFormatter = FilteringFormatter { invoke(it)?.let(other::invoke) }

        public companion object {

            /**
             * Returns a new formatter that is provided with the initial text freed
             * from any previous formatting and wrapped in a [ANSI.Text] for convenient
             * customizations.
             */
            public fun fromScratch(transform: Text.() -> CharSequence?): FilteringFormatter = FilteringFormatter { it.toString().ansiRemoved.ansi.transform() }

            /**
             * A formatter that leaves the [text] unchanged.
             */
            public val ToString: FilteringFormatter = FilteringFormatter { text -> text.toString() }
        }
    }

    public fun interface Formatter : FilteringFormatter {
        override operator fun invoke(value: Any): CharSequence
        public operator fun plus(other: Formatter): Formatter = Formatter { invoke(it).let(other::invoke) }

        public companion object {

            /**
             * Returns a new formatter that is provided with the initial text freed
             * from any previous formatting and wrapped in a [ANSI.Text] for convenient
             * customizations.
             */
            public fun fromScratch(transform: Text.() -> CharSequence): Formatter = Formatter { it.toString().ansiRemoved.ansi.transform() }

            /**
             * A formatter that leaves the [text] unchanged.
             */
            public val ToString: Formatter = Formatter { text -> text.toString() }
        }
    }

    public fun CharSequence.colorize(): String = mapCharacters { Colors.random()(it) }

    public interface Colorizer : Formatter {
        public val bg: Formatter
        public fun on(backgroundColorizer: Colorizer): Formatter
    }

    private open class AnsiCodeFormatter(private val ansiCode: AnsiCode) : Formatter {
        override fun invoke(value: Any): String = ansiCode.format(value.toString())
        override operator fun plus(other: Formatter): Formatter =
            (other as? AnsiCodeFormatter)?.ansiCode?.plus(ansiCode)?.let { AnsiCodeFormatter(it) } ?: super.plus(other)

        override operator fun plus(other: FilteringFormatter): FilteringFormatter =
            (other as? AnsiCodeFormatter)?.ansiCode?.plus(ansiCode)?.let { AnsiCodeFormatter(it) } ?: super.plus(other)
    }

    private class AnsiColorCodeFormatter(ansiCode: AnsiColorCode) : Colorizer, AnsiCodeFormatter(ansiCode) {
        override val bg: Formatter = AnsiCodeFormatter(ansiCode.bg)
        override fun on(backgroundColorizer: Colorizer): Formatter = this + (backgroundColorizer.bg)
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
            AnsiSupport.ANSI4 -> Ansi16ColorCode(color.toAnsi16().code)
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

        public val bold: Formatter get() = AnsiCodeFormatter(ansi(1, 22))
        public val dim: Formatter get() = AnsiCodeFormatter(ansi(2, 22))
        public val italic: Formatter get() = AnsiCodeFormatter(ansi(3, 23))
        public val underline: Formatter get() = AnsiCodeFormatter(ansi(4, 24))
        public val inverse: Formatter get() = AnsiCodeFormatter(ansi(7, 27))
        public val hidden: Formatter get() = AnsiCodeFormatter(ansi(8, 28))
        public val strikethrough: Formatter get() = AnsiCodeFormatter(ansi(9, 29))

        public fun CharSequence.bold(): CharSequence = bold(this)
        public fun CharSequence.dim(): CharSequence = dim(this)
        public fun CharSequence.italic(): CharSequence = italic(this)

        private fun ansi(open: Int, close: Int) =
            if (level == NONE) DisabledAnsiCode else AnsiCode(open, close)
    }

    public open class Preview(
        protected val text: CharSequence,
        protected open val formatter: FilteringFormatter = FilteringFormatter.ToString,
        public val done: String = formatter(text).toString(),
    ) : CharSequence by done {
        @Deprecated("use done", ReplaceWith("this.done"))
        public operator fun not(): String = done
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
        public fun style(formatter: Formatter): T
        public fun style(formatter: FilteringFormatter): T?

        public val bold: T get() = style(Style.bold)
        public val dim: T get() = style(Style.dim)
        public val italic: T get() = style(Style.italic)
        public val underline: T get() = style(Style.underline)
        public val inverse: T get() = style(Style.inverse)
        public val hidden: T get() = style(if (isDeveloping) Formatter { " ".repeat(toString().columns) } else Style.hidden)
        public val strikethrough: T get() = style(Style.strikethrough)
    }

    public class Text private constructor(text: CharSequence, formatter: FilteringFormatter = FilteringFormatter.ToString) :
        Preview(text, formatter),
        Colorable<ColoredText>,
        Styleable<Text> {
        override fun color(colorizer: Colorizer): ColoredText = ColoredText(text, colorizer)
        override fun style(formatter: Formatter): Text = Text(formatter(text))
        override fun style(formatter: FilteringFormatter): Text? = formatter(text)?.let(::Text)

        public class ColoredText(text: CharSequence, private val colorizer: Colorizer) : Preview(text, colorizer),
            Styleable<Text> {
            override fun style(formatter: Formatter): Text = Text(text, colorizer + formatter)
            override fun style(formatter: FilteringFormatter): Text = Text(text, colorizer + formatter)
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

public object Banner {
    private val prefix = with(ANSI.Colors) {
        listOf(
            black to gray, cyan to brightCyan, blue to brightBlue, green to brightGreen, yellow to brightYellow, magenta to brightMagenta, red to brightRed,
        ).joinToString("") { (normal, bright) -> (normal.bg + bright)("░") }
    }
    private val delimiters = Regex("\\s+")
    private val capitalLetter = Regex("[A-Z]")

    public fun banner(text: String): String {
        return text.split(delimiters).mapIndexed { index, word ->
            if (index == 0) {
                val (first: String, second: String) = word.splitCamelCase()
                (prefix + " " + first.toUpperCase().ansi.brightCyan + " " + second.toUpperCase().ansi.cyan).trim()
            } else {
                word.toUpperCase().ansi.brightMagenta
            }
        }.joinToString(" ")
    }

    private fun String.splitCamelCase(): Pair<String, String> =
        replace(capitalLetter) { match -> " " + match.value }
            .split(" ")
            .filter { it.isNotBlank() }
            .let { words -> words.first() to words.drop(1).joinToString("") { it.capitalize() } }
}

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
        val REGEX: Regex = Regex("(?<CSI>${c}\\[|${e}\\[)(?<parameterBytes>[0-?]*)(?<intermediateBytes>[ -/]*)(?<finalByte>[@-~])")
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
    private val cache = mutableMapOf<Int, AnsiString>()
    fun getOrPut(charSequence: CharSequence): AnsiString = when {
        charSequence is AnsiString -> charSequence
        charSequence.isEmpty() -> AnsiString.EMPTY
        else -> cache.getOrPut(charSequence.hashCode()) { charSequence.tokenize() }
    }
}

public typealias Token = Pair<CharSequence, Int>

private object TokenizationCache {
    private val cache = mutableMapOf<Int, AnsiString>()
    fun getOrPut(text: CharSequence, block: () -> AnsiString): AnsiString {
        return cache.getOrPut(text.hashCode(), block)
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
public open class AnsiString(internal vararg val tokens: Token) : CharSequence {

    public companion object {
        public val EMPTY: AnsiString = AnsiString()

        public val CharSequence.ansiString: AnsiString get() = AnsiStringCache.getOrPut(this)
        public fun <T : CharSequence> T?.asAnsiString(): AnsiString = this?.let { AnsiStringCache.getOrPut(it) } ?: EMPTY

        public fun CharSequence.tokenize(): AnsiString = TokenizationCache.getOrPut(this) {
            val tokens = mutableListOf<Token>()
            val codes = mutableListOf<Int>()
            var consumed = 0
            while (consumed < length) {
                val match = REGEX.find(this, consumed)
                val range = match?.range
                if (range?.first == consumed) {
                    val escapeSequence = this.subSequence(consumed, match.range.last + 1).also {
                        val currentCodes = AnsiCodeHelper.parseAnsiCode(match).toList()
                        codes.addAll(currentCodes)
                        consumed += it.length
                    }
                    if (escapeSequence.isNotEmpty()) tokens.add(escapeSequence to 0)
                } else {
                    val first: Int? = range?.first
                    val ansiAhead = if (first != null) {
                        first < length
                    } else false
                    val ansiCodeXFreeString = subSequence(consumed, if (ansiAhead) first!! else length).also {
                        consumed += it.length
                    }
                    tokens.add(ansiCodeXFreeString to ansiCodeXFreeString.length)
                }
            }
            AnsiString(*tokens.toTypedArray())
        }

        private val subSequenceCache = mutableMapOf<Pair<Int, Pair<Int, Int>>, String>()
    }

    public val containsAnsi: Boolean = tokens.any { it.second == 0 }

    public val ansiLength: Int get():Int = tokens.sumOf { it.second }

    /**
     * Contains this [string] with all ANSI escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    public val unformatted: String by lazy { tokens.filter { it.second != 0 }.joinToString("") { it.first } }

    /**
     * Returns the logical length of this string. That is, the same length as the unformatted [String] would return.
     */
    override val length: Int by lazy { unformatted.length }

    /**
     * Returns the unformatted char at the specified [index].
     *
     * Due to the limitation of a [Char] to two byte no formatted [Char] can be returned.
     */
    override fun get(index: Int): Char = unformatted[index]

    /**
     * Returns the same character sequence as an unformatted [String.subSequence] would do.
     *
     * Sole difference: The formatting ANSI escape sequences are kept.
     * Eventually open sequences will be closes at the of the sub sequence.
     */
    override fun subSequence(startIndex: Int, endIndex: Int): AnsiString {
        return subSequenceCache.getOrPut(hashCode() to (startIndex to endIndex)) {
            val full: String = ansiSubSequence(endIndex).first
            if (startIndex > 0) {
                val (prefix, unclosedCodes) = ansiSubSequence(startIndex)
                val controlSequence: String = controlSequence(unclosedCodes)
                val startIndex1 = prefix.length - closingControlSequence(unclosedCodes).length
                controlSequence + full.subSequence(startIndex1, full.length)
            } else {
                full
            }
        }.asAnsiString()
    }

    private fun ansiSubSequence(endIndex: Int): Pair<String, List<Int>> {
        if (endIndex == 0) return "" to emptyList()
        if (endIndex > ansiLength) throw IndexOutOfBoundsException("$endIndex must not be greater than $ansiLength")
        var read = 0
        val codes = mutableListOf<Int>()
        val sb = StringBuilder()

        tokens.forEach { (token, tokenLength) ->
            val needed = endIndex - read
            if (needed > 0 && tokenLength == 0) {
                sb.append(token)
                codes.addAll(token.parseAnsiCodesAsSequence())
            } else {
                if (needed <= tokenLength) {
                    sb.append(token.subSequence(0, needed))
                    return@ansiSubSequence "$sb" + closingControlSequence(codes) to unclosedCodes(codes)
                }
                sb.append(token)
                read += tokenLength
            }
        }
        error("must not happen")
    }

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is blank (≝ is empty or consists of nothing but whitespaces).
     */
    public fun isBlank(): Boolean = unformatted.isBlank()

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is not blank (≝ is not empty and consists of at least one non-whitespace).
     */
    public fun isNotBlank(): Boolean = unformatted.isNotBlank()

    public fun toString(removeAnsi: Boolean = false): String =
        if (!removeAnsi) tokens.joinToString("") { it.first }
        else tokens.filter { it.second != 0 }.joinToString("") { it.first }

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
        chunkedByColumnsSequence(columns) { it.asAnsiString() }

    public operator fun plus(other: CharSequence): AnsiString {
        val otherTokens = if (other is AnsiString) other.tokens else other.toString().tokenize().tokens
        return AnsiString(*tokens, *otherTokens)
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
    fun closingControlSequence(codes: List<Int>): String = controlSequence(closingCodes(unclosedCodes(codes)))

    /**
     * Iterates through the codes and returns the ones that have no closing counterpart.
     *
     * Think of it as a bunch of HTML tags of which a few have not been closed,
     * whereas this function returns those tags that render the HTML invalid.
     */
    fun unclosedCodes(codes: List<Int>): List<Int> {
        val unclosedCodes = mutableListOf<Int>()
        codes.forEach { code: Int ->
            val ansiCodes: List<AnsiCode> = codeToAnsiCodeMappings[code] ?: emptyList()
            ansiCodes.forEach { ansiCode ->
                if (code !in ansiCode.codes.map { it.second }) {
                    unclosedCodes.addAll(ansiCode.codes.flatMap { it.first })
                } else {
                    unclosedCodes.removeAll { it in ansiCode.codes.flatMap { it.first } }
                }
            }
        }
        return unclosedCodes
    }

    /**
     * Returns the codes needed to close the given ones.
     */
    fun closingCodes(codes: List<Int>): List<Int> =
        codes.flatMap { openCode -> codeToAnsiCodeMappings[openCode]?.flatMap { ansiCode -> ansiCode.codes.map { it.second } } ?: emptyList() }

    /**
     * Returns the rendered control sequence for the given codes.
     */
    fun controlSequence(codes: List<Int>): String =
        AnsiCode(codes, 0).format(splitCodeMarker).split(splitCodeMarker)[0]

    /**
     * A map that maps the open and close codes of all supported instances of [AnsiCodeHelper]
     * to their respective [AnsiCodeHelper].
     */
    val codeToAnsiCodeMappings: Map<Int, List<AnsiCode>> by lazy {
        hashMapOf<Int, MutableList<AnsiCode>>().apply {
            listOf(
                Ansi16ColorCode(Ansi16.black.code),
                Ansi16ColorCode(Ansi16.red.code),
                Ansi16ColorCode(Ansi16.green.code),
                Ansi16ColorCode(Ansi16.yellow.code),
                Ansi16ColorCode(Ansi16.blue.code),
                Ansi16ColorCode(Ansi16.purple.code),
                Ansi16ColorCode(Ansi16.cyan.code),
                Ansi16ColorCode(Ansi16.white.code),
                Ansi16ColorCode(Ansi16.brightBlack.code),
                Ansi16ColorCode(Ansi16.brightRed.code),
                Ansi16ColorCode(Ansi16.brightGreen.code),
                Ansi16ColorCode(Ansi16.brightYellow.code),
                Ansi16ColorCode(Ansi16.brightBlue.code),
                Ansi16ColorCode(Ansi16.brightPurple.code),
                Ansi16ColorCode(Ansi16.brightCyan.code),
                Ansi16ColorCode(Ansi16.brightWhite.code),
                AnsiCode(0, 0),
                AnsiCode(1, 22),
                AnsiCode(2, 22),
                AnsiCode(3, 23),
                AnsiCode(4, 24),
                AnsiCode(5, 25),
                AnsiCode(6, 25),
                AnsiCode(7, 27),
                AnsiCode(8, 28),
                AnsiCode(9, 29),
            ).forEach { ansiCode ->
                ansiCode.codes.flatMap { (first, second) -> first + second }.forEach { code ->
                    getOrPut(code) { mutableListOf() }.add(ansiCode)
                }
            }
        }
    }

    /**
     * Searches this character sequence for [AnsiCodeHelper] and returns a stream of codes.
     *
     * ***Note:** This method makes no difference between opening and closing codes.*
     */
    fun CharSequence.parseAnsiCodesAsSequence(): Sequence<Int> = REGEX.findAll(this).flatMap { parseAnsiCode(it) }

    /**
     * Given a [matchResult] resulting from [ansiCodeRegex] all found ANSI codes are returned.
     *
     * ***Note:** This method makes no difference between opening and closing codes.*
     */
    fun parseAnsiCode(matchResult: MatchResult): List<Int> {
        val intermediateBytes: String? = matchResult.namedGroups["intermediateBytes"]?.value
        val lastByte = matchResult.namedGroups["finalByte"]?.value
        return if (intermediateBytes.isNullOrBlank() && lastByte == "m") {
            (matchResult.namedGroups["parameterBytes"] ?: return emptyList())
                .value.split(";").mapNotNull { it.toIntOrNull() }.toList()
        } else emptyList()
    }
}
