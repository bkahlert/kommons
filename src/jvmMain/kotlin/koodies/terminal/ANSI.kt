package koodies.terminal

import com.github.ajalt.mordant.TermColors

object ANSI {
    val randomColor get() = termColors.hsv((Math.random() * 360.0).toInt(), 100, 94)
    val termColors by lazy { TermColors(IDE.ansiSupport) }
}

val TermColors.rainbow
    get() = listOf(
        black to gray,
        cyan to brightCyan,
        blue to brightBlue,
        green to brightGreen,
        yellow to brightYellow,
        magenta to brightMagenta,
        red to brightRed,
    )
val TermColors.prefix get() = rainbow.joinToString("") { (normal, bright) -> (normal.bg + bright)("░") }
val TermColors.grayPrefix get() = rainbow.joinToString("") { (normal, _) -> (red.bg + normal)("░") }
fun TermColors.randomColor() = rainbow.map { (normal, _) -> normal }.shuffled()
fun TermColors.colorize(string: String) = string.map { randomColor().first()(it.toString()) }.joinToString("")
