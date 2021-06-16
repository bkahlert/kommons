package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.mapLines
import koodies.text.prefixWith
import koodies.text.takeUnlessBlank
import koodies.text.withPrefix

public interface Style {
    public val indent: Int
    public fun header(name: CharSequence, formatter: Formatter?): CharSequence
    public fun line(text: CharSequence, formatter: Formatter?): CharSequence
    public fun parentLine(text: CharSequence, formatter: Formatter?): CharSequence = line(text, formatter)
    public fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): CharSequence
}

public sealed class Styles : Style {

    public object Rounded : Styles() {
        override val indent: Int = line("") { it }.length

        override fun header(name: CharSequence, formatter: Formatter?): CharSequence =
            koodies.builder.buildList {
                val nameLines = name.asAnsiString().lines()
                +(formatter("╭──╴").toString() + formatter(nameLines.first()).ansi.bold)
                nameLines.drop(1).forEach {
                    +"${formatter.invoke("│").toString() + "   "}${formatter(it).ansi.bold}"
                }
                +formatter.invoke("│").toString()
            }.joinToString(LineSeparators.LF)

        override fun line(text: CharSequence, formatter: Formatter?): CharSequence =
            formatter.invoke("│").toString() + "   " + text

        override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): CharSequence {
            val processReturnValue = resultValueFormatter(returnValue)
            return when (returnValue.successful) {
                true -> {
                    formatter("│").toString() + LineSeparators.LF + formatter("╰──╴").toString() + processReturnValue.format()
                }
                null -> {
                    val halfLine = formatter("╵").toString()
                    val formatted: String = processReturnValue.symbol + (processReturnValue.textRepresentation?.withPrefix(" ") ?: "")
                    halfLine + LineSeparators.LF + halfLine + (formatted.takeUnlessBlank()?.let { "${LineSeparators.LF}$it" } ?: "")
                }
                false -> {
                    processReturnValue.symbol + LineSeparators.LF + formatter("╰──╴").toString() + (processReturnValue.textRepresentation ?: "")
                }
            }.asAnsiString().mapLines { it.ansi.bold }
        }
    }

    public object Dotted : Styles() {
        override val indent: Int = line("") { it }.length

        private fun playSymbol(formatter: Formatter?) = formatter("▶").toString()
        private fun whitePlaySymbol(formatter: Formatter?) = formatter("▷").toString()

        override fun header(name: CharSequence, formatter: Formatter?): String {
            return koodies.builder.buildList {
                val nameLines = name.asAnsiString().lines()
                +"${playSymbol(formatter)} ${formatter(nameLines.first()).ansi.bold}"
                nameLines.drop(1).forEach {
                    +"${whitePlaySymbol(formatter)} ${formatter(it).ansi.bold}"
                }
            }.joinToString(LineSeparators.LF)
        }

        override fun line(text: CharSequence, formatter: Formatter?): CharSequence =
            formatter("·").toString() + " " + text

        override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): CharSequence =
            resultValueFormatter(returnValue).format().let { if (returnValue.successful == false) it.ansi.red else it }.ansi.bold.done
    }

    public object None : Styles() {
        private val prefix = "    "
        override val indent: Int = prefix.length
        override fun header(name: CharSequence, formatter: Formatter?): CharSequence = formatter.invoke(name)
        override fun line(text: CharSequence, formatter: Formatter?): CharSequence = formatter.invoke(text)
        override fun parentLine(text: CharSequence, formatter: Formatter?): CharSequence = line(text, formatter).prefixWith(prefix)
        override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): CharSequence =
            resultValueFormatter(returnValue).format()
    }

    public companion object {
        public val DEFAULT: Style = None
        public fun from(border: Boolean?): Style = when (border) {
            true -> Rounded
            false -> Dotted
            null -> None
        }
    }
}
