package koodies.tracing.rendering

import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.toAnsiString
import koodies.text.prefixWith
import koodies.text.repeat
import koodies.text.takeUnlessBlank

public interface BlockStyle : Style {
    public val indent: Int
    public val layout: ColumnsLayout
}

public object BlockStyles {

    public class Solid(override val layout: ColumnsLayout) : BlockStyle {

        override val indent: Int = INDENT

        override fun start(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            val startElements = Renderable.of(element).render(layout.totalWidth, 4).toAnsiString().lines()
            appendLine(decorationFormatter(TOP), startElements.first())
            startElements.drop(1).forEach { startElement ->
                appendLine(decorationFormatter(MIDDLE), MIDDLE_SPACE, startElement)
            }
            append(decorationFormatter(MIDDLE))
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            append(decorationFormatter(MIDDLE), MIDDLE_SPACE, element)
        }

        override fun end(
            element: ReturnValue,
            resultValueFormatter: (ReturnValue) -> ReturnValue?,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? {
            val processReturnValue = resultValueFormatter(element)

            return when (element.successful) {
                true -> buildString {
                    appendLine(decorationFormatter(MIDDLE))
                    append(decorationFormatter(BOTTOM), processReturnValue?.format()?.ansi?.bold)
                }
                false -> buildString {
                    appendLine(processReturnValue?.symbol ?: decorationFormatter(MIDDLE))
                    append(decorationFormatter(BOTTOM), processReturnValue?.textRepresentation?.ansi?.bold)
                }
            }
        }

        public companion object : (ColumnsLayout, Int) -> Solid {
            private const val INDENT: Int = 4

            // @formatter:off
            private const val TOP =    "╭──╴"
            private const val MIDDLE = "│"
            private const val BOTTOM = "╰──╴"
            // @formatter:on

            private val MIDDLE_SPACE = " ".repeat(INDENT - MIDDLE.length)

            override fun invoke(layout: ColumnsLayout, indent: Int): Solid = Solid(layout.shrinkBy(indent + INDENT))
        }
    }

    public class Dotted(override val layout: ColumnsLayout) : BlockStyle {

        override val indent: Int = INDENT

        override fun start(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            val startElements = Renderable.of(element).render(layout.totalWidth, 4).toAnsiString().lines()
            append(decorationFormatter(playSymbol), MIDDLE_SPACE, startElements.first())
            startElements.drop(1).forEach { startElement ->
                appendLine()
                append(decorationFormatter(whitePlaySymbol), MIDDLE_SPACE, startElement)
            }
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            append(decorationFormatter(dot), " ", element)
        }

        override fun end(
            element: ReturnValue,
            resultValueFormatter: (ReturnValue) -> ReturnValue?,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? =
            buildString {
                val formatted = resultValueFormatter(element)?.format()
                if (!element.successful) append(formatted?.ansi?.red?.bold)
                else append(formatted?.ansi?.bold)
            }

        public companion object : (ColumnsLayout, Int) -> Dotted {
            private const val INDENT: Int = 2

            private const val playSymbol = "▶"
            private const val whitePlaySymbol = "▷"
            private const val dot = "·"

            private val MIDDLE_SPACE = " ".repeat(INDENT - dot.length)

            override fun invoke(layout: ColumnsLayout, indent: Int): Dotted = Dotted(layout.shrinkBy(indent + INDENT))
        }
    }

    public class None(override val layout: ColumnsLayout) : BlockStyle {

        override val indent: Int = INDENT

        override fun start(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? =
            Renderable.of(element).render(layout.totalWidth, 4).takeUnlessBlank()

        override fun content(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = element.takeUnlessBlank()
        override fun parent(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? =
            content(element, decorationFormatter)?.prefixWith(PREFIX)

        override fun end(
            element: ReturnValue,
            resultValueFormatter: (ReturnValue) -> ReturnValue?,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? =
            resultValueFormatter(element)?.format()

        public companion object : (ColumnsLayout, Int) -> None {
            private const val INDENT: Int = 4
            private val PREFIX = " ".repeat(INDENT)
            override fun invoke(layout: ColumnsLayout, indent: Int): None = None(layout.shrinkBy(indent))
        }
    }

    public val DEFAULT: (ColumnsLayout, Int) -> BlockStyle = Solid
}
