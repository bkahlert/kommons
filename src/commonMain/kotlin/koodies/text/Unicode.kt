package koodies.text

import koodies.math.mod
import koodies.text.CodePoint.CodePointRange
import koodies.text.Unicode.UnicodeBlockMeta.Companion.metaFor

public object Unicode {

    /**
     * Returns the [CodePoint] with the specified index.
     */
    public operator fun get(codePoint: Int): CodePoint = CodePoint(codePoint)

    /**
     * [START OF HEADING](https://codepoints.net/U+0001)
     */
    public const val startOfHeading: Char = '\u0001'

    /**
     * [BELL](https://codepoints.net/U+0007)
     */
    public const val bell: Char = '\u0007'

    /**
     * [CHARACTER TABULATION](https://codepoints.net/U+0009)
     */
    public const val characterTabulation: Char = '\u0009'

    /**
     * [ESCAPE](https://codepoints.net/U+001B)
     */
    public const val escape: Char = '\u001B'

    /**
     * [CONTROL SEQUENCE INTRODUCER](https://codepoints.net/U+009B)
     */
    public const val controlSequenceIntroducer: Char = '\u009B'

    /**
     * [NO-BREAK SPACE](https://codepoints.net/U+00A0)
     */
    public const val NO_BREAK_SPACE: Char = '\u00A0'
    public const val NBSP: Char = NO_BREAK_SPACE

    /**
     * [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D)
     */
    public const val carriageReturn: Char = '\r'

    /**
     * [LINE FEED (LF)](https://codepoints.net/U+000A)
     */
    public const val lineFeed: Char = '\n'

    /**
     * [FIGURE SPACE](https://codepoints.net/U+2007)
     */
    public const val figureSpace: Char = '\u2007'

    /**
     * [ZERO WIDTH SPACE](https://codepoints.net/U+200B)
     */
    public const val zeroWidthSpace: Char = '\u200B'

    /**
     * [ZERO WIDTH NON-JOINER](https://codepoints.net/U+200C)
     */
    public const val zeroWidthNonJoiner: Char = '\u200C'

    /**
     * [ZERO WIDTH JOINER](https://codepoints.net/U+200D)
     */
    public const val zeroWidthJoiner: Char = '\u200D'

    /**
     * [LINE SEPARATOR](https://codepoints.net/U+2028)
     */
    public const val lineSeparator: Char = '\u2028'

    /**
     * [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029)
     */
    public const val paragraphSeparator: Char = '\u2029'

    /**
     * [NARROW NO-BREAK SPACE](https://codepoints.net/U+202F)
     */
    public const val narrowNoBreakSpace: Char = '\u202F'

    /**
     * [NEXT LINE (NEL)](https://codepoints.net/U+0085)
     */
    public const val nextLine: Char = '\u0085'

    /**
     * [PILCROW SIGN](https://codepoints.net/U+00B6) ¶
     */
    public const val pilcrowSign: Char = '\u00B6'

    /**
     * [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F)
     */
    public const val rightToLeftMark: Char = '\u200F'

    /**
     * [Tai Xuan Jing Symbols](https://codepoints.net/tai_xuan_jing_symbols)
     *
     * Block from `U+1D300` to `U+1D35F`. This block was introduced in Unicode version 4.0 (2003). It contains 87 codepoints.
     *
     * `𝌀` to `𝍖`
     */
    public object DivinationSymbols {
        @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
        public enum class Monograms(override val range: CodePointRange = CodePoint("𝌀")..CodePoint("𝌀")) : UnicodeBlock<Monograms> {
            Earth;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Monograms> by metaFor()
        }

        @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
        public enum class Digrams(override val range: CodePointRange = CodePoint("𝌁")..CodePoint("𝌅")) : UnicodeBlock<Digrams> {
            HeavenlyEarth, HumanEarth, EarthlyHeaven, EarthlyHuman, Earth;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Digrams> by metaFor()
        }

        @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
        public enum class Tetragrams(override val range: CodePointRange = CodePoint("𝌆")..CodePoint("𝍖")) : UnicodeBlock<Tetragrams> {
            Centre, FullCircle, Mired, Barrier, KeepingSmall, Contrariety, Ascent, Opposition, BranchingOut, DefectivenessOrDistortion, Divergence, Youthfulness, Increase, Penetration, Reach, Contact, HoldingBack, Waiting, Following, Advance, Release, Resistance, Ease, Joy, Contention, Endeavour, Duties, Change, Decisiveness, BoldResolution, Packing, Legion, Closeness, Kinship, Gathering, Strength, Purity, Fullness, Residence, LawOrModel, Response, GoingToMeet, Encounters, Stove, Greatness, Enlargement, Pattern, Ritual, Flight, VastnessOrWasting, Constancy, Measure, Eternity, Unity, Diminishment, ClosedMouth, Guardedness, GatheringIn, Massing, Accumulation, Embellishment, Doubt, Watch, Sinking, Inner, Departure, Darkening, Dimming, Exhaustion, Severance, Stoppage, Hardness, Completion, Closure, Failure, Aggravation, Compliance, OnTheVerge, Difficulties, Labouring, Fostering;

            override fun toString(): String = string

            public companion object : UnicodeBlockMeta<Tetragrams> by metaFor()
        }
    }

    @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class BoxDrawings(override val range: CodePointRange = CodePoint("─")..CodePoint("╿")) : UnicodeBlock<BoxDrawings> {
        LightHorizontal, HeavyHorizontal, LightVertical, HeavyVertical, LightTripleDashHorizontal, HeavyTripleDashHorizontal, LightTripleDashVertical, HeavyTripleDashVertical, LightQuadrupleDashHorizontal, HeavyQuadrupleDashHorizontal, LightQuadrupleDashVertical, HeavyQuadrupleDashVertical, LightDownAndRight, DownLightAndRightHeavy, DownHeavyAndRightLight, HeavyDownAndRight, LightDownAndLeft, DownLightAndLeftHeavy, DownHeavyAndLeftLight, HeavyDownAndLeft, LightUpAndRight, UpLightAndRightHeavy, UpHeavyAndRightLight, HeavyUpAndRight, LightUpAndLeft, UpLightAndLeftHeavy, UpHeavyAndLeftLight, HeavyUpAndLeft, LightVerticalAndRight, VerticalLightAndRightHeavy, UpHeavyAndRightDownLight, DownHeavyAndRightUpLight, VerticalHeavyAndRightLight, DownLightAndRightUpHeavy, UpLightAndRightDownHeavy, HeavyVerticalAndRight, LightVerticalAndLeft, VerticalLightAndLeftHeavy, UpHeavyAndLeftDownLight, DownHeavyAndLeftUpLight, VerticalHeavyAndLeftLight, DownLightAndLeftUpHeavy, UpLightAndLeftDownHeavy, HeavyVerticalAndLeft, LightDownAndHorizontal, LeftHeavyAndRightDownLight, RightHeavyAndLeftDownLight, DownLightAndHorizontalHeavy, DownHeavyAndHorizontalLight, RightLightAndLeftDownHeavy, LeftLightAndRightDownHeavy, HeavyDownAndHorizontal, LightUpAndHorizontal, LeftHeavyAndRightUpLight, RightHeavyAndLeftUpLight, UpLightAndHorizontalHeavy, UpHeavyAndHorizontalLight, RightLightAndLeftUpHeavy, LeftLightAndRightUpHeavy, HeavyUpAndHorizontal, LightVerticalAndHorizontal, LeftHeavyAndRightVerticalLight, RightHeavyAndLeftVerticalLight, VerticalLightAndHorizontalHeavy, UpHeavyAndDownHorizontalLight, DownHeavyAndUpHorizontalLight, VerticalHeavyAndHorizontalLight, LeftUpHeavyAndRightDownLight, RightUpHeavyAndLeftDownLight, LeftDownHeavyAndRightUpLight, RightDownHeavyAndLeftUpLight, DownLightAndUpHorizontalHeavy, UpLightAndDownHorizontalHeavy, RightLightAndLeftVerticalHeavy, LeftLightAndRightVerticalHeavy, HeavyVerticalAndHorizontal, LightDoubleDashHorizontal, HeavyDoubleDashHorizontal, LightDoubleDashVertical, HeavyDoubleDashVertical, DoubleHorizontal, DoubleVertical, DownSingleAndRightDouble, DownDoubleAndRightSingle, DoubleDownAndRight, DownSingleAndLeftDouble, DownDoubleAndLeftSingle, DoubleDownAndLeft, UpSingleAndRightDouble, UpDoubleAndRightSingle, DoubleUpAndRight, UpSingleAndLeftDouble, UpDoubleAndLeftSingle, DoubleUpAndLeft, VerticalSingleAndRightDouble, VerticalDoubleAndRightSingle, DoubleVerticalAndRight, VerticalSingleAndLeftDouble, VerticalDoubleAndLeftSingle, DoubleVerticalAndLeft, DownSingleAndHorizontalDouble, DownDoubleAndHorizontalSingle, DoubleDownAndHorizontal, UpSingleAndHorizontalDouble, UpDoubleAndHorizontalSingle, DoubleUpAndHorizontal, VerticalSingleAndHorizontalDouble, VerticalDoubleAndHorizontalSingle, DoubleVerticalAndHorizontal, LightArcDownAndRight, LightArcDownAndLeft, LightArcUpAndLeft, LightArcUpAndRight, LightDiagonalUpperRightToLowerLeft, LightDiagonalUpperLeftToLowerRight, LightDiagonalCross, LightLeft, LightUp, LightRight, LightDown, HeavyLeft, HeavyUp, HeavyRight, HeavyDown, LightLeftAndHeavyRight, LightUpAndHeavyDown, HeavyLeftAndLightRight, HeavyUpAndLightDown;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<BoxDrawings> by metaFor()
    }

    public val boxDrawings: List<Char> = ('\u2500'..'\u257F').toList()

    @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarks(override val range: CodePointRange = CodePoint("̀")..CodePoint("ͯ")) : UnicodeBlock<CombiningDiacriticalMarks> {
        CombiningGraveAccent, CombiningAcuteAccent, CombiningCircumflexAccent, CombiningTilde, CombiningMacron, CombiningOverline, CombiningBreve, CombiningDotAbove, CombiningDiaeresis, CombiningHookAbove, CombiningRingAbove, CombiningDoubleAcuteAccent, CombiningCaron, CombiningVerticalLineAbove, CombiningDoubleVerticalLineAbove, CombiningDoubleGraveAccent, CombiningCandrabindu, CombiningInvertedBreve, CombiningTurnedCommaAbove, CombiningCommaAbove, CombiningReversedCommaAbove, CombiningCommaAboveRight, CombiningGraveAccentBelow, CombiningAcuteAccentBelow, CombiningLeftTackBelow, CombiningRightTackBelow, CombiningLeftAngleAbove, CombiningHorn, CombiningLeftHalfRingBelow, CombiningUpTackBelow, CombiningDownTackBelow, CombiningPlusSignBelow, CombiningMinusSignBelow, CombiningPalatalizedHookBelow, CombiningRetroflexHookBelow, CombiningDotBelow, CombiningDiaeresisBelow, CombiningRingBelow, CombiningCommaBelow, CombiningCedilla, CombiningOgonek, CombiningVerticalLineBelow, CombiningBridgeBelow, CombiningInvertedDoubleArchBelow, CombiningCaronBelow, CombiningCircumflexAccentBelow, CombiningBreveBelow, CombiningInvertedBreveBelow, CombiningTildeBelow, CombiningMacronBelow, CombiningLowLine, CombiningDoubleLowLine, CombiningTildeOverlay, CombiningShortStrokeOverlay, CombiningLongStrokeOverlay, CombiningShortSolidusOverlay, CombiningLongSolidusOverlay, CombiningRightHalfRingBelow, CombiningInvertedBridgeBelow, CombiningSquareBelow, CombiningSeagullBelow, CombiningXAbove, CombiningVerticalTilde, CombiningDoubleOverline, CombiningGraveToneMark, CombiningAcuteToneMark, CombiningGreekPerispomeni, CombiningGreekKoronis, CombiningGreekDialytikaTonos, CombiningGreekYpogegrammeni, CombiningBridgeAbove, CombiningEqualsSignBelow, CombiningDoubleVerticalLineBelow, CombiningLeftAngleBelow, CombiningNotTildeAbove, CombiningHomotheticAbove, CombiningAlmostEqualToAbove, CombiningLeftRightArrowBelow, CombiningUpwardsArrowBelow, CombiningGraphemeJoiner, CombiningRightArrowheadAbove, CombiningLeftHalfRingAbove, CombiningFermata, CombiningXBelow, CombiningLeftArrowheadBelow, CombiningRightArrowheadBelow, CombiningRightArrowheadAndUpArrowheadBelow, CombiningRightHalfRingAbove, CombiningDotAboveRight, CombiningAsteriskBelow, CombiningDoubleRingBelow, CombiningZigzagAbove, CombiningDoubleBreveBelow, CombiningDoubleBreve, CombiningDoubleMacron, CombiningDoubleMacronBelow, CombiningDoubleTilde, CombiningDoubleInvertedBreve, CombiningDoubleRightwardsArrowBelow, CombiningLatinSmallLetterA, CombiningLatinSmallLetterE, CombiningLatinSmallLetterI, CombiningLatinSmallLetterO, CombiningLatinSmallLetterU, CombiningLatinSmallLetterC, CombiningLatinSmallLetterD, CombiningLatinSmallLetterH, CombiningLatinSmallLetterM, CombiningLatinSmallLetterR, CombiningLatinSmallLetterT, CombiningLatinSmallLetterV, CombiningLatinSmallLetterX;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarks> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarksSupplementBlock(override val range: CodePointRange = CodePoint("᷀")..CodePoint("᷿")) :
        UnicodeBlock<CombiningDiacriticalMarksSupplementBlock> {
        CombiningDottedGraveAccent, CombiningDottedAcuteAccent, CombiningSnakeBelow, CombiningSuspensionMark, CombiningMacronAcute, CombiningGraveMacron, CombiningMacronGrave, CombiningAcuteMacron, CombiningGraveAcuteGrave, CombiningAcuteGraveAcute, CombiningLatinSmallLetterRBelow, CombiningBreveMacron, CombiningMacronBreve, CombiningDoubleCircumflexAbove, CombiningOgonekAbove, CombiningZigzagBelow, CombiningIsBelow, CombiningUrAbove, CombiningUsAbove, CombiningLatinSmallLetterFlattenedOpenAAbove, CombiningLatinSmallLetterAe, CombiningLatinSmallLetterAo, CombiningLatinSmallLetterAv, CombiningLatinSmallLetterCCedilla, CombiningLatinSmallLetterInsularD, CombiningLatinSmallLetterEth, CombiningLatinSmallLetterG, CombiningLatinLetterSmallCapitalG, CombiningLatinSmallLetterK, CombiningLatinSmallLetterL, CombiningLatinLetterSmallCapitalL, CombiningLatinLetterSmallCapitalM, CombiningLatinSmallLetterN, CombiningLatinLetterSmallCapitalN, CombiningLatinLetterSmallCapitalR, CombiningLatinSmallLetterRRotunda, CombiningLatinSmallLetterS, CombiningLatinSmallLetterLongS, CombiningLatinSmallLetterZ, CombiningDoubleInvertedBreveBelow, CombiningAlmostEqualToBelow, CombiningLeftArrowheadAbove, CombiningRightArrowheadAndDownArrowheadBelow;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarksSupplementBlock> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation", "SpellCheckingInspection", "LongLine")
    public enum class CombiningDiacriticalMarksForSymbolsBlock(override val range: CodePointRange = CodePoint("⃐")..CodePoint("⃰")) :
        UnicodeBlock<CombiningDiacriticalMarksForSymbolsBlock> {
        CombiningLeftHarpoonAbove, CombiningRightHarpoonAbove, CombiningLongVerticalLineOverlay, CombiningShortVerticalLineOverlay, CombiningAnticlockwiseArrowAbove, CombiningClockwiseArrowAbove, CombiningLeftArrowAbove, CombiningRightArrowAbove, CombiningRingOverlay, CombiningClockwiseRingOverlay, CombiningAnticlockwiseRingOverlay, CombiningThreeDotsAbove, CombiningFourDotsAbove, CombiningEnclosingCircle, CombiningEnclosingSquare, CombiningEnclosingDiamond, CombiningEnclosingCircleBackslash, CombiningLeftRightArrowAbove, CombiningEnclosingScreen, CombiningEnclosingKeycap, CombiningEnclosingUpwardPointingTriangle, CombiningReverseSolidusOverlay, CombiningDoubleVerticalStrokeOverlay, CombiningAnnuitySymbol, CombiningTripleUnderdot, CombiningWideBridgeAbove, CombiningLeftwardsArrowOverlay, CombiningLongDoubleSolidusOverlay, CombiningRightwardsHarpoonWithBarbDownwards, CombiningLeftwardsHarpoonWithBarbDownwards, CombiningLeftArrowBelow, CombiningRightArrowBelow, CombiningAsteriskAbove;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningDiacriticalMarksForSymbolsBlock> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation")
    public enum class CombiningHalfMarksBlock(override val range: CodePointRange = CodePoint("︠")..CodePoint("︦")) : UnicodeBlock<CombiningHalfMarksBlock> {
        CombiningLigatureLeftHalf, CombiningLigatureRightHalf, CombiningDoubleTildeLeftHalf, CombiningDoubleTildeRightHalf, CombiningMacronLeftHalf, CombiningMacronRightHalf, CombiningConjoiningMacron;

        override fun toString(): String = string

        public companion object : UnicodeBlockMeta<CombiningHalfMarksBlock> by metaFor()
    }

    @Suppress("SpellCheckingInspection")
    public val whitespaces: List<Char> by lazy { Whitespaces.asChars }

    public val controlCharacters: Map<Char, Char> = mapOf(
        '\u0000' to '\u2400', // ␀
        '\u0001' to '\u2401', // ␁
        '\u0002' to '\u2402', // ␂
        '\u0003' to '\u2403', // ␃
        '\u0004' to '\u2404', // ␄
        '\u0005' to '\u2405', // ␅
        '\u0006' to '\u2406', // ␆
        '\u0007' to '\u2407', // ␇
        '\u0008' to '\u2408', // ␈
        '\u0009' to '\u2409', // ␉
        '\u000A' to '\u240A', // ␊
        '\u000B' to '\u240B', // ␋
        '\u000C' to '\u240C', // ␌
        '\u000D' to '\u240D', // ␍
        '\u000E' to '\u240E', // ␎
        '\u000F' to '\u240F', // ␏
        '\u0010' to '\u2410', // ␐
        '\u0011' to '\u2411', // ␑
        '\u0012' to '\u2412', // ␒
        '\u0013' to '\u2413', // ␓
        '\u0014' to '\u2414', // ␔
        '\u0015' to '\u2415', // ␕
        '\u0016' to '\u2416', // ␖
        '\u0017' to '\u2417', // ␗
        '\u0018' to '\u2418', // ␘
        '\u0019' to '\u2419', // ␙
        '\u001A' to '\u241A', // ␚
        '\u001B' to '\u241B', // ␛
        '\u001C' to '\u241C', // ␜
        '\u001D' to '\u241D', // ␝
        '\u001E' to '\u241E', // ␞
        '\u001F' to '\u241F', // ␟
        '\u007F' to '\u2421', // ␡
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


    /**
     * [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) �
     */
    public const val replacementCharacter: Char = '\uFFFD'


    /**
     * [GREEK LETTER KOPPA](https://codepoints.net/U+03DE) Ϟ
     */
    public const val greekLetterKoppa: Char = 'Ϟ'

    /**
     * [GREEK SMALL LETTER KOPPA](https://codepoints.net/U+03DF) ϟ
     */
    public const val greekSmallLetterKoppa: Char = 'ϟ'

    /**
     * [TRIPLE VERTICAL BAR DELIMITER](https://codepoints.net/U+2980) ⦀
     */
    public const val tripleVerticalBarDelimiter: Char = '⦀'

    /**
     * Unicode emojis as specified by the [Unicode® Technical Standard #51](https://unicode.org/reports/tr51/) 🤓
     */
    public object Emojis {

        public class Emoji(private val emoji: String) :
            CharSequence by emoji.removeSuffix(variationSelector15.toString()).removeSuffix(variationSelector16.toString()) {
            public constructor(emoji: Char) : this(emoji.toString())

            public val textVariant: String get() = "$emoji$variationSelector15"
            public val emojiVariant: String get() = "$emoji$variationSelector16"

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
         * The dictionary applies the [rem] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3 o'clock"/🕒 emoji)
         * will also return the corresponding hour.
         */
        public object FullHoursDictionary {
            public operator fun get(key: Int): Emoji = fullHourClocks[key.mod(fullHourClocks.size)] ?: error("Missing clock in dictionary")
        }

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding next half hour, e.g. `3` will return a "3:30 o'clock"/🕞 emoji.
         *
         * This dictionary applies the [rem] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3:30 o'clock"/🕞 emoji)
         * will also return the corresponding next half hour.
         */
        public object HalfHoursDictionary {
            public operator fun get(key: Int): Emoji = halfHourClocks[key.mod(halfHourClocks.size)] ?: error("Missing clock in dictionary")
        }

        /**
         * [HOURGLASS](https://codepoints.net/U+231B) ⌛️ ⌛︎
         */
        public val hourglass: Emoji = Emoji('⌛')

        /**
         * [HOURGLASS WITH FLOWING SAND](https://codepoints.net/U+23F3) ⏳️ ⏳︎
         */
        public val hourglassWithFlowingSand: Emoji = Emoji('⏳')

        /**
         * [BALLOT BOX](https://codepoints.net/U+2610) ☐️ ☐︎
         */
        public val ballotBox: Emoji = Emoji('☐')

        /**
         * [BALLOT BOX WITH CHECK](https://codepoints.net/U+2611) ☑️ ☑︎
         */
        public val ballotBoxWithCheck: Emoji = Emoji('☑')

        /**
         * [BALLOT BOX WITH X](https://codepoints.net/U+2612) ☒️ ☒︎
         */
        public val ballotBoxWithX: Emoji = Emoji('☒')


        /**
         * [LINE FEED (LF)](https://codepoints.net/U+26A1) ⚡️ ⚡︎
         */
        public val highVoltageSign: Emoji = Emoji('⚡')

        /**
         * [CHECK MARK](https://codepoints.net/U+2713) ✓️ ✓︎
         */
        public val checkMark: Emoji = Emoji('✓')

        /**
         * [HEAVY CHECK MARK](https://codepoints.net/U+2714) ✔️ ✔︎
         */
        public val heavyCheckMark: Emoji = Emoji('✔')

        /**
         * [CHECK MARK](https://codepoints.net/U+2705) ✅️ ✅︎
         */
        public val checkMark_: Emoji = Emoji('✅')

        /**
         * [X MARK](https://codepoints.net/U+274E) ❎️ ❎︎
         */
        public val xMark: Emoji = Emoji('❎')

        /**
         * [BALLOT X](https://codepoints.net/U+2717) ✗️ ✗︎
         */
        public val ballotX: Emoji = Emoji('✗')

        /**
         * [HEAVY BALLOT X](https://codepoints.net/U+2718) ✘️ ✘︎
         */
        public val heavyBallotX: Emoji = Emoji('✘')

        /**
         * [CROSS MARK](https://codepoints.net/U+274C) ❌️ ❌︎
         */
        public val crossMark: Emoji = Emoji('❌')

        /**
         * [HEAVY LARGE CIRCLE](https://codepoints.net/U+2B55) ⭕️ ⭕︎
         */
        public val heavyLargeCircle: Emoji = Emoji('⭕')

        /**
         * [HEAVY ROUND-TIPPED RIGHTWARDS ARROW](https://codepoints.net/U+279C) ➜️ ➜︎
         */
        public val heavyRoundTippedRightwardsArrow: Emoji = Emoji('➜')

        /**
         * [GREEN CIRCLE](https://codepoints.net/U+1F7E2) 🟢️ 🟢︎
         */
        public val greenCircle: Emoji = Emoji("🟢")

        /**
         * [PAGE FACING UP](https://codepoints.net/U+1F4C4) 📄️ 📄︎
         */
        public val pageFacingUp: Emoji = Emoji("📄")

        /**
         * [VARIATION SELECTOR-15](https://codepoints.net/U+FE0E)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0E forces it to be rendered
         * in a textual fashion as compared to a colorful image.</cite>
         */
        public const val variationSelector15: Char = '︎'

        /**
         * [VARIATION SELECTOR-16](https://codepoints.net/U+FE0F)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0F forces it to be rendered
         * as a colorful image as compared to a monochrome text variant."</cite>
         */
        public const val variationSelector16: Char = '️'
    }

    /**
     * Interface to facilitate implementing named, enumerable Unicode code points by their names.
     *
     * @sample DivinationSymbols.Digrams
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
