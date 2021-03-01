package koodies.text

import koodies.number.mod
import koodies.text.CodePoint.CodePointRange
import koodies.text.Unicode.UnicodeBlockMeta.Companion.metaFor

object Unicode {

    /**
     * Returns the [CodePoint] with the specified index.
     */
    operator fun get(codePoint: Int): CodePoint = CodePoint(codePoint)

    /**
     * [START OF HEADING](https://codepoints.net/U+0001)
     */
    const val startOfHeading = '\u0001'

    /**
     * [ESCAPE](https://codepoints.net/U+001B)
     */
    const val escape = '\u001B'

    /**
     * [CONTROL SEQUENCE INTRODUCER](https://codepoints.net/U+009B)
     */
    const val controlSequenceIntroducer = '\u009B'

    /**
     * [NO-BREAK SPACE](https://codepoints.net/U+00A0)
     */
    const val NO_BREAK_SPACE = '\u00A0'
    const val NBSP = NO_BREAK_SPACE

    /**
     * [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D)
     */
    const val carriageReturn = "\r"

    /**
     * [LINE FEED (LF)](https://codepoints.net/U+000A)
     */
    const val lineFeed = "\n"

    /**
     * [FIGURE SPACE](https://codepoints.net/U+2007)
     */
    const val figureSpace = "\u2007"

    /**
     * [ZERO WIDTH SPACE](https://codepoints.net/U+200B)
     */
    const val zeroWidthSpace = "\u200B"

    /**
     * [LINE SEPARATOR](https://codepoints.net/U+2028)
     */
    const val lineSeparator = "\u2028"

    /**
     * [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029)
     */
    const val paragraphSeparator = "\u2029"

    /**
     * [NARROW NO-BREAK SPACE](https://codepoints.net/U+202F)
     */
    const val narrowNoBreakSpace = "\u202F"

    /**
     * [NEXT LINE (NEL)](https://codepoints.net/U+0085)
     */
    const val nextLine = "\u0085"

    /**
     * [PILCROW SIGN](https://codepoints.net/U+00B6) ¶
     */
    const val pilcrowSign = "\u00B6"

    /**
     * [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F)
     */
    const val rightToLeftMark = '\u200F'

    /**
     * [Tai Xuan Jing Symbols](https://codepoints.net/tai_xuan_jing_symbols)
     *
     * Block from `U+1D300` to `U+1D35F`. This block was introduced in Unicode version 4.0 (2003). It contains 87 codepoints.
     *
     * `𝌀` to `𝍖`
     */
    object DivinationSymbols {
        @Suppress("unused", "KDocMissingDocumentation")
        enum class Monograms(override val range: CodePointRange = CodePoint("𝌀")..CodePoint("𝌀")) : UnicodeBlock<Monograms> {
            Earth;

            override fun toString(): String = string

            companion object : UnicodeBlockMeta<Monograms> by metaFor()
        }

        @Suppress("unused", "KDocMissingDocumentation")
        enum class Digrams(override val range: CodePointRange = CodePoint("𝌁")..CodePoint("𝌅")) : UnicodeBlock<Digrams> {
            HeavenlyEarth, HumanEarth, EarthlyHeaven, EarthlyHuman, Earth;

            override fun toString(): String = string

            companion object : UnicodeBlockMeta<Digrams> by metaFor()
        }

        @Suppress("unused", "KDocMissingDocumentation")
        enum class Tetragrams(override val range: CodePointRange = CodePoint("𝌆")..CodePoint("𝍖")) : UnicodeBlock<Tetragrams> {
            Centre, FullCircle, Mired, Barrier, KeepingSmall, Contrariety, Ascent, Opposition, BranchingOut, DefectivenessOrDistortion, Divergence, Youthfulness, Increase, Penetration, Reach, Contact, HoldingBack, Waiting, Following, Advance, Release, Resistance, Ease, Joy, Contention, Endeavour, Duties, Change, Decisiveness, BoldResolution, Packing, Legion, Closeness, Kinship, Gathering, Strength, Purity, Fullness, Residence, LawOrModel, Response, GoingToMeet, Encounters, Stove, Greatness, Enlargement, Pattern, Ritual, Flight, VastnessOrWasting, Constancy, Measure, Eternity, Unity, Diminishment, ClosedMouth, Guardedness, GatheringIn, Massing, Accumulation, Embellishment, Doubt, Watch, Sinking, Inner, Departure, Darkening, Dimming, Exhaustion, Severance, Stoppage, Hardness, Completion, Closure, Failure, Aggravation, Compliance, OnTheVerge, Difficulties, Labouring, Fostering;

            override fun toString(): String = string

            companion object : UnicodeBlockMeta<Tetragrams> by metaFor()
        }
    }

    @Suppress("unused", "KDocMissingDocumentation")
    enum class BoxDrawings(override val range: CodePointRange = CodePoint("─")..CodePoint("╿")) : UnicodeBlock<BoxDrawings> {
        LightHorizontal, HeavyHorizontal, LightVertical, HeavyVertical, LightTripleDashHorizontal, HeavyTripleDashHorizontal, LightTripleDashVertical, HeavyTripleDashVertical, LightQuadrupleDashHorizontal, HeavyQuadrupleDashHorizontal, LightQuadrupleDashVertical, HeavyQuadrupleDashVertical, LightDownAndRight, DownLightAndRightHeavy, DownHeavyAndRightLight, HeavyDownAndRight, LightDownAndLeft, DownLightAndLeftHeavy, DownHeavyAndLeftLight, HeavyDownAndLeft, LightUpAndRight, UpLightAndRightHeavy, UpHeavyAndRightLight, HeavyUpAndRight, LightUpAndLeft, UpLightAndLeftHeavy, UpHeavyAndLeftLight, HeavyUpAndLeft, LightVerticalAndRight, VerticalLightAndRightHeavy, UpHeavyAndRightDownLight, DownHeavyAndRightUpLight, VerticalHeavyAndRightLight, DownLightAndRightUpHeavy, UpLightAndRightDownHeavy, HeavyVerticalAndRight, LightVerticalAndLeft, VerticalLightAndLeftHeavy, UpHeavyAndLeftDownLight, DownHeavyAndLeftUpLight, VerticalHeavyAndLeftLight, DownLightAndLeftUpHeavy, UpLightAndLeftDownHeavy, HeavyVerticalAndLeft, LightDownAndHorizontal, LeftHeavyAndRightDownLight, RightHeavyAndLeftDownLight, DownLightAndHorizontalHeavy, DownHeavyAndHorizontalLight, RightLightAndLeftDownHeavy, LeftLightAndRightDownHeavy, HeavyDownAndHorizontal, LightUpAndHorizontal, LeftHeavyAndRightUpLight, RightHeavyAndLeftUpLight, UpLightAndHorizontalHeavy, UpHeavyAndHorizontalLight, RightLightAndLeftUpHeavy, LeftLightAndRightUpHeavy, HeavyUpAndHorizontal, LightVerticalAndHorizontal, LeftHeavyAndRightVerticalLight, RightHeavyAndLeftVerticalLight, VerticalLightAndHorizontalHeavy, UpHeavyAndDownHorizontalLight, DownHeavyAndUpHorizontalLight, VerticalHeavyAndHorizontalLight, LeftUpHeavyAndRightDownLight, RightUpHeavyAndLeftDownLight, LeftDownHeavyAndRightUpLight, RightDownHeavyAndLeftUpLight, DownLightAndUpHorizontalHeavy, UpLightAndDownHorizontalHeavy, RightLightAndLeftVerticalHeavy, LeftLightAndRightVerticalHeavy, HeavyVerticalAndHorizontal, LightDoubleDashHorizontal, HeavyDoubleDashHorizontal, LightDoubleDashVertical, HeavyDoubleDashVertical, DoubleHorizontal, DoubleVertical, DownSingleAndRightDouble, DownDoubleAndRightSingle, DoubleDownAndRight, DownSingleAndLeftDouble, DownDoubleAndLeftSingle, DoubleDownAndLeft, UpSingleAndRightDouble, UpDoubleAndRightSingle, DoubleUpAndRight, UpSingleAndLeftDouble, UpDoubleAndLeftSingle, DoubleUpAndLeft, VerticalSingleAndRightDouble, VerticalDoubleAndRightSingle, DoubleVerticalAndRight, VerticalSingleAndLeftDouble, VerticalDoubleAndLeftSingle, DoubleVerticalAndLeft, DownSingleAndHorizontalDouble, DownDoubleAndHorizontalSingle, DoubleDownAndHorizontal, UpSingleAndHorizontalDouble, UpDoubleAndHorizontalSingle, DoubleUpAndHorizontal, VerticalSingleAndHorizontalDouble, VerticalDoubleAndHorizontalSingle, DoubleVerticalAndHorizontal, LightArcDownAndRight, LightArcDownAndLeft, LightArcUpAndLeft, LightArcUpAndRight, LightDiagonalUpperRightToLowerLeft, LightDiagonalUpperLeftToLowerRight, LightDiagonalCross, LightLeft, LightUp, LightRight, LightDown, HeavyLeft, HeavyUp, HeavyRight, HeavyDown, LightLeftAndHeavyRight, LightUpAndHeavyDown, HeavyLeftAndLightRight, HeavyUpAndLightDown;

        override fun toString(): String = string

        companion object : UnicodeBlockMeta<BoxDrawings> by metaFor()
    }

    val boxDrawings = ('\u2500'..'\u257F').toList()

    @Suppress("unused", "KDocMissingDocumentation")
    enum class CombiningDiacriticalMarks(override val range: CodePointRange = CodePoint("̀")..CodePoint("ͯ")) : UnicodeBlock<CombiningDiacriticalMarks> {
        CombiningGraveAccent, CombiningAcuteAccent, CombiningCircumflexAccent, CombiningTilde, CombiningMacron, CombiningOverline, CombiningBreve, CombiningDotAbove, CombiningDiaeresis, CombiningHookAbove, CombiningRingAbove, CombiningDoubleAcuteAccent, CombiningCaron, CombiningVerticalLineAbove, CombiningDoubleVerticalLineAbove, CombiningDoubleGraveAccent, CombiningCandrabindu, CombiningInvertedBreve, CombiningTurnedCommaAbove, CombiningCommaAbove, CombiningReversedCommaAbove, CombiningCommaAboveRight, CombiningGraveAccentBelow, CombiningAcuteAccentBelow, CombiningLeftTackBelow, CombiningRightTackBelow, CombiningLeftAngleAbove, CombiningHorn, CombiningLeftHalfRingBelow, CombiningUpTackBelow, CombiningDownTackBelow, CombiningPlusSignBelow, CombiningMinusSignBelow, CombiningPalatalizedHookBelow, CombiningRetroflexHookBelow, CombiningDotBelow, CombiningDiaeresisBelow, CombiningRingBelow, CombiningCommaBelow, CombiningCedilla, CombiningOgonek, CombiningVerticalLineBelow, CombiningBridgeBelow, CombiningInvertedDoubleArchBelow, CombiningCaronBelow, CombiningCircumflexAccentBelow, CombiningBreveBelow, CombiningInvertedBreveBelow, CombiningTildeBelow, CombiningMacronBelow, CombiningLowLine, CombiningDoubleLowLine, CombiningTildeOverlay, CombiningShortStrokeOverlay, CombiningLongStrokeOverlay, CombiningShortSolidusOverlay, CombiningLongSolidusOverlay, CombiningRightHalfRingBelow, CombiningInvertedBridgeBelow, CombiningSquareBelow, CombiningSeagullBelow, CombiningXAbove, CombiningVerticalTilde, CombiningDoubleOverline, CombiningGraveToneMark, CombiningAcuteToneMark, CombiningGreekPerispomeni, CombiningGreekKoronis, CombiningGreekDialytikaTonos, CombiningGreekYpogegrammeni, CombiningBridgeAbove, CombiningEqualsSignBelow, CombiningDoubleVerticalLineBelow, CombiningLeftAngleBelow, CombiningNotTildeAbove, CombiningHomotheticAbove, CombiningAlmostEqualToAbove, CombiningLeftRightArrowBelow, CombiningUpwardsArrowBelow, CombiningGraphemeJoiner, CombiningRightArrowheadAbove, CombiningLeftHalfRingAbove, CombiningFermata, CombiningXBelow, CombiningLeftArrowheadBelow, CombiningRightArrowheadBelow, CombiningRightArrowheadAndUpArrowheadBelow, CombiningRightHalfRingAbove, CombiningDotAboveRight, CombiningAsteriskBelow, CombiningDoubleRingBelow, CombiningZigzagAbove, CombiningDoubleBreveBelow, CombiningDoubleBreve, CombiningDoubleMacron, CombiningDoubleMacronBelow, CombiningDoubleTilde, CombiningDoubleInvertedBreve, CombiningDoubleRightwardsArrowBelow, CombiningLatinSmallLetterA, CombiningLatinSmallLetterE, CombiningLatinSmallLetterI, CombiningLatinSmallLetterO, CombiningLatinSmallLetterU, CombiningLatinSmallLetterC, CombiningLatinSmallLetterD, CombiningLatinSmallLetterH, CombiningLatinSmallLetterM, CombiningLatinSmallLetterR, CombiningLatinSmallLetterT, CombiningLatinSmallLetterV, CombiningLatinSmallLetterX;

        override fun toString(): String = string

        companion object : UnicodeBlockMeta<CombiningDiacriticalMarks> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation")
    enum class CombiningDiacriticalMarksSupplementBlock(override val range: CodePointRange = CodePoint("᷀")..CodePoint("᷿")) :
        UnicodeBlock<CombiningDiacriticalMarksSupplementBlock> {
        CombiningDottedGraveAccent, CombiningDottedAcuteAccent, CombiningSnakeBelow, CombiningSuspensionMark, CombiningMacronAcute, CombiningGraveMacron, CombiningMacronGrave, CombiningAcuteMacron, CombiningGraveAcuteGrave, CombiningAcuteGraveAcute, CombiningLatinSmallLetterRBelow, CombiningBreveMacron, CombiningMacronBreve, CombiningDoubleCircumflexAbove, CombiningOgonekAbove, CombiningZigzagBelow, CombiningIsBelow, CombiningUrAbove, CombiningUsAbove, CombiningLatinSmallLetterFlattenedOpenAAbove, CombiningLatinSmallLetterAe, CombiningLatinSmallLetterAo, CombiningLatinSmallLetterAv, CombiningLatinSmallLetterCCedilla, CombiningLatinSmallLetterInsularD, CombiningLatinSmallLetterEth, CombiningLatinSmallLetterG, CombiningLatinLetterSmallCapitalG, CombiningLatinSmallLetterK, CombiningLatinSmallLetterL, CombiningLatinLetterSmallCapitalL, CombiningLatinLetterSmallCapitalM, CombiningLatinSmallLetterN, CombiningLatinLetterSmallCapitalN, CombiningLatinLetterSmallCapitalR, CombiningLatinSmallLetterRRotunda, CombiningLatinSmallLetterS, CombiningLatinSmallLetterLongS, CombiningLatinSmallLetterZ, CombiningDoubleInvertedBreveBelow, CombiningAlmostEqualToBelow, CombiningLeftArrowheadAbove, CombiningRightArrowheadAndDownArrowheadBelow;

        override fun toString(): String = string

        companion object : UnicodeBlockMeta<CombiningDiacriticalMarksSupplementBlock> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation")
    enum class CombiningDiacriticalMarksForSymbolsBlock(override val range: CodePointRange = CodePoint("⃐")..CodePoint("⃰")) :
        UnicodeBlock<CombiningDiacriticalMarksForSymbolsBlock> {
        CombiningLeftHarpoonAbove, CombiningRightHarpoonAbove, CombiningLongVerticalLineOverlay, CombiningShortVerticalLineOverlay, CombiningAnticlockwiseArrowAbove, CombiningClockwiseArrowAbove, CombiningLeftArrowAbove, CombiningRightArrowAbove, CombiningRingOverlay, CombiningClockwiseRingOverlay, CombiningAnticlockwiseRingOverlay, CombiningThreeDotsAbove, CombiningFourDotsAbove, CombiningEnclosingCircle, CombiningEnclosingSquare, CombiningEnclosingDiamond, CombiningEnclosingCircleBackslash, CombiningLeftRightArrowAbove, CombiningEnclosingScreen, CombiningEnclosingKeycap, CombiningEnclosingUpwardPointingTriangle, CombiningReverseSolidusOverlay, CombiningDoubleVerticalStrokeOverlay, CombiningAnnuitySymbol, CombiningTripleUnderdot, CombiningWideBridgeAbove, CombiningLeftwardsArrowOverlay, CombiningLongDoubleSolidusOverlay, CombiningRightwardsHarpoonWithBarbDownwards, CombiningLeftwardsHarpoonWithBarbDownwards, CombiningLeftArrowBelow, CombiningRightArrowBelow, CombiningAsteriskAbove;

        override fun toString(): String = string

        companion object : UnicodeBlockMeta<CombiningDiacriticalMarksForSymbolsBlock> by metaFor()
    }

    @Suppress("unused", "KDocMissingDocumentation")
    enum class CombiningHalfMarksBlock(override val range: CodePointRange = CodePoint("︠")..CodePoint("︦")) : UnicodeBlock<CombiningHalfMarksBlock> {
        CombiningLigatureLeftHalf, CombiningLigatureRightHalf, CombiningDoubleTildeLeftHalf, CombiningDoubleTildeRightHalf, CombiningMacronLeftHalf, CombiningMacronRightHalf, CombiningConjoiningMacron;

        override fun toString(): String = string

        companion object : UnicodeBlockMeta<CombiningHalfMarksBlock> by metaFor()
    }

    @Suppress("SpellCheckingInspection")
    val whitespaces: List<Char> = listOf(
        '\u0020', // SPACE: Depends on font, typically 1/4 em, often adjusted
        '\u00A0', // NO-BREAK SPACE: As a space, but often not adjusted
        '\u1680', // OGHAM SPACE MARK: Unspecified; usually not really a space but a dash
        '\u180E', // MONGOLIAN VOWEL SEPARATOR: 0
        '\u2000', // EN QUAD: 1 en (= 1/2 em)
        '\u2001', // EM QUAD: 1 em (nominally, the height of the font)
        '\u2002', // EN SPACE (nut): 1 en (= 1/2 em)
        '\u2003', // EM SPACE (mutton): 1 em
        '\u2004', // THREE-PER-EM SPACE (thick space): 1/3 em
        '\u2005', // FOUR-PER-EM SPACE (mid space): 1/4 em
        '\u2006', // SIX-PER-EM SPACE: 1/6 em
        '\u2007', // FIGURE SPACE	fo: “Tabular width”, the width of digits
        '\u2008', // PUNCTUATION SPACE: The width of a period “.”
        '\u2009', // THIN SPACE: 1/5 em (or sometimes 1/6 em)
        '\u200A', // HAIR SPACE: Narrower than THIN SPACE
        '\u200B', // ZERO WIDTH SPACE: 0
        '\u202F', // NARROW NO-BREAK SPACE	fo: Narrower than NO-BREAK SPACE (or SPACE), “typically the width of a thin space or a mid space”
        '\u205F', // MEDIUM MATHEMATICAL SPACE: 4/18 em
        '\u3000', // IDEOGRAPHIC SPACE: The width of ideographic (CJK) characters.
        '\uFEFF', // ZERO WIDTH NO-BREAK SPACE: 0
    )

    val controlCharacters: Map<Char, Char> = mapOf(
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
    val Char.replacementSymbol: Char? get() = controlCharacters[this]

    /**
     * Contains this code point's replacement symbol if any.
     *
     * This only applies to the so called [controlCharacters].
     */
    val CodePoint.replacementSymbol: Char? get() = char?.replacementSymbol


    /**
     * [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) �
     */
    const val replacementCharacter = '\uFFFD'


    /**
     * [GREEK LETTER KOPPA](https://codepoints.net/U+03DE) Ϟ
     */
    val greekLetterKoppa = 'Ϟ'

    /**
     * [GREEK SMALL LETTER KOPPA](https://codepoints.net/U+03DF) ϟ
     */
    val greekSmallLetterKoppa = 'ϟ'


    /**
     * Unicode emojis as specified by the [Unicode® Technical Standard #51](https://unicode.org/reports/tr51/) 🤓
     */
    object Emojis {

        class Emoji(private val emoji: String) :
            CharSequence by emoji.removeSuffix(variationSelector15.toString()).removeSuffix(variationSelector16.toString()) {
            constructor(emoji: Char) : this(emoji.toString())

            val textVariant get() = "$emoji$variationSelector15"
            val emojiVariant get() = "$emoji$variationSelector16"

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
        object FullHoursDictionary {
            operator fun get(key: Int): Emoji = fullHourClocks[key.mod(fullHourClocks.size)] ?: error("Missing clock in dictionary")
        }

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding next half hour, e.g. `3` will return a "3:30 o'clock"/🕞 emoji.
         *
         * This dictionary applies the [rem] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3:30 o'clock"/🕞 emoji)
         * will also return the corresponding next half hour.
         */
        object HalfHoursDictionary {
            operator fun get(key: Int): Emoji = halfHourClocks[key.mod(halfHourClocks.size)] ?: error("Missing clock in dictionary")
        }

        /**
         * [BALLOT BOX](https://codepoints.net/U+2610) ☐️ ☐︎
         */
        val ballotBox = Emoji('☐')

        /**
         * [BALLOT BOX WITH CHECK](https://codepoints.net/U+2611) ☑️ ☑︎
         */
        val ballotBoxWithCheck = Emoji('☑')

        /**
         * [BALLOT BOX WITH X](https://codepoints.net/U+2612) ☒️ ☒︎
         */
        val ballotBoxWithX = Emoji('☒')


        /**
         * [LINE FEED (LF)](https://codepoints.net/U+26A1) ⚡️ ⚡︎
         */
        val highVoltageSign = Emoji('⚡')

        /**
         * [CHECK MARK](https://codepoints.net/U+2713) ✓️ ✓︎
         */
        val checkMark = Emoji('✓')

        /**
         * [HEAVY CHECK MARK](https://codepoints.net/U+2714) ✔️ ✔︎
         */
        val heavyCheckMark = Emoji('✔')

        /**
         * [CHECK MARK](https://codepoints.net/U+2705) ✅️ ✅︎
         */
        val checkMark_ = Emoji('✅')

        /**
         * [X MARK](https://codepoints.net/U+274E) ❎️ ❎︎
         */
        val xMark = Emoji('❎')

        /**
         * [BALLOT X](https://codepoints.net/U+2717) ✗️ ✗︎
         */
        val ballotX = Emoji('✗')

        /**
         * [HEAVY BALLOT X](https://codepoints.net/U+2718) ✘️ ✘︎
         */
        val heavyBallotX = Emoji('✘')

        /**
         * [CROSS MARK](https://codepoints.net/U+274C) ❌️ ❌︎
         */
        val crossMark = Emoji('❌')

        /**
         * [HEAVY LARGE CIRCLE](https://codepoints.net/U+2B55) ⭕️ ⭕︎
         */
        val heavyLargeCircle = Emoji('⭕')

        /**
         * [HEAVY ROUND-TIPPED RIGHTWARDS ARROW](https://codepoints.net/U+279C) ➜️ ➜︎
         */
        val heavyRoundTippedRightwardsArrow = Emoji('➜')

        /**
         * [GREEN CIRCLE](https://codepoints.net/U+1F7E2) 🟢️ 🟢︎
         */
        val greenCircle = Emoji("🟢")

        /**
         * [PAGE FACING UP](https://codepoints.net/U+1F4C4) 📄️ 📄︎
         */
        val pageFacingUp = Emoji("📄")

        /**
         * [VARIATION SELECTOR-15](https://codepoints.net/U+FE0E)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0E forces it to be rendered
         * in a textual fashion as compared to a colorful image.</cite>
         */
        const val variationSelector15: Char = '︎'

        /**
         * [VARIATION SELECTOR-16](https://codepoints.net/U+FE0F)
         *
         * <cite>This codepoint may change the appearance of the preceding character.
         * If that is a symbol, dingbat or emoji, U+FE0F forces it to be rendered
         * as a colorful image as compared to a monochrome text variant."</cite>
         */
        const val variationSelector16: Char = '️'
    }

    /**
     * Interface to facilitate implementing named, enumerable Unicode code points by their names.
     *
     * @sample DivinationSymbols.Digrams
     */
    interface UnicodeBlock<T : Enum<T>> : ClosedRange<CodePoint> {
        override val start: CodePoint get() = range.start
        override val endInclusive: CodePoint get() = range.endInclusive
        override fun contains(value: CodePoint): Boolean = range.contains(value)
        override fun isEmpty(): Boolean = range.isEmpty()

        val range: CodePointRange
        val ordinal: Int
        val string: String get() = (range.first + ordinal).string
    }

    interface UnicodeBlockMeta<T> where T : UnicodeBlock<T>, T : Enum<T> {
        val valueCount: Int
        val unicodeBlock: UnicodeBlock<T>
        val codePointCount: Int
        val isValid: Boolean
        val name: String

        companion object {
            inline fun <reified T> metaFor(): UnicodeBlockMeta<T> where T : Enum<T>, T : UnicodeBlock<T> =
                SimpleUnicodeBlockMeta(enumValues())
        }

        class SimpleUnicodeBlockMeta<T>(val values: Array<T>) : UnicodeBlockMeta<T> where T : Enum<T>, T : UnicodeBlock<T> {
            override val valueCount: Int by lazy { values.size }
            override val unicodeBlock: UnicodeBlock<T> by lazy { values.first() }
            override val codePointCount: Int by lazy { unicodeBlock.range.last - unicodeBlock.range.first + 1 }
            override val isValid: Boolean by lazy { valueCount == codePointCount }
            override val name: String by lazy { toString().split("$").last { !it.contains("Companion") } }
        }
    }
}
