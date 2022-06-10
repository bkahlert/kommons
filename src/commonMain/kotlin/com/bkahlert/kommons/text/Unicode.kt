package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.CodePoint.CodePointRange
import com.bkahlert.kommons.text.Unicode.UnicodeBlockMeta.Companion.metaFor

/**
 * Named Unicode code points, like [Unicode.LINE_FEED], [Unicode.SYMBOL_FOR_START_OF_HEADING], [Unicode.Emojis.BALLOT_BOX], etc.
 *
 * Further [Unicode.get] can be used to get a [CodePoint] by index.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
public object Unicode {

    /** Returns the [CodePoint] with the specified index. */
    public operator fun get(codePoint: Int): CodePoint = CodePoint(codePoint)

    /** [NULL](https://codepoints.net/U+0000) */
    public const val NULL: Char = '\u0000'

    /** [START OF HEADING](https://codepoints.net/U+0001) */
    public const val START_OF_HEADING: Char = '\u0001'

    /** [START OF TEXT](https://codepoints.net/U+0002) */
    public const val START_OF_TEXT: Char = '\u0002'

    /** [END OF TEXT](https://codepoints.net/U+0003) */
    public const val END_OF_TEXT: Char = '\u0003'

    /** [END OF TRANSMISSION](https://codepoints.net/U+0004) */
    public const val END_OF_TRANSMISSION: Char = '\u0004'

    /** [ENQUIRY](https://codepoints.net/U+0005) */
    public const val ENQUIRY: Char = '\u0005'

    /** [ACKNOWLEDGE](https://codepoints.net/U+0006) */
    public const val ACKNOWLEDGE: Char = '\u0006'

    /** [BELL](https://codepoints.net/U+0007) */
    public const val BELL: Char = '\u0007'

    /** [BACKSPACE](https://codepoints.net/U+0008) */
    public const val BACKSPACE: Char = '\u0008'

    /** [CHARACTER TABULATION](https://codepoints.net/U+0009) */
    public const val CHARACTER_TABULATION: Char = '\u0009'

    /** Synonym for [CHARACTER_TABULATION] */
    public const val TAB: Char = CHARACTER_TABULATION

    /** [LINE FEED (LF)](https://codepoints.net/U+000A) */
    public const val LINE_FEED: Char = '\u000A'

    /** [LINE TABULATION](https://codepoints.net/U+000B) */
    public const val LINE_TABULATION: Char = '\u000B'

    /** [FORM FEED (FF)](https://codepoints.net/U+000C) */
    public const val FORM_FEED: Char = '\u000C'

    /** [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D) */
    public const val CARRIAGE_RETURN: Char = '\u000D'

    /** [SHIFT OUT](https://codepoints.net/U+000E) */
    public const val SHIFT_OUT: Char = '\u000E'

    /** [SHIFT IN](https://codepoints.net/U+000F) */
    public const val SHIFT_IN: Char = '\u000F'

    /** [DATA LINK ESCAPE](https://codepoints.net/U+0010) */
    public const val DATA_LINK_ESCAPE: Char = '\u0010'

    /** [DEVICE CONTROL ONE](https://codepoints.net/U+0011) */
    public const val DEVICE_CONTROL_ONE: Char = '\u0011'

    /** [DEVICE CONTROL TWO](https://codepoints.net/U+0012) */
    public const val DEVICE_CONTROL_TWO: Char = '\u0012'

    /** [DEVICE CONTROL THREE](https://codepoints.net/U+0013) */
    public const val DEVICE_CONTROL_THREE: Char = '\u0013'

    /** [DEVICE CONTROL FOUR](https://codepoints.net/U+0014) */
    public const val DEVICE_CONTROL_FOUR: Char = '\u0014'

    /** [NEGATIVE ACKNOWLEDGE](https://codepoints.net/U+0015) */
    public const val NEGATIVE_ACKNOWLEDGE: Char = '\u0015'

    /** [SYNCHRONOUS IDLE](https://codepoints.net/U+0016) */
    public const val SYNCHRONOUS_IDLE: Char = '\u0016'

    /** [END OF TRANSMISSION BLOCK](https://codepoints.net/U+0017) */
    public const val END_OF_TRANSMISSION_BLOCK: Char = '\u0017'

    /** [CANCEL](https://codepoints.net/U+0018) */
    public const val CANCEL: Char = '\u0018'

    /** [END OF MEDIUM](https://codepoints.net/U+0019) */
    public const val END_OF_MEDIUM: Char = '\u0019'

    /** [SUBSTITUTE](https://codepoints.net/U+001A) */
    public const val SUBSTITUTE: Char = '\u001A'

    /** [ESCAPE](https://codepoints.net/U+001B) */
    public const val ESCAPE: Char = '\u001B'

    /** Synonym for [ESCAPE] */
    public const val ESC: Char = ESCAPE

    /** [INFORMATION SEPARATOR FOUR](https://codepoints.net/U+001C) */
    public const val INFORMATION_SEPARATOR_FOUR: Char = '\u001C'

    /** [INFORMATION SEPARATOR THREE](https://codepoints.net/U+001D) */
    public const val INFORMATION_SEPARATOR_THREE: Char = '\u001D'

    /** [INFORMATION_SEPARATOR_TWO](https://codepoints.net/U+001E) */
    public const val INFORMATION_SEPARATOR_TWO: Char = '\u001E'

    /** [INFORMATION_SEPARATOR_ONE](https://codepoints.net/U+001F) */
    public const val INFORMATION_SEPARATOR_ONE: Char = '\u001F'

    /** [DELETE](https://codepoints.net/U+007F) */
    public const val DELETE: Char = '\u007F'

    /** [CONTROL SEQUENCE INTRODUCER](https://codepoints.net/U+009B) */
    public const val CONTROL_SEQUENCE_INTRODUCER: Char = '\u009B'

    /** Synonym for [CONTROL_SEQUENCE_INTRODUCER] */
    public const val CSI: Char = CONTROL_SEQUENCE_INTRODUCER

    /** [NO-BREAK SPACE](https://codepoints.net/U+00A0) */
    public const val NO_BREAK_SPACE: Char = '\u00A0'

    /** Synonym for [NO_BREAK_SPACE] */
    public const val NBSP: Char = NO_BREAK_SPACE

    /** [FIGURE SPACE](https://codepoints.net/U+2007) */
    public const val FIGURE_SPACE: Char = '\u2007'

    /** [ZERO WIDTH SPACE](https://codepoints.net/U+200B) */
    public const val ZERO_WIDTH_SPACE: Char = '\u200B'

    /** [ZERO WIDTH NON-JOINER](https://codepoints.net/U+200C) */
    public const val ZERO_WIDTH_NON_JOINER: Char = '\u200C'

    /** [ZERO WIDTH JOINER](https://codepoints.net/U+200D) */
    public const val ZERO_WIDTH_JOINER: Char = '\u200D'

    /** Synonym for [ZERO_WIDTH_JOINER] */
    public const val ZWJ: Char = ZERO_WIDTH_JOINER

    /** [HORIZONTAL ELLIPSIS](https://codepoints.net/U+2026) */
    public const val HORIZONTAL_ELLIPSIS: Char = '\u2026'

    /** Synonym for [HORIZONTAL_ELLIPSIS] */
    public const val ELLIPSIS: Char = HORIZONTAL_ELLIPSIS


    /** [LINE SEPARATOR](https://codepoints.net/U+2028) */
    public const val LINE_SEPARATOR: Char = '\u2028'

    /** [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029) */
    public const val PARAGRAPH_SEPARATOR: Char = '\u2029'

    /** [NARROW NO-BREAK SPACE](https://codepoints.net/U+202F) */
    public const val NARROW_NO_BREAK_SPACE: Char = '\u202F'

    /** [NEXT LINE (NEL)](https://codepoints.net/U+0085) */
    public const val NEXT_LINE: Char = '\u0085'

    /** [PILCROW SIGN](https://codepoints.net/U+00B6) ¶ */
    public const val PILCROW_SIGN: Char = '\u00B6'

    /** [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F) */
    public const val RIGHT_TO_LEFT_MARK: Char = '\u200F'

    /** [SYMBOL FOR NULL](https://codepoints.net/U+2400) `␀` */
    public const val SYMBOL_FOR_NULL: Char = '\u2400'

    /** [SYMBOL FOR START OF HEADING](https://codepoints.net/U+2401) `␁` */
    public const val SYMBOL_FOR_START_OF_HEADING: Char = '\u2401'

    /** [SYMBOL FOR START OF TEXT](https://codepoints.net/U+2402) `␂` */
    public const val SYMBOL_FOR_START_OF_TEXT: Char = '\u2402'

    /** [SYMBOL FOR END OF TEXT](https://codepoints.net/U+2403) `␃` */
    public const val SYMBOL_FOR_END_OF_TEXT: Char = '\u2403'

    /** [SYMBOL FOR END OF TRANSMISSION](https://codepoints.net/U+2404) `␄` */
    public const val SYMBOL_FOR_END_OF_TRANSMISSION: Char = '\u2404'

    /** [SYMBOL FOR ENQUIRY](https://codepoints.net/U+2405) `␅` */
    public const val SYMBOL_FOR_ENQUIRY: Char = '\u2405'

    /** [SYMBOL FOR ACKNOWLEDGE](https://codepoints.net/U+2406) `␆` */
    public const val SYMBOL_FOR_ACKNOWLEDGE: Char = '\u2406'

    /** [SYMBOL FOR BELL](https://codepoints.net/U+2407) `␇` */
    public const val SYMBOL_FOR_BELL: Char = '\u2407'

    /** [SYMBOL FOR BACKSPACE](https://codepoints.net/U+2408) `␈` */
    public const val SYMBOL_FOR_BACKSPACE: Char = '\u2408'

    /** [SYMBOL FOR HORIZONTAL TABULATION](https://codepoints.net/U+2409) `␉` */
    public const val SYMBOL_FOR_HORIZONTAL_TABULATION: Char = '\u2409'

    /** [SYMBOL FOR LINE FEED](https://codepoints.net/U+240A) `␊` */
    public const val SYMBOL_FOR_LINE_FEED: Char = '\u240A'

    /** [SYMBOL FOR VERTICAL TABULATION](https://codepoints.net/U+240B) `␋` */
    public const val SYMBOL_FOR_VERTICAL_TABULATION: Char = '\u240B'

    /** [SYMBOL FOR FORM FEED](https://codepoints.net/U+240C) `␌` */
    public const val SYMBOL_FOR_FORM_FEED: Char = '\u240C'

    /** [SYMBOL FOR CARRIAGE RETURN](https://codepoints.net/U+240D) `␍` */
    public const val SYMBOL_FOR_CARRIAGE_RETURN: Char = '\u240D'

    /** [SYMBOL FOR SHIFT OUT](https://codepoints.net/U+240E) `␎` */
    public const val SYMBOL_FOR_SHIFT_OUT: Char = '\u240E'

    /** [SYMBOL FOR SHIFT IN](https://codepoints.net/U+240F) `␏` */
    public const val SYMBOL_FOR_SHIFT_IN: Char = '\u240F'

    /** [SYMBOL FOR DATA LINK ESCAPE](https://codepoints.net/U+2410) `␐` */
    public const val SYMBOL_FOR_DATA_LINK_ESCAPE: Char = '\u2410'

    /** [SYMBOL FOR DEVICE CONTROL ONE](https://codepoints.net/U+2411) `␑` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_ONE: Char = '\u2411'

    /** [SYMBOL FOR DEVICE CONTROL TWO](https://codepoints.net/U+2412) `␒` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_TWO: Char = '\u2412'

    /** [SYMBOL FOR DEVICE CONTROL THREE](https://codepoints.net/U+2413) `␓` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_THREE: Char = '\u2413'

    /** [SYMBOL FOR DEVICE CONTROL FOUR](https://codepoints.net/U+2414) `␔` */
    public const val SYMBOL_FOR_DEVICE_CONTROL_FOUR: Char = '\u2414'

    /** [SYMBOL FOR NEGATIVE ACKNOWLEDGE](https://codepoints.net/U+2415) `␕` */
    public const val SYMBOL_FOR_NEGATIVE_ACKNOWLEDGE: Char = '\u2415'

    /** [SYMBOL FOR SYNCHRONOUS IDLE](https://codepoints.net/U+2416) `␖` */
    public const val SYMBOL_FOR_SYNCHRONOUS_IDLE: Char = '\u2416'

    /** [SYMBOL FOR END OF TRANSMISSION BLOCK](https://codepoints.net/U+2417) `␗` */
    public const val SYMBOL_FOR_END_OF_TRANSMISSION_BLOCK: Char = '\u2417'

    /** [SYMBOL FOR CANCEL](https://codepoints.net/U+2418) `␘` */
    public const val SYMBOL_FOR_CANCEL: Char = '\u2418'

    /** [SYMBOL FOR END OF MEDIUM](https://codepoints.net/U+2419) `␙` */
    public const val SYMBOL_FOR_END_OF_MEDIUM: Char = '\u2419'

    /** [SYMBOL FOR SUBSTITUTE](https://codepoints.net/U+241A) `␚` */
    public const val SYMBOL_FOR_SUBSTITUTE: Char = '\u241A'

    /** [SYMBOL FOR ESCAPE](https://codepoints.net/U+241B) `␛` */
    public const val SYMBOL_FOR_ESCAPE: Char = '\u241B'

    /** [SYMBOL FOR FILE SEPARATOR](https://codepoints.net/U+241C) `␜` */
    public const val SYMBOL_FOR_FILE_SEPARATOR: Char = '\u241C'

    /** [SYMBOL FOR GROUP SEPARATOR](https://codepoints.net/U+241D) `␝` */
    public const val SYMBOL_FOR_GROUP_SEPARATOR: Char = '\u241D'

    /** [SYMBOL FOR RECORD SEPARATOR](https://codepoints.net/U+241E) `␞` */
    public const val SYMBOL_FOR_RECORD_SEPARATOR: Char = '\u241E'

    /** [SYMBOL FOR UNIT SEPARATOR](https://codepoints.net/U+241F) `␟` */
    public const val SYMBOL_FOR_UNIT_SEPARATOR: Char = '\u241F'

    /** [SYMBOL FOR DELETE](https://codepoints.net/U+2421) `␡` */
    public const val SYMBOL_FOR_DELETE: Char = '\u2421'

    /** Mapping of control characters to their respective symbols. */
    public val controlCharacters: Map<Char, Char> = mapOf(
        NULL to SYMBOL_FOR_NULL,
        START_OF_HEADING to SYMBOL_FOR_START_OF_HEADING,
        START_OF_TEXT to SYMBOL_FOR_START_OF_TEXT,
        END_OF_TEXT to SYMBOL_FOR_END_OF_TEXT,
        END_OF_TRANSMISSION to SYMBOL_FOR_END_OF_TRANSMISSION,
        ENQUIRY to SYMBOL_FOR_ENQUIRY,
        ACKNOWLEDGE to SYMBOL_FOR_ACKNOWLEDGE,
        BELL to SYMBOL_FOR_BELL,
        BACKSPACE to SYMBOL_FOR_BACKSPACE,
        CHARACTER_TABULATION to SYMBOL_FOR_HORIZONTAL_TABULATION,
        LINE_FEED to SYMBOL_FOR_LINE_FEED,
        LINE_TABULATION to SYMBOL_FOR_VERTICAL_TABULATION,
        FORM_FEED to SYMBOL_FOR_FORM_FEED,
        CARRIAGE_RETURN to SYMBOL_FOR_CARRIAGE_RETURN,
        SHIFT_OUT to SYMBOL_FOR_SHIFT_OUT,
        SHIFT_IN to SYMBOL_FOR_SHIFT_IN,
        DATA_LINK_ESCAPE to SYMBOL_FOR_DATA_LINK_ESCAPE,
        DEVICE_CONTROL_ONE to SYMBOL_FOR_DEVICE_CONTROL_ONE,
        DEVICE_CONTROL_TWO to SYMBOL_FOR_DEVICE_CONTROL_TWO,
        DEVICE_CONTROL_THREE to SYMBOL_FOR_DEVICE_CONTROL_THREE,
        DEVICE_CONTROL_FOUR to SYMBOL_FOR_DEVICE_CONTROL_FOUR,
        NEGATIVE_ACKNOWLEDGE to SYMBOL_FOR_NEGATIVE_ACKNOWLEDGE,
        SYNCHRONOUS_IDLE to SYMBOL_FOR_SYNCHRONOUS_IDLE,
        END_OF_TRANSMISSION_BLOCK to SYMBOL_FOR_END_OF_TRANSMISSION_BLOCK,
        CANCEL to SYMBOL_FOR_CANCEL,
        END_OF_MEDIUM to SYMBOL_FOR_END_OF_MEDIUM,
        SUBSTITUTE to SYMBOL_FOR_SUBSTITUTE,
        ESCAPE to SYMBOL_FOR_ESCAPE,
        INFORMATION_SEPARATOR_FOUR to SYMBOL_FOR_FILE_SEPARATOR,
        INFORMATION_SEPARATOR_THREE to SYMBOL_FOR_GROUP_SEPARATOR,
        INFORMATION_SEPARATOR_TWO to SYMBOL_FOR_RECORD_SEPARATOR,
        INFORMATION_SEPARATOR_ONE to SYMBOL_FOR_UNIT_SEPARATOR,
        DELETE to SYMBOL_FOR_DELETE,
    )

    /**
     * Contains this character's replacement symbol if any.
     *
     * This only applies to the so called [controlCharacters].
     */
    public val Char.replacementSymbol: Char? get() = controlCharacters[this]

    /**
     * Contains this code point's replacement symbol if any.
     *
     * This only applies to the so called [controlCharacters].
     */
    public val CodePoint.replacementSymbol: Char? get() = char?.replacementSymbol

    @Suppress("KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class BoxDrawings(override val range: CodePointRange = CodePoint("─")..CodePoint("╿")) : UnicodeBlock<BoxDrawings> {
        LightHorizontal, HeavyHorizontal, LightVertical, HeavyVertical, LightTripleDashHorizontal, HeavyTripleDashHorizontal, LightTripleDashVertical, HeavyTripleDashVertical, LightQuadrupleDashHorizontal, HeavyQuadrupleDashHorizontal, LightQuadrupleDashVertical, HeavyQuadrupleDashVertical, LightDownAndRight, DownLightAndRightHeavy, DownHeavyAndRightLight, HeavyDownAndRight, LightDownAndLeft, DownLightAndLeftHeavy, DownHeavyAndLeftLight, HeavyDownAndLeft, LightUpAndRight, UpLightAndRightHeavy, UpHeavyAndRightLight, HeavyUpAndRight, LightUpAndLeft, UpLightAndLeftHeavy, UpHeavyAndLeftLight, HeavyUpAndLeft, LightVerticalAndRight, VerticalLightAndRightHeavy, UpHeavyAndRightDownLight, DownHeavyAndRightUpLight, VerticalHeavyAndRightLight, DownLightAndRightUpHeavy, UpLightAndRightDownHeavy, HeavyVerticalAndRight, LightVerticalAndLeft, VerticalLightAndLeftHeavy, UpHeavyAndLeftDownLight, DownHeavyAndLeftUpLight, VerticalHeavyAndLeftLight, DownLightAndLeftUpHeavy, UpLightAndLeftDownHeavy, HeavyVerticalAndLeft, LightDownAndHorizontal, LeftHeavyAndRightDownLight, RightHeavyAndLeftDownLight, DownLightAndHorizontalHeavy, DownHeavyAndHorizontalLight, RightLightAndLeftDownHeavy, LeftLightAndRightDownHeavy, HeavyDownAndHorizontal, LightUpAndHorizontal, LeftHeavyAndRightUpLight, RightHeavyAndLeftUpLight, UpLightAndHorizontalHeavy, UpHeavyAndHorizontalLight, RightLightAndLeftUpHeavy, LeftLightAndRightUpHeavy, HeavyUpAndHorizontal, LightVerticalAndHorizontal, LeftHeavyAndRightVerticalLight, RightHeavyAndLeftVerticalLight, VerticalLightAndHorizontalHeavy, UpHeavyAndDownHorizontalLight, DownHeavyAndUpHorizontalLight, VerticalHeavyAndHorizontalLight, LeftUpHeavyAndRightDownLight, RightUpHeavyAndLeftDownLight, LeftDownHeavyAndRightUpLight, RightDownHeavyAndLeftUpLight, DownLightAndUpHorizontalHeavy, UpLightAndDownHorizontalHeavy, RightLightAndLeftVerticalHeavy, LeftLightAndRightVerticalHeavy, HeavyVerticalAndHorizontal, LightDoubleDashHorizontal, HeavyDoubleDashHorizontal, LightDoubleDashVertical, HeavyDoubleDashVertical, DoubleHorizontal, DoubleVertical, DownSingleAndRightDouble, DownDoubleAndRightSingle, DoubleDownAndRight, DownSingleAndLeftDouble, DownDoubleAndLeftSingle, DoubleDownAndLeft, UpSingleAndRightDouble, UpDoubleAndRightSingle, DoubleUpAndRight, UpSingleAndLeftDouble, UpDoubleAndLeftSingle, DoubleUpAndLeft, VerticalSingleAndRightDouble, VerticalDoubleAndRightSingle, DoubleVerticalAndRight, VerticalSingleAndLeftDouble, VerticalDoubleAndLeftSingle, DoubleVerticalAndLeft, DownSingleAndHorizontalDouble, DownDoubleAndHorizontalSingle, DoubleDownAndHorizontal, UpSingleAndHorizontalDouble, UpDoubleAndHorizontalSingle, DoubleUpAndHorizontal, VerticalSingleAndHorizontalDouble, VerticalDoubleAndHorizontalSingle, DoubleVerticalAndHorizontal, LightArcDownAndRight, LightArcDownAndLeft, LightArcUpAndLeft, LightArcUpAndRight, LightDiagonalUpperRightToLowerLeft, LightDiagonalUpperLeftToLowerRight, LightDiagonalCross, LightLeft, LightUp, LightRight, LightDown, HeavyLeft, HeavyUp, HeavyRight, HeavyDown, LightLeftAndHeavyRight, LightUpAndHeavyDown, HeavyLeftAndLightRight, HeavyUpAndLightDown;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<BoxDrawings> by metaFor()
    }

    @Suppress("KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarks(override val range: CodePointRange = CodePoint("̀")..CodePoint("ͯ")) : UnicodeBlock<CombiningDiacriticalMarks> {
        CombiningGraveAccent, CombiningAcuteAccent, CombiningCircumflexAccent, CombiningTilde, CombiningMacron, CombiningOverline, CombiningBreve, CombiningDotAbove, CombiningDiaeresis, CombiningHookAbove, CombiningRingAbove, CombiningDoubleAcuteAccent, CombiningCaron, CombiningVerticalLineAbove, CombiningDoubleVerticalLineAbove, CombiningDoubleGraveAccent, CombiningCandrabindu, CombiningInvertedBreve, CombiningTurnedCommaAbove, CombiningCommaAbove, CombiningReversedCommaAbove, CombiningCommaAboveRight, CombiningGraveAccentBelow, CombiningAcuteAccentBelow, CombiningLeftTackBelow, CombiningRightTackBelow, CombiningLeftAngleAbove, CombiningHorn, CombiningLeftHalfRingBelow, CombiningUpTackBelow, CombiningDownTackBelow, CombiningPlusSignBelow, CombiningMinusSignBelow, CombiningPalatalizedHookBelow, CombiningRetroflexHookBelow, CombiningDotBelow, CombiningDiaeresisBelow, CombiningRingBelow, CombiningCommaBelow, CombiningCedilla, CombiningOgonek, CombiningVerticalLineBelow, CombiningBridgeBelow, CombiningInvertedDoubleArchBelow, CombiningCaronBelow, CombiningCircumflexAccentBelow, CombiningBreveBelow, CombiningInvertedBreveBelow, CombiningTildeBelow, CombiningMacronBelow, CombiningLowLine, CombiningDoubleLowLine, CombiningTildeOverlay, CombiningShortStrokeOverlay, CombiningLongStrokeOverlay, CombiningShortSolidusOverlay, CombiningLongSolidusOverlay, CombiningRightHalfRingBelow, CombiningInvertedBridgeBelow, CombiningSquareBelow, CombiningSeagullBelow, CombiningXAbove, CombiningVerticalTilde, CombiningDoubleOverline, CombiningGraveToneMark, CombiningAcuteToneMark, CombiningGreekPerispomeni, CombiningGreekKoronis, CombiningGreekDialytikaTonos, CombiningGreekYpogegrammeni, CombiningBridgeAbove, CombiningEqualsSignBelow, CombiningDoubleVerticalLineBelow, CombiningLeftAngleBelow, CombiningNotTildeAbove, CombiningHomotheticAbove, CombiningAlmostEqualToAbove, CombiningLeftRightArrowBelow, CombiningUpwardsArrowBelow, CombiningGraphemeJoiner, CombiningRightArrowheadAbove, CombiningLeftHalfRingAbove, CombiningFermata, CombiningXBelow, CombiningLeftArrowheadBelow, CombiningRightArrowheadBelow, CombiningRightArrowheadAndUpArrowheadBelow, CombiningRightHalfRingAbove, CombiningDotAboveRight, CombiningAsteriskBelow, CombiningDoubleRingBelow, CombiningZigzagAbove, CombiningDoubleBreveBelow, CombiningDoubleBreve, CombiningDoubleMacron, CombiningDoubleMacronBelow, CombiningDoubleTilde, CombiningDoubleInvertedBreve, CombiningDoubleRightwardsArrowBelow, CombiningLatinSmallLetterA, CombiningLatinSmallLetterE, CombiningLatinSmallLetterI, CombiningLatinSmallLetterO, CombiningLatinSmallLetterU, CombiningLatinSmallLetterC, CombiningLatinSmallLetterD, CombiningLatinSmallLetterH, CombiningLatinSmallLetterM, CombiningLatinSmallLetterR, CombiningLatinSmallLetterT, CombiningLatinSmallLetterV, CombiningLatinSmallLetterX;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarks> by metaFor()
    }

    @Suppress("KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarksSupplementBlock(override val range: CodePointRange = CodePoint("᷀")..CodePoint("᷿")) :
        UnicodeBlock<CombiningDiacriticalMarksSupplementBlock> {
        CombiningDottedGraveAccent, CombiningDottedAcuteAccent, CombiningSnakeBelow, CombiningSuspensionMark, CombiningMacronAcute, CombiningGraveMacron, CombiningMacronGrave, CombiningAcuteMacron, CombiningGraveAcuteGrave, CombiningAcuteGraveAcute, CombiningLatinSmallLetterRBelow, CombiningBreveMacron, CombiningMacronBreve, CombiningDoubleCircumflexAbove, CombiningOgonekAbove, CombiningZigzagBelow, CombiningIsBelow, CombiningUrAbove, CombiningUsAbove, CombiningLatinSmallLetterFlattenedOpenAAbove, CombiningLatinSmallLetterAe, CombiningLatinSmallLetterAo, CombiningLatinSmallLetterAv, CombiningLatinSmallLetterCCedilla, CombiningLatinSmallLetterInsularD, CombiningLatinSmallLetterEth, CombiningLatinSmallLetterG, CombiningLatinLetterSmallCapitalG, CombiningLatinSmallLetterK, CombiningLatinSmallLetterL, CombiningLatinLetterSmallCapitalL, CombiningLatinLetterSmallCapitalM, CombiningLatinSmallLetterN, CombiningLatinLetterSmallCapitalN, CombiningLatinLetterSmallCapitalR, CombiningLatinSmallLetterRRotunda, CombiningLatinSmallLetterS, CombiningLatinSmallLetterLongS, CombiningLatinSmallLetterZ, CombiningDoubleInvertedBreveBelow, CombiningAlmostEqualToBelow, CombiningLeftArrowheadAbove, CombiningRightArrowheadAndDownArrowheadBelow;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarksSupplementBlock> by metaFor()
    }

    @Suppress("KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarksForSymbolsBlock(override val range: CodePointRange = CodePoint("⃐")..CodePoint("⃰")) :
        UnicodeBlock<CombiningDiacriticalMarksForSymbolsBlock> {
        CombiningLeftHarpoonAbove, CombiningRightHarpoonAbove, CombiningLongVerticalLineOverlay, CombiningShortVerticalLineOverlay, CombiningAnticlockwiseArrowAbove, CombiningClockwiseArrowAbove, CombiningLeftArrowAbove, CombiningRightArrowAbove, CombiningRingOverlay, CombiningClockwiseRingOverlay, CombiningAnticlockwiseRingOverlay, CombiningThreeDotsAbove, CombiningFourDotsAbove, CombiningEnclosingCircle, CombiningEnclosingSquare, CombiningEnclosingDiamond, CombiningEnclosingCircleBackslash, CombiningLeftRightArrowAbove, CombiningEnclosingScreen, CombiningEnclosingKeycap, CombiningEnclosingUpwardPointingTriangle, CombiningReverseSolidusOverlay, CombiningDoubleVerticalStrokeOverlay, CombiningAnnuitySymbol, CombiningTripleUnderdot, CombiningWideBridgeAbove, CombiningLeftwardsArrowOverlay, CombiningLongDoubleSolidusOverlay, CombiningRightwardsHarpoonWithBarbDownwards, CombiningLeftwardsHarpoonWithBarbDownwards, CombiningLeftArrowBelow, CombiningRightArrowBelow, CombiningAsteriskAbove;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarksForSymbolsBlock> by metaFor()
    }

    @Suppress("KDocMissingDocumentation", "LongLine")
    public enum class CombiningHalfMarksBlock(override val range: CodePointRange = CodePoint("︠")..CodePoint("︦")) : UnicodeBlock<CombiningHalfMarksBlock> {
        CombiningLigatureLeftHalf, CombiningLigatureRightHalf, CombiningDoubleTildeLeftHalf, CombiningDoubleTildeRightHalf, CombiningMacronLeftHalf, CombiningMacronRightHalf, CombiningConjoiningMacron;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningHalfMarksBlock> by metaFor()
    }

    /** [ZERO WIDTH NO-BREAK SPACE](https://codepoints.net/U+FEFF) */
    public const val ZERO_WIDTH_NO_BREAK_SPACE: Char = '\uFEFF'

    /** [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) `�` */
    public const val REPLACEMENT_CHARACTER: Char = '\uFFFD'

    /** [GREEK LETTER KOPPA](https://codepoints.net/U+03DE) `Ϟ` */
    @Suppress("SpellCheckingInspection") public const val GREEK_LETTER_KOPPA: Char = 'Ϟ'

    /** [GREEK SMALL LETTER KOPPA](https://codepoints.net/U+03DF) `ϟ` */
    @Suppress("SpellCheckingInspection") public const val GREEK_SMALL_LETTER_KOPPA: Char = 'ϟ'

    /** [TRIPLE VERTICAL BAR DELIMITER](https://codepoints.net/U+2980) `⦀` */
    public const val TRIPLE_VERTICAL_BAR_DELIMITER: Char = '⦀'

    /**
     * Unicode emojis as specified by the [Unicode® Technical Standard #51](https://unicode.org/reports/tr51/)
     */
    public object Emojis {

        /**
         * Emoji, e.g. `😀`
         */
        public class Emoji(private val emoji: String) :
            CharSequence by emoji.removeSuffix(VARIATION_SELECTOR_15.toString()).removeSuffix(VARIATION_SELECTOR_16.toString()) {
            public constructor(emoji: Char) : this(emoji.toString())

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

        private val fullHourClocks = listOf("🕛", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚").toIndexMap()
        private val halfHourClocks = listOf("🕧", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦").toIndexMap()
        private fun List<String>.toIndexMap() = mapIndexed { index, clock -> index to Emoji(clock) }.toMap()

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding full hour, e.g. `3` will return a "3 o'clock"/🕒 emoji.
         *
         * The dictionary applies the [mod] operation. Consequently, all multiples of 12 of a certain hour (e.g. `15` will return a "3 o'clock"/🕒 emoji)
         * will also return the corresponding hour.
         */
        public object FullHoursDictionary {
            public operator fun get(key: Int): Emoji = fullHourClocks[key.mod(fullHourClocks.size)] ?: error("Missing clock in dictionary")
        }

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding next half hour, e.g. `3` will return a "3:30 o'clock"/🕞 emoji.
         *
         * This dictionary applies the [mod] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3:30 o'clock"/🕞 emoji)
         * will also return the corresponding next half hour.
         */
        public object HalfHoursDictionary {
            public operator fun get(key: Int): Emoji = halfHourClocks[key.mod(halfHourClocks.size)] ?: error("Missing clock in dictionary")
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
        public const val VARIATION_SELECTOR_15: Char = '︎'

        /**
         * [VARIATION SELECTOR-16](https://codepoints.net/U+FE0F)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0F forces it to be rendered
         * as a colorful image as compared to a monochrome text variant."</cite>
         */
        public const val VARIATION_SELECTOR_16: Char = '️'
    }

    /**
     * [Tai Xuan Jing Symbols](https://codepoints.net/tai_xuan_jing_symbols)
     *
     * Block from `U+1D300` to `U+1D35F`. This block was introduced in Unicode version 4.0 (2003). It contains 87 codepoints.
     *
     * `𝌀` to `𝍖`
     */
    @Suppress("SpellCheckingInspection")
    public object DivinationSymbols {
        @Suppress("KDocMissingDocumentation", "LongLine")
        public enum class Monograms(override val range: CodePointRange = CodePoint("𝌀")..CodePoint("𝌀")) : UnicodeBlock<Monograms> {
            Earth;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Monograms> by metaFor()
        }

        @Suppress("KDocMissingDocumentation", "LongLine")
        public enum class Digrams(override val range: CodePointRange = CodePoint("𝌁")..CodePoint("𝌅")) : UnicodeBlock<Digrams> {
            HeavenlyEarth, HumanEarth, EarthlyHeaven, EarthlyHuman, Earth;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Digrams> by metaFor()
        }

        @Suppress("KDocMissingDocumentation", "LongLine")
        public enum class Tetragrams(override val range: CodePointRange = CodePoint("𝌆")..CodePoint("𝍖")) : UnicodeBlock<Tetragrams> {
            Centre, FullCircle, Mired, Barrier, KeepingSmall, Contrariety, Ascent, Opposition, BranchingOut, DefectivenessOrDistortion, Divergence, Youthfulness, Increase, Penetration, Reach, Contact, HoldingBack, Waiting, Following, Advance, Release, Resistance, Ease, Joy, Contention, Endeavour, Duties, Change, Decisiveness, BoldResolution, Packing, Legion, Closeness, Kinship, Gathering, Strength, Purity, Fullness, Residence, LawOrModel, Response, GoingToMeet, Encounters, Stove, Greatness, Enlargement, Pattern, Ritual, Flight, VastnessOrWasting, Constancy, Measure, Eternity, Unity, Diminishment, ClosedMouth, Guardedness, GatheringIn, Massing, Accumulation, Embellishment, Doubt, Watch, Sinking, Inner, Departure, Darkening, Dimming, Exhaustion, Severance, Stoppage, Hardness, Completion, Closure, Failure, Aggravation, Compliance, OnTheVerge, Difficulties, Labouring, Fostering;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Tetragrams> by metaFor()
        }
    }

    /**
     * Interface to facilitate implementing named, enumerable Unicode code points by their names.
     *
     * @sample BoxDrawings
     */
    public interface UnicodeBlock<T : Enum<T>> : ClosedRange<CodePoint> {
        override val start: CodePoint get() = range.start
        override val endInclusive: CodePoint get() = range.endInclusive
        override fun contains(value: CodePoint): Boolean = range.contains(value)
        override fun isEmpty(): Boolean = range.isEmpty()

        public val range: CodePointRange
        public val ordinal: Int
        public val string: String get() = (range.first + ordinal).string
    }

    /**
     * Meta information about a [UnicodeBlock].
     */
    public interface UnicodeBlockMeta<T> where T : UnicodeBlock<T>, T : Enum<T> {
        public val valueCount: Int
        public val unicodeBlock: UnicodeBlock<T>
        public val codePointCount: Int
        public val isValid: Boolean
        public val name: String

        public companion object {
            public inline fun <reified T> metaFor(): UnicodeBlockMeta<T> where T : Enum<T>, T : UnicodeBlock<T> =
                SimpleUnicodeBlockMeta(enumValues())
        }


        public class SimpleUnicodeBlockMeta<T>(public val values: Array<T>) : UnicodeBlockMeta<T> where T : Enum<T>, T : UnicodeBlock<T> {
            override val valueCount: Int by lazy { values.size }
            override val unicodeBlock: UnicodeBlock<T> by lazy { values.first() }
            override val codePointCount: Int by lazy { unicodeBlock.range.last - unicodeBlock.range.first + 1 }
            override val isValid: Boolean by lazy { valueCount == codePointCount }
            override val name: String by lazy { toString().split("$").last { !it.contains("Companion") } }
        }
    }
}
