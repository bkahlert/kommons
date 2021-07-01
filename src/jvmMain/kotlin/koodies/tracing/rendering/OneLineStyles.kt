package koodies.tracing.rendering

import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi

public object OneLineStyles {

    public object Brackets : Style {

        private val prefix = "❱❱"
        private val infix = "❱"
        private val suffix = "❱❱"

        override fun start(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(element.ansi.bold)
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(" ", decorationFormatter(infix), " ", element.ansi.bold)
        }

        override fun parent(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(
                " ",
                decorationFormatter(prefix),
                " ",
                element,
                " ",
                decorationFormatter(suffix),
            )
        }

        override fun end(element: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue?, decorationFormatter: Formatter): CharSequence? =
            buildString {
                val formatted = resultValueFormatter(element)?.format()?.ansi?.bold
                append(" ", formatted)
            }
    }

    public val DEFAULT: Style = Brackets
}
