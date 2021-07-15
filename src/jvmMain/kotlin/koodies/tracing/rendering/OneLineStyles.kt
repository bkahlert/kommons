package koodies.tracing.rendering

import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi

public object OneLineStyles {

    public object Brackets : Style {

        private val prefix = "❱❱"
        private val infix = "❱"
        private val suffix = "❱❱"

        override fun start(
            element: CharSequence,
            contentFormatter: FilteringFormatter<CharSequence>,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? = buildString {
            append(Renderable.of(element).render(null, 1))
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            append(" ", decorationFormatter(infix), " ", element)
        }

        override fun parent(element: CharSequence, decorationFormatter: Formatter<CharSequence>): CharSequence? = buildString {
            append(
                " ",
                decorationFormatter(prefix),
                " ",
                element,
                " ",
                decorationFormatter(suffix),
            )
        }

        override fun end(
            element: ReturnValue,
            resultValueFormatter: (ReturnValue) -> ReturnValue?,
            decorationFormatter: Formatter<CharSequence>,
        ): CharSequence? =
            buildString {
                val formatted = resultValueFormatter(element)?.format()?.ansi?.bold
                append(" ", formatted)
            }
    }

    public val DEFAULT: Style = Brackets
}
