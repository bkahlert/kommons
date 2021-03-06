package koodies.terminal

import com.github.ajalt.mordant.AnsiColorCode

public object AnsiColors {
    public fun CharSequence.colorize(fg: AnsiColorCode? = null, bg: ColorProvider? = null): String {
        if (fg != null && bg != null) return kotlin.runCatching { (fg on bg(ANSI.termColors))("$this") }.getOrDefault("$this")
        if (fg != null && bg == null) return kotlin.runCatching { (fg)("$this") }.getOrDefault("$this")
        if (fg == null && bg != null) return kotlin.runCatching { (bg(ANSI.termColors))("$this") }.getOrDefault("$this")
        return "$this"
    }

    public fun CharSequence.black(bg: ColorProvider? = null): String = colorize(ANSI.termColors.black, bg)
    public fun CharSequence.red(bg: ColorProvider? = null): String = colorize(ANSI.termColors.red, bg)
    public fun CharSequence.green(bg: ColorProvider? = null): String = colorize(ANSI.termColors.green, bg)
    public fun CharSequence.yellow(bg: ColorProvider? = null): String = colorize(ANSI.termColors.yellow, bg)
    public fun CharSequence.blue(bg: ColorProvider? = null): String = colorize(ANSI.termColors.blue, bg)
    public fun CharSequence.magenta(bg: ColorProvider? = null): String = colorize(ANSI.termColors.magenta, bg)
    public fun CharSequence.cyan(bg: ColorProvider? = null): String = colorize(ANSI.termColors.cyan, bg)
    public fun CharSequence.white(bg: ColorProvider? = null): String = colorize(ANSI.termColors.white, bg)
    public fun CharSequence.gray(bg: ColorProvider? = null): String = colorize(ANSI.termColors.gray, bg)
    public fun CharSequence.brightRed(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightRed, bg)
    public fun CharSequence.brightGreen(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightGreen, bg)
    public fun CharSequence.brightYellow(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightYellow, bg)
    public fun CharSequence.brightBlue(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightBlue, bg)
    public fun CharSequence.brightMagenta(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightMagenta, bg)
    public fun CharSequence.brightCyan(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightCyan, bg)
    public fun CharSequence.brightWhite(bg: ColorProvider? = null): String = colorize(ANSI.termColors.brightWhite, bg)
}
