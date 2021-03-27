package koodies.text

import koodies.runtime.AnsiSupport
import koodies.runtime.AnsiSupport.NONE
import koodies.runtime.Program
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.text.Regex.Companion.escape

public object ANSI {

    /**
     * Contains `this` char sequence with all [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
     */
    public val CharSequence.escapeSequencesRemoved: String get() = AnsiCode.REGEX.replace(toString(), "")
    public val <T : CharSequence> T.containsEscapeSequences: Boolean get() = AnsiCode.REGEX.containsMatchIn(this)

    public fun interface Formatter {
        public operator fun invoke(text: CharSequence): CharSequence
        public operator fun plus(other: Formatter): Formatter = Formatter { other(this(it)) }

        public companion object {

            /**
             * Returns a new formatter that is provided with the initial text freed
             * from any previous formatting and wrapped in a [ANSI.Text] for convenient
             * customizations.
             */
            public fun fromScratch(transform: Text.() -> CharSequence): Formatter = Formatter { it.escapeSequencesRemoved.ansi.transform() }

            /**
             * `Null`-safe formatter function that delegates to the actual formatter if
             * it's not `null` but if it is, the [text] is simply passed-through.
             */
            public operator fun Formatter?.invoke(text: CharSequence): CharSequence = (this ?: PassThrough).invoke(text)

            /**
             * `Null`-safe formatter extension that delegates to the actual formatter if
             * it's not `null` but if it is, the resulting formatter is just the [other] one.
             */
            public operator fun Formatter?.plus(other: Formatter): Formatter = this?.let { it + other } ?: other

            /**
             * A formatter that leaves the [text] unchanged.
             */
            public val PassThrough: Formatter = Formatter { text -> text }
        }
    }

    private class AnsiCodeFormatter(private val ansiCode: AnsiCode) : Formatter {
        override fun invoke(text: CharSequence): String = ansiCode.format(text)
        override operator fun plus(other: Formatter): Formatter = AnsiCodeFormatter(ansiCode + (other as AnsiCodeFormatter).ansiCode)
    }

    private class Colorizer(vararg val colors: AnsiColorCode?) :
        Formatter by AnsiCodeFormatter(colors.filterNotNull().fold(EmptyAnsiColorCode as AnsiCode) { acc, code -> acc.plus(code) })

    public object Colors {
        private val ansiColors: AnsiColors by lazy { AnsiColors(Program.ansiSupport) }

        public val black: Formatter get() = AnsiCodeFormatter(ansiColors.black)
        public val red: Formatter get() = AnsiCodeFormatter(ansiColors.red)
        public val green: Formatter get() = AnsiCodeFormatter(ansiColors.green)
        public val yellow: Formatter get() = AnsiCodeFormatter(ansiColors.yellow)
        public val blue: Formatter get() = AnsiCodeFormatter(ansiColors.blue)
        public val magenta: Formatter get() = AnsiCodeFormatter(ansiColors.magenta)
        public val cyan: Formatter get() = AnsiCodeFormatter(ansiColors.cyan)
        public val white: Formatter get() = AnsiCodeFormatter(ansiColors.white)
        public val gray: Formatter get() = AnsiCodeFormatter(ansiColors.gray)

        public val brightRed: Formatter get() = AnsiCodeFormatter(ansiColors.brightRed)
        public val brightGreen: Formatter get() = AnsiCodeFormatter(ansiColors.brightGreen)
        public val brightYellow: Formatter get() = AnsiCodeFormatter(ansiColors.brightYellow)
        public val brightBlue: Formatter get() = AnsiCodeFormatter(ansiColors.brightBlue)
        public val brightMagenta: Formatter get() = AnsiCodeFormatter(ansiColors.brightMagenta)
        public val brightCyan: Formatter get() = AnsiCodeFormatter(ansiColors.brightCyan)
        public val brightWhite: Formatter get() = AnsiCodeFormatter(ansiColors.brightWhite)

        public val random: Formatter get() = AnsiCodeFormatter(ansiColors.hsv((kotlin.random.Random.nextDouble() * 360.0).toInt(), 82, 89))

        public fun CharSequence.black(): CharSequence = black(this)
        public fun CharSequence.red(): CharSequence = red(this)
        public fun CharSequence.green(): CharSequence = green(this)
        public fun CharSequence.yellow(): CharSequence = yellow(this)
        public fun CharSequence.blue(): CharSequence = blue(this)
        public fun CharSequence.magenta(): CharSequence = magenta(this)
        public fun CharSequence.cyan(): CharSequence = cyan(this)
        public fun CharSequence.white(): CharSequence = white(this)
        public fun CharSequence.gray(): CharSequence = gray(this)

        public fun CharSequence.brightRed(): CharSequence = brightRed(this)
        public fun CharSequence.brightGreen(): CharSequence = brightGreen(this)
        public fun CharSequence.brightYellow(): CharSequence = brightYellow(this)
        public fun CharSequence.brightBlue(): CharSequence = brightBlue(this)
        public fun CharSequence.brightMagenta(): CharSequence = brightMagenta(this)
        public fun CharSequence.brightCyan(): CharSequence = brightCyan(this)
        public fun CharSequence.brightWhite(): CharSequence = brightWhite(this)
    }

    public object Style {
        private val ansiColors: AnsiColors by lazy { AnsiColors(Program.ansiSupport) }

        public val bold: Formatter get() = AnsiCodeFormatter(ansiColors.bold)
        public val dim: Formatter get() = AnsiCodeFormatter(ansiColors.dim)
        public val italic: Formatter get() = AnsiCodeFormatter(ansiColors.italic)
        public val underline: Formatter get() = AnsiCodeFormatter(ansiColors.underline)
        public val inverse: Formatter get() = AnsiCodeFormatter(ansiColors.inverse)
        public val hidden: Formatter get() = AnsiCodeFormatter(ansiColors.hidden)
        public val strikethrough: Formatter get() = AnsiCodeFormatter(ansiColors.strikethrough)

        public fun CharSequence.bold(): CharSequence = bold(this)
        public fun CharSequence.dim(): CharSequence = dim(this)
        public fun CharSequence.italic(): CharSequence = italic(this)
        public fun CharSequence.underline(): CharSequence = underline(this)
        public fun CharSequence.inverse(): CharSequence = inverse(this)
        public fun CharSequence.hidden(): CharSequence = if (Program.isDeveloping) " ".repeat((length * 1.35).toInt()) else hidden("$this")
        public fun CharSequence.strikethrough(): CharSequence = strikethrough("$this")
    }

    public class Text private constructor(private val text: CharSequence) : CharSequence by text {
        private val string = text.toString()
        override fun toString(): String = string

        public fun black(): CharSequence = Colors.black(text)
        public fun red(): CharSequence = Colors.red(text)
        public fun green(): CharSequence = Colors.green(text)
        public fun yellow(): CharSequence = Colors.yellow(text)
        public fun blue(): CharSequence = Colors.blue(text)
        public fun magenta(): CharSequence = Colors.magenta(text)
        public fun cyan(): CharSequence = Colors.cyan(text)
        public fun white(): CharSequence = Colors.white(text)
        public fun gray(): CharSequence = Colors.gray(text)

        public fun brightRed(): CharSequence = Colors.brightRed(text)
        public fun brightGreen(): CharSequence = Colors.brightGreen(text)
        public fun brightYellow(): CharSequence = Colors.brightYellow(text)
        public fun brightBlue(): CharSequence = Colors.brightBlue(text)
        public fun brightMagenta(): CharSequence = Colors.brightMagenta(text)
        public fun brightCyan(): CharSequence = Colors.brightCyan(text)
        public fun brightWhite(): CharSequence = Colors.brightWhite(text)

        public fun random(): CharSequence = Colors.random(text)

        public fun bold(): CharSequence = Style.bold(text)
        public fun dim(): CharSequence = Style.dim(text)
        public fun italic(): CharSequence = Style.italic(text)
        public fun underline(): CharSequence = Style.underline(text)
        public fun inverse(): CharSequence = Style.inverse(text)
        public fun hidden(): CharSequence = if (Program.isDeveloping) " ".repeat((toString().length * 1.35).toInt()) else Style.hidden(string)
        public fun strikethrough(): CharSequence = Style.strikethrough(text)

        public companion object {
            public val CharSequence.ansi: Text get() = Text(this)
        }
    }
}

private class AnsiColors(val support: AnsiSupport = Program.ansiSupport) {

    val black: AnsiColorCode get() = ansi16(30)
    val red: AnsiColorCode get() = ansi16(31)
    val green: AnsiColorCode get() = ansi16(32)
    val yellow: AnsiColorCode get() = ansi16(33)
    val blue: AnsiColorCode get() = ansi16(34)
    val magenta: AnsiColorCode get() = ansi16(35)
    val cyan: AnsiColorCode get() = ansi16(36)
    val white: AnsiColorCode get() = ansi16(37)
    val gray: AnsiColorCode get() = ansi16(90)

    val brightRed: AnsiColorCode get() = ansi16(91)
    val brightGreen: AnsiColorCode get() = ansi16(92)
    val brightYellow: AnsiColorCode get() = ansi16(93)
    val brightBlue: AnsiColorCode get() = ansi16(94)
    val brightMagenta: AnsiColorCode get() = ansi16(95)
    val brightCyan: AnsiColorCode get() = ansi16(96)
    val brightWhite: AnsiColorCode get() = ansi16(97)

    /** Clear all active styles */
    val reset get() = if (support == NONE) DisabledAnsiCode else AnsiCode(0, 0)

    /**
     * Render text as bold or increased intensity.
     *
     * Might be rendered as a different color instead of a different font weight.
     */
    val bold get() = ansi(1, 22)

    /**
     * Render text as faint or decreased intensity.
     *
     * Not widely supported.
     */
    val dim get() = ansi(2, 22)

    /**
     * Render text as italic.
     *
     * Not widely supported, might be rendered as inverse instead of italic.
     */
    val italic get() = ansi(3, 23)

    /**
     * Underline text.
     *
     * Might be rendered with different colors instead of underline.
     */
    val underline get() = ansi(4, 24)

    /** Render text with background and foreground colors switched. */
    val inverse get() = ansi(7, 27)

    /**
     * Conceal text.
     *
     * Not widely supported.
     */
    val hidden get() = ansi(8, 28)

    /**
     * Render text with a strikethrough.
     *
     * Not widely supported.
     */
    val strikethrough get() = ansi(9, 29)

    /** @param hex An rgb hex string in the form "#ffffff" or "ffffff" */
    fun rgb(hex: String): AnsiColorCode = color(RGB(hex))

    /**
     * Create a color code from an RGB color.
     *
     * @param r The red amount, in the range \[0, 255]
     * @param g The green amount, in the range \[0, 255]
     * @param b The blue amount, in the range \[0, 255]
     */
    fun rgb(r: Int, g: Int, b: Int): AnsiColorCode = color(RGB(r, g, b))

    /**
     * Create a color code from an HSV color.
     *
     * @param h The hue, in the range \[0, 360]
     * @param s The saturation, in the range \[0,100]
     * @param v The value, in the range \[0,100]
     */
    fun hsv(h: Int, s: Int, v: Int): AnsiColorCode = color(HSV(h, s, v))

    /**
     * Create a grayscale color code from a fraction in the range \[0, 1].
     *
     * @param fraction The fraction of white in the color. 0 is pure black, 1 is pure white.
     */
    fun gray(fraction: Double): AnsiColorCode {
        require(fraction in 0.0..1.0) { "fraction must be in the range [0, 1]" }
        return round(255 * fraction).toInt().let { rgb(it, it, it) }
    }

    /**
     * Create an ANSI code to move the cursor up [count] cells.
     *
     * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
     * If [count] is negative, the cursor will be moved down instead.
     */
    fun cursorUp(count: Int): String = moveCursor(if (count < 0) "B" else "A", abs(count))

    /**
     * Create an ANSI code to move the cursor down [count] cells.
     *
     * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
     * If [count] is negative, the cursor will be moved up instead.
     */
    fun cursorDown(count: Int): String = moveCursor(if (count < 0) "A" else "B", abs(count))

    /**
     * Create an ANSI code to move the cursor left [count] cells.
     *
     * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
     * If [count] is negative, the cursor will be moved right instead.
     */
    fun cursorLeft(count: Int): String = moveCursor(if (count < 0) "C" else "D", abs(count))

    /**
     * Create an ANSI code to move the cursor right [count] cells.
     *
     * If ANSI codes are not supported, or [count] is 0, an empty string is returned.
     * If [count] is negative, the cursor will be moved left instead.
     */
    fun cursorRight(count: Int): String = moveCursor(if (count < 0) "D" else "C", abs(count))

    /**
     * Create an ANSI code to hide the cursor.
     *
     * If ANSI codes are not supported, an empty string is returned.
     */
    val hideCursor: String get() = if (support == NONE) "" else "$CSI?25l"

    /**
     * Create an ANSI code to show the cursor.
     *
     * If ANSI codes are not supported, an empty string is returned.
     */
    val showCursor: String get() = if (support == NONE) "" else "$CSI?25h"

    private fun moveCursor(dir: String, count: Int): String {
        return if (count == 0 || support == NONE) ""
        else "$CSI$count$dir"
    }

    private fun ansi16(code: Int) =
        if (support == NONE) DisabledAnsiColorCode else Ansi16ColorCode(code)

    private fun ansi(open: Int, close: Int) =
        if (support == NONE) DisabledAnsiCode else AnsiCode(open, close)

    /**
     * Create a color from an existing [Color].
     *
     * It's usually easier to use a function like [rgb] or [hsv] instead.
     */
    fun color(color: Color): AnsiColorCode = when (support) {
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


private const val ESC = Unicode.escape
private const val CSI = "$ESC["
private val ansiCloseRe = Regex("""$ESC\[((?:\d{1,3};?)+)m""")

/**
 * A class representing one or more numeric ANSI codes.
 *
 * @property codes A list of pairs, with each pair being the list of opening codes and a closing code.
 */
private open class AnsiCode(protected val codes: List<Pair<List<Int>, Int>>) {
    constructor(openCodes: List<Int>, closeCode: Int) : this(listOf(openCodes to closeCode))
    constructor(openCode: Int, closeCode: Int) : this(listOf(openCode), closeCode)

    val open: String get() = tag(codes.flatMap { it.first })
    val close: String get() = tag(codes.map { it.second })

    override fun toString() = open

    fun format(text: CharSequence): String = if (text.isEmpty()) "" else open + nest(text) + close

    open operator fun plus(other: AnsiCode) = AnsiCode(codes + other.codes)

    private fun nest(text: CharSequence) = ansiCloseRe.replace(text) {
        // Replace instances of our close codes with their corresponding opening codes. If the close
        // code is at the end of the text, omit it instead so that we don't open and immediately
        // close a command.
        val openCodesByCloseCode = HashMap<Int, List<Int>>()
        for ((o, c) in codes) openCodesByCloseCode[c] = o
        val atEnd = it.range.endInclusive == text.lastIndex
        val codes = it.groupValues[1].splitToSequence(';').flatMap {
            it.toInt().let {
                if (atEnd && it in openCodesByCloseCode) emptySequence()
                else (openCodesByCloseCode[it]?.asSequence() ?: sequenceOf(it))
            }
        }

        tag(codes.toList())
    }

    private fun tag(c: List<Int>) = if (c.isEmpty()) "" else "$CSI${c.joinToString(";")}m"

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
        public val REGEX: Regex = Regex("(?<CSI>${escape(CSI)}|${ESC}\\[)(?<parameterBytes>[0-?]*)(?<intermediateBytes>[ -/]*)(?<finalByte>[@-~])")
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
private abstract class AnsiColorCode(codes: List<Pair<List<Int>, Int>>) : AnsiCode(codes) {
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

private class Ansi16ColorCode(code: Int) : AnsiColorCode(code, 39) {
    override val bgCodes get() = codes.map { listOf(it.first[0] + 10) to 49 }
}

private class Ansi256ColorCode(code: Int) : AnsiColorCode(listOf(38, 5, code), 39) {
    override val bgCodes get() = codes.map { listOf(48, 5, it.first[2]) to 49 }
}

private class AnsiRGBColorCode(r: Int, g: Int, b: Int) : AnsiColorCode(listOf(38, 2, r, g, b), 39) {
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

    public companion object
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

