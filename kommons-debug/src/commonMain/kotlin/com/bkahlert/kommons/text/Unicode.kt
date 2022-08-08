package com.bkahlert.kommons.text

/** Named Unicode code points, like [Unicode.LINE_FEED] or [Unicode.SYMBOL_FOR_START_OF_HEADING]. */
@Suppress("MemberVisibilityCanBePrivate")
public object Unicode {

    /** [NULL](https://codepoints.net/U+0000) */
    public const val NULL: kotlin.Char = '\u0000'

    /** [START OF HEADING](https://codepoints.net/U+0001) */
    public const val START_OF_HEADING: kotlin.Char = '\u0001'

    /** [START OF TEXT](https://codepoints.net/U+0002) */
    public const val START_OF_TEXT: kotlin.Char = '\u0002'

    /** [END OF TEXT](https://codepoints.net/U+0003) */
    public const val END_OF_TEXT: kotlin.Char = '\u0003'

    /** [END OF TRANSMISSION](https://codepoints.net/U+0004) */
    public const val END_OF_TRANSMISSION: kotlin.Char = '\u0004'

    /** [ENQUIRY](https://codepoints.net/U+0005) */
    public const val ENQUIRY: kotlin.Char = '\u0005'

    /** [ACKNOWLEDGE](https://codepoints.net/U+0006) */
    public const val ACKNOWLEDGE: kotlin.Char = '\u0006'

    /** [BELL](https://codepoints.net/U+0007) */
    public const val BELL: kotlin.Char = '\u0007'

    /** [BACKSPACE](https://codepoints.net/U+0008) */
    public const val BACKSPACE: kotlin.Char = '\u0008'

    /** [CHARACTER TABULATION](https://codepoints.net/U+0009) */
    public const val CHARACTER_TABULATION: kotlin.Char = '\u0009'

    /** Synonym for [CHARACTER_TABULATION] */
    public const val TAB: kotlin.Char = CHARACTER_TABULATION

    /** [LINE FEED (LF)](https://codepoints.net/U+000A) */
    public const val LINE_FEED: kotlin.Char = '\u000A'

    /** [LINE TABULATION](https://codepoints.net/U+000B) */
    public const val LINE_TABULATION: kotlin.Char = '\u000B'

    /** [FORM FEED (FF)](https://codepoints.net/U+000C) */
    public const val FORM_FEED: kotlin.Char = '\u000C'

    /** [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D) */
    public const val CARRIAGE_RETURN: kotlin.Char = '\u000D'

    /** [SHIFT OUT](https://codepoints.net/U+000E) */
    public const val SHIFT_OUT: kotlin.Char = '\u000E'

    /** [SHIFT IN](https://codepoints.net/U+000F) */
    public const val SHIFT_IN: kotlin.Char = '\u000F'

    /** [DATA LINK ESCAPE](https://codepoints.net/U+0010) */
    public const val DATA_LINK_ESCAPE: kotlin.Char = '\u0010'

    /** [DEVICE CONTROL ONE](https://codepoints.net/U+0011) */
    public const val DEVICE_CONTROL_ONE: kotlin.Char = '\u0011'

    /** [DEVICE CONTROL TWO](https://codepoints.net/U+0012) */
    public const val DEVICE_CONTROL_TWO: kotlin.Char = '\u0012'

    /** [DEVICE CONTROL THREE](https://codepoints.net/U+0013) */
    public const val DEVICE_CONTROL_THREE: kotlin.Char = '\u0013'

    /** [DEVICE CONTROL FOUR](https://codepoints.net/U+0014) */
    public const val DEVICE_CONTROL_FOUR: kotlin.Char = '\u0014'

    /** [NEGATIVE ACKNOWLEDGE](https://codepoints.net/U+0015) */
    public const val NEGATIVE_ACKNOWLEDGE: kotlin.Char = '\u0015'

    /** [SYNCHRONOUS IDLE](https://codepoints.net/U+0016) */
    public const val SYNCHRONOUS_IDLE: kotlin.Char = '\u0016'

    /** [END OF TRANSMISSION BLOCK](https://codepoints.net/U+0017) */
    public const val END_OF_TRANSMISSION_BLOCK: kotlin.Char = '\u0017'

    /** [CANCEL](https://codepoints.net/U+0018) */
    public const val CANCEL: kotlin.Char = '\u0018'

    /** [END OF MEDIUM](https://codepoints.net/U+0019) */
    public const val END_OF_MEDIUM: kotlin.Char = '\u0019'

    /** [SUBSTITUTE](https://codepoints.net/U+001A) */
    public const val SUBSTITUTE: kotlin.Char = '\u001A'

    /** [ESCAPE](https://codepoints.net/U+001B) */
    public const val ESCAPE: kotlin.Char = '\u001B'

    /** Synonym for [ESCAPE] */
    public const val ESC: kotlin.Char = ESCAPE

    /** [INFORMATION SEPARATOR FOUR](https://codepoints.net/U+001C) */
    public const val INFORMATION_SEPARATOR_FOUR: kotlin.Char = '\u001C'

    /** [INFORMATION SEPARATOR THREE](https://codepoints.net/U+001D) */
    public const val INFORMATION_SEPARATOR_THREE: kotlin.Char = '\u001D'

    /** [INFORMATION_SEPARATOR_TWO](https://codepoints.net/U+001E) */
    public const val INFORMATION_SEPARATOR_TWO: kotlin.Char = '\u001E'

    /** [INFORMATION_SEPARATOR_ONE](https://codepoints.net/U+001F) */
    public const val INFORMATION_SEPARATOR_ONE: kotlin.Char = '\u001F'

    /** [DELETE](https://codepoints.net/U+007F) */
    public const val DELETE: kotlin.Char = '\u007F'

    /** [CONTROL SEQUENCE INTRODUCER](https://codepoints.net/U+009B) */
    public const val CONTROL_SEQUENCE_INTRODUCER: kotlin.Char = '\u009B'

    /** Synonym for [CONTROL_SEQUENCE_INTRODUCER] */
    public const val CSI: kotlin.Char = CONTROL_SEQUENCE_INTRODUCER

    /** [NO-BREAK SPACE](https://codepoints.net/U+00A0) */
    public const val NO_BREAK_SPACE: kotlin.Char = '\u00A0'

    /** Synonym for [NO_BREAK_SPACE] */
    public const val NBSP: kotlin.Char = NO_BREAK_SPACE

    /** [FIGURE SPACE](https://codepoints.net/U+2007) */
    public const val FIGURE_SPACE: kotlin.Char = '\u2007'

    /** [ZERO WIDTH SPACE](https://codepoints.net/U+200B) */
    public const val ZERO_WIDTH_SPACE: kotlin.Char = '\u200B'

    /** [ZERO WIDTH NON-JOINER](https://codepoints.net/U+200C) */
    public const val ZERO_WIDTH_NON_JOINER: kotlin.Char = '\u200C'

    /** [ZERO WIDTH JOINER](https://codepoints.net/U+200D) */
    public const val ZERO_WIDTH_JOINER: kotlin.Char = '\u200D'

    /** Synonym for [ZERO_WIDTH_JOINER] */
    public const val ZWJ: kotlin.Char = ZERO_WIDTH_JOINER

    /** [HORIZONTAL ELLIPSIS](https://codepoints.net/U+2026) */
    public const val HORIZONTAL_ELLIPSIS: kotlin.Char = '\u2026'

    /** Synonym for [HORIZONTAL_ELLIPSIS] */
    public const val ELLIPSIS: kotlin.Char = HORIZONTAL_ELLIPSIS


    /** [LINE SEPARATOR](https://codepoints.net/U+2028) */
    public const val LINE_SEPARATOR: kotlin.Char = '\u2028'

    /** [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029) */
    public const val PARAGRAPH_SEPARATOR: kotlin.Char = '\u2029'

    /** [NARROW NO-BREAK SPACE](https://codepoints.net/U+202F) */
    public const val NARROW_NO_BREAK_SPACE: kotlin.Char = '\u202F'

    /** [NEXT LINE (NEL)](https://codepoints.net/U+0085) */
    public const val NEXT_LINE: kotlin.Char = '\u0085'

    /** [PILCROW SIGN](https://codepoints.net/U+00B6) ¶ */
    public const val PILCROW_SIGN: kotlin.Char = '\u00B6'

    /** [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F) */
    public const val RIGHT_TO_LEFT_MARK: kotlin.Char = '\u200F'

    /** [SYMBOL FOR NULL](https://codepoints.net/U+2400) `␀` */
    public const val SYMBOL_FOR_NULL: kotlin.Char = '\u2400'

    /** [SYMBOL FOR START OF HEADING](https://codepoints.net/U+2401) `␁` */
    public const val SYMBOL_FOR_START_OF_HEADING: kotlin.Char = '\u2401'

    /** [SYMBOL FOR START OF TEXT](https://codepoints.net/U+2402) `␂` */
    public const val SYMBOL_FOR_START_OF_TEXT: kotlin.Char = '\u2402'

    /** [SYMBOL FOR END OF TEXT](https://codepoints.net/U+2403) `␃` */
    public const val SYMBOL_FOR_END_OF_TEXT: kotlin.Char = '\u2403'

    /** [SYMBOL FOR END OF TRANSMISSION](https://codepoints.net/U+2404) `␄` */
    public const val SYMBOL_FOR_END_OF_TRANSMISSION: kotlin.Char = '\u2404'

    /** [SYMBOL FOR ENQUIRY](https://codepoints.net/U+2405) `␅` */
    public const val SYMBOL_FOR_ENQUIRY: kotlin.Char = '\u2405'

    /** [SYMBOL FOR ACKNOWLEDGE](https://codepoints.net/U+2406) `␆` */
    public const val SYMBOL_FOR_ACKNOWLEDGE: kotlin.Char = '\u2406'

    /** [SYMBOL FOR BELL](https://codepoints.net/U+2407) `␇` */
    public const val SYMBOL_FOR_BELL: kotlin.Char = '\u2407'

    /** [SYMBOL FOR BACKSPACE](https://codepoints.net/U+2408) `␈` */
    public const val SYMBOL_FOR_BACKSPACE: kotlin.Char = '\u2408'

    /** [SYMBOL FOR HORIZONTAL TABULATION](https://codepoints.net/U+2409) `␉` */
    public const val SYMBOL_FOR_HORIZONTAL_TABULATION: kotlin.Char = '\u2409'

    /** [SYMBOL FOR LINE FEED](https://codepoints.net/U+240A) `␊` */
    public const val SYMBOL_FOR_LINE_FEED: kotlin.Char = '\u240A'

    /** [SYMBOL FOR VERTICAL TABULATION](https://codepoints.net/U+240B) `␋` */
    public const val SYMBOL_FOR_VERTICAL_TABULATION: kotlin.Char = '\u240B'

    /** [SYMBOL FOR FORM FEED](https://codepoints.net/U+240C) `␌` */
    public const val SYMBOL_FOR_FORM_FEED: kotlin.Char = '\u240C'

    /** [SYMBOL FOR CARRIAGE RETURN](https://codepoints.net/U+240D) `␍` */
    public const val SYMBOL_FOR_CARRIAGE_RETURN: kotlin.Char = '\u240D'

    /** [SYMBOL FOR SHIFT OUT](https://codepoints.net/U+240E) `␎` */
    public const val SYMBOL_FOR_SHIFT_OUT: kotlin.Char = '\u240E'

    /** [SYMBOL FOR SHIFT IN](https://codepoints.net/U+240F) `␏` */
    public const val SYMBOL_FOR_SHIFT_IN: kotlin.Char = '\u240F'

    /** [SYMBOL FOR DATA LINK ESCAPE](https://codepoints.net/U+2410) `␐` */
    public const val SYMBOL_FOR_DATA_LINK_ESCAPE: kotlin.Char = '\u2410'

    /** [SYMBOL FOR DEVICE CONTROL ONE](https://codepoints.net/U+2411) `␑` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_ONE: kotlin.Char = '\u2411'

    /** [SYMBOL FOR DEVICE CONTROL TWO](https://codepoints.net/U+2412) `␒` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_TWO: kotlin.Char = '\u2412'

    /** [SYMBOL FOR DEVICE CONTROL THREE](https://codepoints.net/U+2413) `␓` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_THREE: kotlin.Char = '\u2413'

    /** [SYMBOL FOR DEVICE CONTROL FOUR](https://codepoints.net/U+2414) `␔` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_FOUR: kotlin.Char = '\u2414'

    /** [SYMBOL FOR NEGATIVE ACKNOWLEDGE](https://codepoints.net/U+2415) `␕` */
    public const val SYMBOL_FOR_NEGATIVE_ACKNOWLEDGE: kotlin.Char = '\u2415'

    /** [SYMBOL FOR SYNCHRONOUS IDLE](https://codepoints.net/U+2416) `␖` */
    public const val SYMBOL_FOR_SYNCHRONOUS_IDLE: kotlin.Char = '\u2416'

    /** [SYMBOL FOR END OF TRANSMISSION BLOCK](https://codepoints.net/U+2417) `␗` */
    public const val SYMBOL_FOR_END_OF_TRANSMISSION_BLOCK: kotlin.Char = '\u2417'

    /** [SYMBOL FOR CANCEL](https://codepoints.net/U+2418) `␘` */
    public const val SYMBOL_FOR_CANCEL: kotlin.Char = '\u2418'

    /** [SYMBOL FOR END OF MEDIUM](https://codepoints.net/U+2419) `␙` */
    public const val SYMBOL_FOR_END_OF_MEDIUM: kotlin.Char = '\u2419'

    /** [SYMBOL FOR SUBSTITUTE](https://codepoints.net/U+241A) `␚` */
    public const val SYMBOL_FOR_SUBSTITUTE: kotlin.Char = '\u241A'

    /** [SYMBOL FOR ESCAPE](https://codepoints.net/U+241B) `␛` */
    public const val SYMBOL_FOR_ESCAPE: kotlin.Char = '\u241B'

    /** [SYMBOL FOR FILE SEPARATOR](https://codepoints.net/U+241C) `␜` */
    public const val SYMBOL_FOR_FILE_SEPARATOR: kotlin.Char = '\u241C'

    /** [SYMBOL FOR GROUP SEPARATOR](https://codepoints.net/U+241D) `␝` */
    public const val SYMBOL_FOR_GROUP_SEPARATOR: kotlin.Char = '\u241D'

    /** [SYMBOL FOR RECORD SEPARATOR](https://codepoints.net/U+241E) `␞` */
    public const val SYMBOL_FOR_RECORD_SEPARATOR: kotlin.Char = '\u241E'

    /** [SYMBOL FOR UNIT SEPARATOR](https://codepoints.net/U+241F) `␟` */
    public const val SYMBOL_FOR_UNIT_SEPARATOR: kotlin.Char = '\u241F'

    /** [SYMBOL FOR DELETE](https://codepoints.net/U+2421) `␡` */
    public const val SYMBOL_FOR_DELETE: kotlin.Char = '\u2421'
}
