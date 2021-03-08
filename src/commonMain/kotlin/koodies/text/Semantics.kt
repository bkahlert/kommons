package koodies.text

import koodies.text.ANSI.Colors.brightCyan
import koodies.text.ANSI.Colors.brightYellow
import koodies.text.ANSI.Colors.cyan
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Colors.green
import koodies.text.ANSI.Colors.red

public object Semantics {

    public val OK: String = Unicode.Emojis.heavyCheckMark.textVariant.formattedAs.success
    public val Error: String = Unicode.greekSmallLetterKoppa.toString().formattedAs.error
    public val PointNext: String = Unicode.Emojis.heavyRoundTippedRightwardsArrow.toString()

    /**
     * Semantic formatter which binds to the specified [text].
     */
    public class SemanticText(private val text: String) {
        /**
         * Formats `this` [text] as expressing something successful.
         */
        public val success: String get() = text.green()

        /**
         * Formats `this` [text] as expressing a warning.
         */
        public val warning: String get() = text.brightYellow()

        /**
         * Formats `this` [text] as expressing something that failed.
         */
        public val failure: String get() = text.red()

        /**
         * Formats `this` [text] as expressing something that failed.
         */
        public val error: String get() = text.red()

        /**
         * Formats `this` [text] to ease temporary debugging.
         */
        public val debug: String get() = text.brightCyan()

        /**
         * Formats `this` [text] as expressing an input.
         */
        public val input: String get() = text.cyan()

        /**
         * Formats `this` [text] as expressing a meta information.
         */
        public val meta: String get() = text.gray()
    }

    public val CharSequence.formattedAs: SemanticText get() = SemanticText(toString())
}
