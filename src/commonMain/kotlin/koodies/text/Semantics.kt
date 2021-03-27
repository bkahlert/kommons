package koodies.text

import koodies.text.ANSI.Colors.blue
import koodies.text.ANSI.Colors.brightCyan
import koodies.text.ANSI.Colors.brightYellow
import koodies.text.ANSI.Colors.cyan
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Colors.green
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.Style.italic

public object Semantics {

    public val OK: String = Unicode.Emojis.heavyCheckMark.textVariant.formattedAs.success
    public val Computation: String = Unicode.Emojis.hourglass.emojiVariant.formattedAs.progress
    public val Error: String = Unicode.greekSmallLetterKoppa.toString().formattedAs.error
    public val PointNext: String = Unicode.Emojis.heavyRoundTippedRightwardsArrow.formattedAs.meta

    public val Document: String = Unicode.Emojis.pageFacingUp.toString()

    public val Null: String = "␀".bold()

    /**
     * Semantic formatter which binds to the specified [text].
     */
    public class SemanticText(private val text: String) {
        /**
         * Formats `this` [text] as expressing something successful.
         */
        public val success: String get() = text.green()

        /**
         * Formats `this` [text] as expressing something ongoing.
         */
        public val progress: String get() = text.blue()

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
        public val meta: String get() = text.gray().italic()
    }

    public val <T> T.formattedAs: SemanticText get() = SemanticText(toString())
}
