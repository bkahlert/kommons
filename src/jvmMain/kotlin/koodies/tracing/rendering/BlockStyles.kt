package koodies.tracing.rendering

import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.prefixWith
import koodies.text.takeUnlessBlank

public interface BlockStyle : Style {
    public val indent: Int
}

public object BlockStyles {

    public class Solid(private val layout: ColumnsLayout) : BlockStyle {

        override val indent: Int = content("") { it.toString() }?.length ?: 0

        override fun start(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            val startElements = Renderable.of(element).render(layout.totalWidth - indent, 4).asAnsiString().lines()
            appendLine(decorationFormatter(TOP), decorationFormatter(startElements.first()).ansi.bold)
            startElements.drop(1).forEach { startElement ->
                appendLine(decorationFormatter(MIDDLE), "   ", decorationFormatter(startElement).ansi.bold)
            }
            append(decorationFormatter(MIDDLE))
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(decorationFormatter(MIDDLE), "   ", element)
        }

        override fun end(element: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue?, decorationFormatter: Formatter): CharSequence? {
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

        public companion object : (ColumnsLayout) -> Solid {
            // @formatter:off
            private const val TOP =    "╭──╴"
            private const val MIDDLE = "│"
            private const val BOTTOM = "╰──╴"
            // @formatter:on

            override fun invoke(layout: ColumnsLayout): Solid = Solid(layout)
        }
    }

    public class Dotted(private val layout: ColumnsLayout) : BlockStyle {

        override val indent: Int = content("") { it.toString() }?.length ?: 0

        override fun start(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            val startElements = Renderable.of(element).render(layout.totalWidth - indent, 4).asAnsiString().lines()
            append(decorationFormatter(playSymbol), " ", decorationFormatter(startElements.first()).ansi.bold)
            startElements.drop(1).forEach { startElement ->
                appendLine()
                append(decorationFormatter(whitePlaySymbol), " ", decorationFormatter(startElement).ansi.bold)
            }
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(decorationFormatter(dot), " ", element)
        }

        override fun end(element: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue?, decorationFormatter: Formatter): CharSequence? =
            buildString {
                val formatted = resultValueFormatter(element)?.format()
                if (!element.successful) append(formatted?.ansi?.red?.bold)
                else append(formatted?.ansi?.bold)
            }

        public companion object : (ColumnsLayout) -> Dotted {
            private const val playSymbol = "▶"
            private const val whitePlaySymbol = "▷"
            private const val dot = "·"

            override fun invoke(layout: ColumnsLayout): Dotted = Dotted(layout)
        }
    }

    public class None(private val layout: ColumnsLayout) : BlockStyle {
        private val prefix = "    "
        override val indent: Int = prefix.length
        override fun start(element: CharSequence, decorationFormatter: Formatter): CharSequence? =
            decorationFormatter(Renderable.of(element).render(layout.totalWidth - indent, 4)).takeUnlessBlank()

        override fun content(element: CharSequence, decorationFormatter: Formatter): CharSequence? = element.takeUnlessBlank()
        override fun parent(element: CharSequence, decorationFormatter: Formatter): CharSequence? = content(element, decorationFormatter)?.prefixWith(prefix)

        override fun end(element: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue?, decorationFormatter: Formatter): CharSequence? =
            resultValueFormatter(element)?.format()

        public companion object : (ColumnsLayout) -> None {
            override fun invoke(layout: ColumnsLayout): None = None(layout)
        }
    }

    public val DEFAULT: (ColumnsLayout) -> BlockStyle = Solid
}
