package koodies.text

import koodies.collections.map
import koodies.text.ANSI.Colors.blue
import koodies.text.ANSI.Colors.brightCyan
import koodies.text.ANSI.Colors.brightYellow
import koodies.text.ANSI.Colors.cyan
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Colors.green
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Style.italic

public object Semantics {

    /**
     * An object that has a symbolic representation.
     */
    public interface Symbolizable {
        /**
         * Symbolic text representing this object.
         */
        public val symbol: String
    }

    public object Symbols {
        public val OK: String = Unicode.Emojis.heavyCheckMark.textVariant.formattedAs.success
        public val Negative: String = Unicode.BoxDrawings.HeavyHorizontal.formattedAs.error
        public val Computation: String = Unicode.Emojis.hourglass.emojiVariant.formattedAs.progress
        public val Error: String = Unicode.greekSmallLetterKoppa.toString().formattedAs.error
        public val PointNext: String = Unicode.Emojis.heavyRoundTippedRightwardsArrow.formattedAs.meta
        public val Delimiter: String = Unicode.tripleVerticalBarDelimiter.formattedAs.meta
        public val Document: String = Unicode.Emojis.pageFacingUp.toString()
        public val Null: String = "␀".formattedAs.warning
    }

    /**
     * Semantic formatter which binds to the specified [text].
     */
    public class SemanticText(private val text: String) {

        /**
         * Formats `this` [text] as expressing something successful.
         */
        public val success: String get() = text { green() }

        /**
         * Formats `this` [text] as expressing something ongoing.
         */
        public val progress: String get() = text { blue() }

        /**
         * Formats `this` [text] as expressing a warning.
         */
        public val warning: String get() = text { brightYellow() }

        /**
         * Formats `this` [text] as expressing something that failed.
         */
        public val failure: String get() = text { red() }

        /**
         * Formats `this` [text] as expressing something that failed.
         */
        public val error: String get() = text { red() }

        /**
         * Formats `this` [text] to ease temporary debugging.
         */
        public val debug: String get() = text { brightCyan() }

        /**
         * Formats `this` [text] as expressing an input.
         */
        public val input: String get() = text { cyan() }

        /**
         * Formats `this` [text] as expressing a meta information.
         */
        public val meta: String get() = text { italic().gray() }

        /**
         * Formats `this` [text] as expressing a unit.
         */
        public val unit: String get() = text { wrap(Markers.unit.map { meta }) }

        /**
         * Formats `this` [text] as expressing a block.
         */
        public val block: String get() = text { wrap(Markers.block.map { meta }) }
    }

    public object Markers {
        public val unit: Pair<String, String> = "⟨" to "⟩"
        public val block: Pair<String, String> = "{" to "}"
        public val introspection: Pair<String, String> = "❬" to "❭"
    }

    private inline operator fun String.invoke(transform: String.() -> CharSequence): String = transform().toString()
    public val <T> T.formattedAs: SemanticText get() = SemanticText(toString())
}
