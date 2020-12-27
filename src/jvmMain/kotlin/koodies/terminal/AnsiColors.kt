package koodies.terminal

import com.github.ajalt.mordant.AnsiColorCode

object AnsiColors {
    fun CharSequence.colorize(fg: AnsiColorCode? = null, bg: ColorProvider? = null): String {
        if (fg != null && bg != null) return kotlin.runCatching { (fg on bg(ANSI.termColors))("$this") }.getOrDefault("$this")
        if (fg != null && bg == null) return kotlin.runCatching { (fg)("$this") }.getOrDefault("$this")
        if (fg == null && bg != null) return kotlin.runCatching { (bg(ANSI.termColors))("$this") }.getOrDefault("$this")
        return "$this"
    }

    fun CharSequence.black(bg: ColorProvider? = null) = colorize(ANSI.termColors.black, bg)
    fun CharSequence.red(bg: ColorProvider? = null) = colorize(ANSI.termColors.red, bg)
    fun CharSequence.green(bg: ColorProvider? = null) = colorize(ANSI.termColors.green, bg)
    fun CharSequence.yellow(bg: ColorProvider? = null) = colorize(ANSI.termColors.yellow, bg)
    fun CharSequence.blue(bg: ColorProvider? = null) = colorize(ANSI.termColors.blue, bg)
    fun CharSequence.magenta(bg: ColorProvider? = null) = colorize(ANSI.termColors.magenta, bg)
    fun CharSequence.cyan(bg: ColorProvider? = null) = colorize(ANSI.termColors.cyan, bg)
    fun CharSequence.white(bg: ColorProvider? = null) = colorize(ANSI.termColors.white, bg)
    fun CharSequence.gray(bg: ColorProvider? = null) = colorize(ANSI.termColors.gray, bg)
    fun CharSequence.brightRed(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightRed, bg)
    fun CharSequence.brightGreen(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightGreen, bg)
    fun CharSequence.brightYellow(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightYellow, bg)
    fun CharSequence.brightBlue(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightBlue, bg)
    fun CharSequence.brightMagenta(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightMagenta, bg)
    fun CharSequence.brightCyan(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightCyan, bg)
    fun CharSequence.brightWhite(bg: ColorProvider? = null) = colorize(ANSI.termColors.brightWhite, bg)
}
