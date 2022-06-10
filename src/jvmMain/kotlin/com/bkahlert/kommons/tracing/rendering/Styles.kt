package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.takeUnlessBlank
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.prefixWith
import com.bkahlert.kommons.text.repeat

public object Styles {

    public class Solid(override val layout: ColumnsLayout) : Style {

        override val onlineLinePrefix: String = ONE_LINE_PREFIX
        override val onlineLineSeparator: String = ONE_LINE_SEPARATOR
        override val indent: Int = INDENT

        override fun start(
            element: CharSequence,
            contentFormatter: FilteringFormatter<CharSequence>,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? = buildString {
            val startElements = Renderable.of(element)
                .render(layout.totalWidth, 4)
                .toAnsiString()
                .lines()
                .mapNotNull { contentFormatter(it) }
            appendLine(decorationFormatter(TOP), startElements.firstOrNull())
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

            private const val ONE_LINE_PREFIX = "╶──╴"
            private const val ONE_LINE_SEPARATOR = "╶─╴"

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

    public class Dotted(override val layout: ColumnsLayout) : Style {
        override val onlineLinePrefix: String = ONE_LINE_PREFIX
        override val onlineLineSeparator: String = ONE_LINE_SEPARATOR
        override val indent: Int = INDENT

        override fun start(
            element: CharSequence,
            contentFormatter: FilteringFormatter<CharSequence>,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? = buildString {
            val startElements = Renderable.of(element)
                .render(layout.totalWidth, 4)
                .toAnsiString()
                .lines()
                .mapNotNull { contentFormatter(it) }
            append(decorationFormatter(PLAY_SYMBOL), MIDDLE_SPACE, startElements.firstOrNull())
            startElements.drop(1).forEach { startElement ->
                appendLine()
                append(decorationFormatter(WHITE_PLAY_SYMBOL), MIDDLE_SPACE, startElement)
            }
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            append(decorationFormatter(DOT), " ", element)
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

            private const val PLAY_SYMBOL = "▶"
            private const val WHITE_PLAY_SYMBOL = "▷"
            private const val DOT = "·"

            private const val ONE_LINE_PREFIX = "$PLAY_SYMBOL "
            private const val ONE_LINE_SEPARATOR = " $WHITE_PLAY_SYMBOL "

            private const val INDENT: Int = 2

            private val MIDDLE_SPACE = " ".repeat(INDENT - DOT.length)

            override fun invoke(layout: ColumnsLayout, indent: Int): Dotted = Dotted(layout.shrinkBy(indent + INDENT))
        }
    }

    public class None(override val layout: ColumnsLayout) : Style {
        override val onlineLinePrefix: String = ""
        override val onlineLineSeparator: String = " ❱ "
        override val indent: Int = INDENT

        override fun start(
            element: CharSequence,
            contentFormatter: FilteringFormatter<CharSequence>,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? = Renderable.of(element)
            .render(layout.totalWidth, null)
            .toAnsiString()
            .lines()
            .mapNotNull { contentFormatter(it) }
            .run { takeIf { it.isNotEmpty() }?.joinToString(LineSeparators.Default) }

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

    public val DEFAULT: (ColumnsLayout, Int) -> Style = Solid
}
