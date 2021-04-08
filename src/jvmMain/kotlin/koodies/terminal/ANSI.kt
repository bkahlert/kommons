package koodies.terminal

import com.github.ajalt.mordant.AnsiColorCode
import com.github.ajalt.mordant.TermColors
import koodies.collections.toLinkedMap
import koodies.runtime.AnsiSupport
import koodies.runtime.ansiSupport

public object ANSI {
    public val termColors: TermColors by lazy {
        TermColors(when (ansiSupport) {
            AnsiSupport.NONE -> TermColors.Level.NONE
            AnsiSupport.ANSI4 -> TermColors.Level.ANSI16
            AnsiSupport.ANSI8 -> TermColors.Level.ANSI256
            AnsiSupport.ANSI24 -> TermColors.Level.TRUECOLOR
        })
    }
}

public val TermColors.rainbow: Map<AnsiColorCode, AnsiColorCode>
    get() = listOf(
        black to gray,
        cyan to brightCyan,
        blue to brightBlue,
        green to brightGreen,
        yellow to brightYellow,
        magenta to brightMagenta,
        red to brightRed,
    ).toLinkedMap()

public val TermColors.prefix: String get() = rainbow.toList().joinToString("") { (normal, bright) -> (normal.bg + bright)("░") }
public val TermColors.grayPrefix: String get() = rainbow.toList().joinToString("") { (normal, _) -> (red.bg + normal)("░") }
public fun TermColors.randomColor(): List<AnsiColorCode> = rainbow.map { (normal, _) -> normal }.shuffled()
public fun TermColors.colorize(string: String): String = string.map { randomColor().first()(it.toString()) }.joinToString("")
