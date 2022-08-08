package com.bkahlert.kommons.text

/**
 * Named Unicode code points, like [UnicodeOld.LINE_FEED], [UnicodeOld.SYMBOL_FOR_START_OF_HEADING], [UnicodeOld.Emojis.BALLOT_BOX], etc.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public object UnicodeOld {

    /** Mapping of control characters to their respective symbols. */
    public val controlCharacters: Map<kotlin.Char, kotlin.Char> = mapOf(
        Unicode.NULL to Unicode.SYMBOL_FOR_NULL,
        Unicode.START_OF_HEADING to Unicode.SYMBOL_FOR_START_OF_HEADING,
        Unicode.START_OF_TEXT to Unicode.SYMBOL_FOR_START_OF_TEXT,
        Unicode.END_OF_TEXT to Unicode.SYMBOL_FOR_END_OF_TEXT,
        Unicode.END_OF_TRANSMISSION to Unicode.SYMBOL_FOR_END_OF_TRANSMISSION,
        Unicode.ENQUIRY to Unicode.SYMBOL_FOR_ENQUIRY,
        Unicode.ACKNOWLEDGE to Unicode.SYMBOL_FOR_ACKNOWLEDGE,
        Unicode.BELL to Unicode.SYMBOL_FOR_BELL,
        Unicode.BACKSPACE to Unicode.SYMBOL_FOR_BACKSPACE,
        Unicode.CHARACTER_TABULATION to Unicode.SYMBOL_FOR_HORIZONTAL_TABULATION,
        Unicode.LINE_FEED to Unicode.SYMBOL_FOR_LINE_FEED,
        Unicode.LINE_TABULATION to Unicode.SYMBOL_FOR_VERTICAL_TABULATION,
        Unicode.FORM_FEED to Unicode.SYMBOL_FOR_FORM_FEED,
        Unicode.CARRIAGE_RETURN to Unicode.SYMBOL_FOR_CARRIAGE_RETURN,
        Unicode.SHIFT_OUT to Unicode.SYMBOL_FOR_SHIFT_OUT,
        Unicode.SHIFT_IN to Unicode.SYMBOL_FOR_SHIFT_IN,
        Unicode.DATA_LINK_ESCAPE to Unicode.SYMBOL_FOR_DATA_LINK_ESCAPE,
        Unicode.DEVICE_CONTROL_ONE to Unicode.SYMBOL_FOR_DEVICE_CONTROL_ONE,
        Unicode.DEVICE_CONTROL_TWO to Unicode.SYMBOL_FOR_DEVICE_CONTROL_TWO,
        Unicode.DEVICE_CONTROL_THREE to Unicode.SYMBOL_FOR_DEVICE_CONTROL_THREE,
        Unicode.DEVICE_CONTROL_FOUR to Unicode.SYMBOL_FOR_DEVICE_CONTROL_FOUR,
        Unicode.NEGATIVE_ACKNOWLEDGE to Unicode.SYMBOL_FOR_NEGATIVE_ACKNOWLEDGE,
        Unicode.SYNCHRONOUS_IDLE to Unicode.SYMBOL_FOR_SYNCHRONOUS_IDLE,
        Unicode.END_OF_TRANSMISSION_BLOCK to Unicode.SYMBOL_FOR_END_OF_TRANSMISSION_BLOCK,
        Unicode.CANCEL to Unicode.SYMBOL_FOR_CANCEL,
        Unicode.END_OF_MEDIUM to Unicode.SYMBOL_FOR_END_OF_MEDIUM,
        Unicode.SUBSTITUTE to Unicode.SYMBOL_FOR_SUBSTITUTE,
        Unicode.ESCAPE to Unicode.SYMBOL_FOR_ESCAPE,
        Unicode.INFORMATION_SEPARATOR_FOUR to Unicode.SYMBOL_FOR_FILE_SEPARATOR,
        Unicode.INFORMATION_SEPARATOR_THREE to Unicode.SYMBOL_FOR_GROUP_SEPARATOR,
        Unicode.INFORMATION_SEPARATOR_TWO to Unicode.SYMBOL_FOR_RECORD_SEPARATOR,
        Unicode.INFORMATION_SEPARATOR_ONE to Unicode.SYMBOL_FOR_UNIT_SEPARATOR,
        Unicode.DELETE to Unicode.SYMBOL_FOR_DELETE,
    )

    /**
     * Contains this character's replacement symbol if any.
     *
     * This only applies to the so called [controlCharacters].
     */
    public val kotlin.Char.replacementSymbol: kotlin.Char? get() = controlCharacters[this]

    /**
     * Contains this code point's replacement symbol if any.
     *
     * This only applies to the so called [controlCharacters].
     */
    public val CodePoint.replacementSymbol: kotlin.Char? get() = char?.replacementSymbol

    /** [ZERO WIDTH NO-BREAK SPACE](https://codepoints.net/U+FEFF) */
    public const val ZERO_WIDTH_NO_BREAK_SPACE: kotlin.Char = '\uFEFF'

    /** [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) `�` */
    public const val REPLACEMENT_CHARACTER: kotlin.Char = '\uFFFD'

    /** [GREEK LETTER KOPPA](https://codepoints.net/U+03DE) `Ϟ` */
    @Suppress("SpellCheckingInspection") public const val GREEK_LETTER_KOPPA: kotlin.Char = 'Ϟ'

    /** [GREEK SMALL LETTER KOPPA](https://codepoints.net/U+03DF) `ϟ` */
    @Suppress("SpellCheckingInspection") public const val GREEK_SMALL_LETTER_KOPPA: kotlin.Char = 'ϟ'

    /** [TRIPLE VERTICAL BAR DELIMITER](https://codepoints.net/U+2980) `⦀` */
    public const val TRIPLE_VERTICAL_BAR_DELIMITER: kotlin.Char = '⦀'

    /**
     * Unicode emojis as specified by the [Unicode® Technical Standard #51](https://unicode.org/reports/tr51/)
     */
    public object Emojis {

        /**
         * Emoji, e.g. `😀`
         */
        public class Emoji(private val emoji: String) :
            CharSequence by emoji.removeSuffix(VARIATION_SELECTOR_15.toString()).removeSuffix(VARIATION_SELECTOR_16.toString()) {
            public constructor(emoji: kotlin.Char) : this(emoji.toString())

            /**
             * The monochrome variant of this emoji.
             * @see VARIATION_SELECTOR_15
             */
            public val textVariant: String get() = "$emoji$VARIATION_SELECTOR_15"

            /**
             * The colorful variant of this emoji.
             * @see VARIATION_SELECTOR_15
             */
            public val emojiVariant: String get() = "$emoji$VARIATION_SELECTOR_16"

            override fun equals(other: Any?): Boolean = toString() == other.toString()
            override fun hashCode(): Int = emoji.hashCode()
            override fun toString(): String = emoji
        }

        /** [HOURGLASS](https://codepoints.net/U+231B) ⌛️ ⌛︎ */
        public val HOURGLASS: Emoji = Emoji('⌛')

        /** [HOURGLASS WITH FLOWING SAND](https://codepoints.net/U+23F3) ⏳️ ⏳︎ */
        public val HOURGLASS_WITH_FLOWING_SAND: Emoji = Emoji('⏳')

        /** [BALLOT BOX](https://codepoints.net/U+2610) ☐️ ☐︎ */
        public val BALLOT_BOX: Emoji = Emoji('☐')

        /** [BALLOT BOX WITH CHECK](https://codepoints.net/U+2611) ☑️ ☑︎ */
        public val BALLOT_BOX_WITH_CHECK: Emoji = Emoji('☑')

        /** [BALLOT BOX WITH X](https://codepoints.net/U+2612) ☒️ ☒︎ */
        public val BALLOT_BOX_WITH_X: Emoji = Emoji('☒')

        /** [LINE FEED (LF)](https://codepoints.net/U+26A1) ⚡️ ⚡︎ */
        public val HIGH_VOLTAGE_SIGN: Emoji = Emoji('⚡')

        /** [CHECK MARK](https://codepoints.net/U+2713) ✓️ ✓︎ */
        public val CHECK_MARK: Emoji = Emoji('✓')

        /** [HEAVY CHECK MARK](https://codepoints.net/U+2714) ✔️ ✔︎ */
        public val HEAVY_CHECK_MARK: Emoji = Emoji('✔')

        /** [WHITE HEAVY CHECK MARK](https://codepoints.net/U+2705) ✅️ ✅︎ */
        public val WHITE_HEAVY_CHECK_MARK: Emoji = Emoji('✅')

        /** [X MARK](https://codepoints.net/U+274E) ❎️ ❎︎ */
        public val X_MARK: Emoji = Emoji('❎')

        /** [BALLOT X](https://codepoints.net/U+2717) ✗️ ✗︎ */
        public val BALLOT_X: Emoji = Emoji('✗')

        /** [HEAVY BALLOT X](https://codepoints.net/U+2718) ✘️ ✘︎ */
        public val HEAVY_BALLOT_X: Emoji = Emoji('✘')

        /** [CROSS MARK](https://codepoints.net/U+274C) ❌️ ❌︎ */
        public val CROSS_MARK: Emoji = Emoji('❌')

        /** [HEAVY LARGE CIRCLE](https://codepoints.net/U+2B55) ⭕️ ⭕︎ */
        public val HEAVY_LARGE_CIRCLE: Emoji = Emoji('⭕')

        /** [HEAVY ROUND-TIPPED RIGHTWARDS ARROW](https://codepoints.net/U+279C) ➜️ ➜︎ */
        public val HEAVY_ROUND_TIPPED_RIGHTWARDS_ARROW: Emoji = Emoji('➜')

        /** [GREEN CIRCLE](https://codepoints.net/U+1F7E2) 🟢️ 🟢︎ */
        public val GREEN_CIRCLE: Emoji = Emoji("🟢")

        /** [PAGE FACING UP](https://codepoints.net/U+1F4C4) 📄️ 📄︎ */
        public val PAGE_FACING_UP: Emoji = Emoji("📄")

        /** [LEFT-POINTING MAGNIFYING GLASS](https://codepoints.net/U+1F50D) 🔍️ 🔍︎ */
        public val LEFT_POINTING_MAGNIFYING_GLASS: Emoji = Emoji("\uD83D\uDD0D")

        /** [RIGHT-POINTING MAGNIFYING GLASS](https://codepoints.net/U+1F50E) 🔎️ 🔎︎ */
        public val RIGHT_POINTING_MAGNIFYING_GLASS: Emoji = Emoji("\uD83D\uDD0E")

        /**
         * [VARIATION SELECTOR-15](https://codepoints.net/U+FE0E)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0E forces it to be rendered
         * in a textual fashion as compared to a colorful image.</cite>
         */
        public const val VARIATION_SELECTOR_15: kotlin.Char = '︎'

        /**
         * [VARIATION SELECTOR-16](https://codepoints.net/U+FE0F)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0F forces it to be rendered
         * as a colorful image as compared to a monochrome text variant."</cite>
         */
        public const val VARIATION_SELECTOR_16: kotlin.Char = '️'
    }
}

private operator fun String.rangeTo(other: String): CodePointRange = this.toCodePointList().single()..other.toCodePointList().single()
