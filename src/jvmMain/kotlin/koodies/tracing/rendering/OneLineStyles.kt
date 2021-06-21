package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi

public object OneLineStyles {

    public object Brackets : Style {

        private val prefix = "❰❰"
        private val infix = "❱"
        private val suffix = "❱❱"

        private val nestedReplacements = listOf(
            "«" to "‹",
            "❰❰" to "",
            "❰" to "‹",

            "»" to "›",
            "❱❱" to "",
            "❱" to "»",
        )

        override fun start(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(decorationFormatter(prefix), " ", element.ansi.bold)
        }

        override fun content(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(" ", decorationFormatter(infix), " ", element.ansi.bold)
        }

        override fun parent(element: CharSequence, decorationFormatter: Formatter): CharSequence? = buildString {
            append(" ", decorationFormatter(infix), " ", nestedReplacements.fold(element.ansi.italic.done) { acc, (old, new) -> acc.replace(old, new) })
        }

        override fun end(element: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue?, decorationFormatter: Formatter): CharSequence? =
            buildString {
                val formatted = resultValueFormatter(element)?.format()?.ansi?.bold
                if (element.successful == false) append(" ", formatted, " ", decorationFormatter(suffix))
                else append(" ", decorationFormatter(infix), " ", formatted, " ", decorationFormatter(suffix))
            }
    }

    public val DEFAULT: Style = Brackets
}
