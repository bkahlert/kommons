package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.map
import com.bkahlert.kommons.text.ANSI.Text
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi

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
        public val OK: String = Unicode.Emojis.HEAVY_CHECK_MARK.textVariant.formattedAs.success
        public val Negative: String = Unicode.BoxDrawings.HeavyHorizontal.formattedAs.error
        public val Computation: String = Unicode.Emojis.HOURGLASS_WITH_FLOWING_SAND.emojiVariant.formattedAs.progress
        public val Error: String = Unicode.GREEK_SMALL_LETTER_KOPPA.toString().ansi.bold.formattedAs.error
        public val PointNext: String = Unicode.Emojis.HEAVY_ROUND_TIPPED_RIGHTWARDS_ARROW.formattedAs.meta
        public val Document: String = Unicode.Emojis.PAGE_FACING_UP.toString()
        public val Null: String = "␀".formattedAs.warning
        public val Unknown: String = "❓"
    }

    /**
     * Semantic formatter which binds to the specified [text].
     */
    public class SemanticText(private val text: String) {

        /**
         * Formats this [text] as expressing something successful.
         */
        public val success: String get() = text { green }

        /**
         * Formats this [text] as expressing something ongoing.
         */
        public val progress: String get() = text { blue }

        /**
         * Formats this [text] as expressing a warning.
         */
        public val warning: String get() = text { brightYellow }

        /**
         * Formats this [text] as expressing something that failed.
         */
        public val failure: String get() = text { red }

        /**
         * Formats this [text] as expressing something that failed.
         */
        public val error: String get() = text { red }

        /**
         * Formats this [text] to ease temporary debugging.
         */
        public val debug: String get() = text { brightCyan }

        /**
         * Formats this [text] as expressing an input.
         */
        public val input: String get() = text { cyan }

        /**
         * Formats this [text] as expressing a meta information.
         */
        public val meta: String get() = text { italic.gray }

        /**
         * Formats this [text] as expressing a unit.
         */
        public val unit: String get() = text { wrap(BlockDelimiters.UNIT.map { it.formattedAs.meta }) }

        /**
         * Formats this [text] as expressing a named unit.
         */
        public fun unit(name: String): String = "${name.formattedAs.meta} $text".formattedAs.unit

        /**
         * Formats this [text] as expressing a block.
         */
        public val block: String get() = text { wrap(BlockDelimiters.BLOCK.map { it.formattedAs.meta }) }
    }

    public object FieldDelimiters {
        /**
         * Delimiter to delimit units.
         */
        public val UNIT: String = ".".formattedAs.meta

        /**
         * Delimiter to delimit fields.
         * @see Unicode.TRIPLE_VERTICAL_BAR_DELIMITER
         */
        public val FIELD: String = Unicode.TRIPLE_VERTICAL_BAR_DELIMITER.formattedAs.meta
    }

    public object BlockDelimiters {
        public val TEXT: Pair<String, String> = "〝" to "〞"
        public val UNIT: Pair<String, String> = "⟨" to "⟩"
        public val BLOCK: Pair<String, String> = "{" to "}"
        public val INTROSPECTION: Pair<String, String> = "❬" to "❭"
    }

    private inline operator fun String.invoke(transform: Text.() -> CharSequence): String = ansi.transform().toString()
    public val <T> T.formattedAs: SemanticText get() = SemanticText(toString())
}
