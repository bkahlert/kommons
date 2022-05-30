package com.bkahlert.kommons.kaomoji

import com.bkahlert.kommons.takeIfNotBlank
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.CodePoint
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.asCodePointSequence
import com.bkahlert.kommons.text.charCount
import com.bkahlert.kommons.text.codePointCount
import com.bkahlert.kommons.text.columns
import com.bkahlert.kommons.text.maxLength
import com.bkahlert.kommons.text.padEnd
import kotlin.random.Random

/**
 * A [kaomoji](https://en.wikipedia.org/wiki/Emoticon#Japanese_style).
 */
public data class Kaomoji(
    private val kaomojiString: String,
    private val leftArmRange: IntRange,
    private val leftEyeRange: IntRange,
    private val mouthRange: IntRange,
    private val rightEyeRange: IntRange,
    private val rightArmRange: IntRange,
    private val accessoryRange: IntRange,
) : CharSequence {

    private constructor(
        leftArm: Pair<CharSequence, Int>,
        leftEye: Pair<CharSequence, Int>,
        mouth: Pair<CharSequence, Int>,
        rightEye: Pair<CharSequence, Int>,
        rightArm: Pair<CharSequence, Int>,
        accessory: Pair<CharSequence, Int>,
    ) : this(
        "${leftArm.first}${leftEye.first}${mouth.first}${rightEye.first}${rightArm.first}${accessory.first}",
        0 until leftArm.second,
        leftArm.second until leftArm.second + leftEye.second,
        leftArm.second + leftEye.second until leftArm.second + leftEye.second + mouth.second,
        leftArm.second + leftEye.second + mouth.second until leftArm.second + leftEye.second + mouth.second + rightEye.second,
        leftArm.second + leftEye.second + mouth.second + rightEye.second until leftArm.second + leftEye.second + mouth.second + rightEye.second + rightArm.second,
        leftArm.second + leftEye.second + mouth.second + rightEye.second + rightArm.second until leftArm.second + leftEye.second + mouth.second + rightEye.second + rightArm.second + accessory.second
    )

    public constructor(
        leftArm: CharSequence,
        leftEye: CharSequence,
        mouth: CharSequence,
        rightEye: CharSequence,
        rightArm: CharSequence,
        accessory: CharSequence = "",
    ) : this(
        leftArm to leftArm.codePointCount,
        leftEye to leftEye.codePointCount,
        mouth to mouth.codePointCount,
        rightEye to rightEye.codePointCount,
        rightArm to rightArm.codePointCount,
        accessory to accessory.codePointCount,
    )

    public val leftArm: CharSequence get() = leftArmRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    public val leftEye: CharSequence get() = leftEyeRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    public val mouth: CharSequence get() = mouthRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    public val rightEye: CharSequence get() = rightEyeRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    public val rightArm: CharSequence get() = rightArmRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    public val accessory: CharSequence get() = accessoryRange.takeUnless { it.isEmpty() }?.let { kaomojiString.subSequence(it) } ?: ""
    private val string = "$leftArm$leftEye$mouth$rightEye$rightArm$accessory"

    public fun random(): Kaomoji = Generator.random(
        leftArm.takeIf { Random.nextDouble() >= 0.8 },
        leftEye.takeIf { Random.nextDouble() >= 0.8 },
        mouth.takeIf { Random.nextDouble() >= 0.8 },
        rightEye.takeIf { Random.nextDouble() >= 0.8 },
        rightArm.takeIf { Random.nextDouble() >= 0.8 },
        accessory,
    )

    /**
     * Returns a fishing [Kaomoji] of the form:
     *
     * ```
     * （♯▼皿▼）o/￣￣￣<゜)))彡
     * ```
     */
    public fun fishing(fish: Kaomoji = (Fish + Whales).random()): Kaomoji {
        val fishingRod = "/￣￣￣"
        val fishingArm = ")o"
        return Kaomoji(leftArm, leftEye, mouth, rightEye, fishingArm, "$fishingRod$fish")
    }

    private val blank: String by lazy { " ".repeat(toString().columns) }

    /**
     * Returns a thinking [Kaomoji] of the form:
     *
     * ```
     *        ̣ ˱ ❨ ( something )
     * (^～^)
     * ```
     *
     * ```
     *             ⎛ something ⎞
     *             ⎜           ⎟
     *        ̣ ˱ ❨ ⎝ more      ⎠
     * ・㉨・
     * ```
     */
    public fun thinking(text: String? = null): String {
        val subject = text?.takeIfNotBlank()
        val lines = subject?.toAnsiString().lines()
        return when (lines.size) {
            0 -> {
                """
                    $blank  ̣ ˱ ❨ ( … )
                    $this
                """.trimIndent()
            }
            1 -> {
                """
                    $blank  ̣ ˱ ❨ ( $subject )
                    $this
                """.trimIndent()
            }
            2 -> {
                val max = lines.maxLength()
                """
                    $blank       ⎛ ${lines.first().padEnd(max)} ⎞
                    $blank  ̣ ˱ ❨ ⎝ ${lines.last().padEnd(max)} ⎠
                    $this
                """.trimIndent()
            }
            else -> {
                val max = lines.maxLength()
                "$blank       ⎛ ${lines.first().padEnd(max)} ⎞$LF" +
                    lines.drop(1).dropLast(1).joinToString("") { "$blank       ⎜ ${it.padEnd(max)} ⎟$LF" } +
                    "$blank  ̣ ˱ ❨ ⎝ ${lines.last().padEnd(max)} ⎠$LF" +
                    this
            }
        }
    }

    override val length: Int get() = toString().length
    override fun get(index: Int): Char = toString().get(index)
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
    override fun toString(): String = string
    override fun hashCode(): Int = string.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Kaomoji

        if (string != other.string) return false

        return true
    }


    @Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")
    public companion object {

        /**
         * An empty kaomoji...
         */
        public val EMPTY: Kaomoji = Kaomoji("", IntRange.EMPTY, IntRange.EMPTY, IntRange.EMPTY, IntRange.EMPTY, IntRange.EMPTY, IntRange.EMPTY)

        /**
         * Parses a [Kaomoji] from the given [kaomojiString].
         */
        public fun parse(kaomojiString: String): Kaomoji? {
            val parts = kaomojiString.asCodePointSequence().toMutableList()
            val ranges = parts.runningFold(IntRange(0, -1)) { previousRange: IntRange, codePoint: CodePoint ->
                (previousRange.last + 1)..(previousRange.last + codePoint.charCount)
            }.drop(1)

            return when (parts.size) {
                0, 1, 2 -> null
                3 -> Kaomoji(
                    kaomojiString,
                    leftArmRange = IntRange.EMPTY,
                    leftEyeRange = ranges[0],
                    mouthRange = ranges[1],
                    rightEyeRange = ranges[2],
                    rightArmRange = IntRange.EMPTY,
                    accessoryRange = IntRange.EMPTY,
                )
                4 -> Kaomoji(
                    kaomojiString,
                    leftArmRange = IntRange.EMPTY,
                    leftEyeRange = ranges[0],
                    mouthRange = ranges[1],
                    rightEyeRange = ranges[2],
                    rightArmRange = ranges[3],
                    accessoryRange = IntRange.EMPTY,
                )
                5 -> Kaomoji(
                    kaomojiString,
                    leftArmRange = ranges[0],
                    leftEyeRange = ranges[1],
                    mouthRange = ranges[2],
                    rightEyeRange = ranges[3],
                    rightArmRange = ranges[4],
                    accessoryRange = IntRange.EMPTY,
                )
                else -> Kaomoji(
                    kaomojiString,
                    leftArmRange = ranges[0],
                    leftEyeRange = ranges[1],
                    mouthRange = ranges[2],
                    rightEyeRange = ranges[3],
                    rightArmRange = ranges[4],
                    accessoryRange = ranges[5].first until kaomojiString.length
                )
            }
        }

        /**
         * Returns a randomly created [Kaomoji].
         */
        public fun random(): Kaomoji = Generator.random()

        /**
         * Returns a random [Kaomoji] from the kaomoji.
         */
        public fun random(kaomoji: Kaomoji, vararg kaomojis: Kaomoji): Kaomoji = listOf(kaomoji, *kaomojis).random()

        /**
         * Returns a random [Kaomoji] from the provided categories.
         */
        public fun random(category: Category, vararg categories: Category): Kaomoji = listOf(category, *categories).random().random()

        /**
         * Selection of angry [Kaomoji]
         */
        public val Angry: com.bkahlert.kommons.kaomoji.categories.Angry = com.bkahlert.kommons.kaomoji.categories.Angry

        /**
         * Selection of baby [Kaomoji]
         */
        public val Babies: com.bkahlert.kommons.kaomoji.categories.Babies = com.bkahlert.kommons.kaomoji.categories.Babies

        /**
         * Selection of bad mood [Kaomoji]
         */
        public val BadMood: com.bkahlert.kommons.kaomoji.categories.BadMood = com.bkahlert.kommons.kaomoji.categories.BadMood

        /**
         * Selection of bear [Kaomoji]
         */
        public val Bears: com.bkahlert.kommons.kaomoji.categories.Bears = com.bkahlert.kommons.kaomoji.categories.Bears

        /**
         * Selection of begging [Kaomoji]
         */
        public val Begging: com.bkahlert.kommons.kaomoji.categories.Begging = com.bkahlert.kommons.kaomoji.categories.Begging

        /**
         * Selection of blushing [Kaomoji]
         */
        public val Blushing: com.bkahlert.kommons.kaomoji.categories.Blushing = com.bkahlert.kommons.kaomoji.categories.Blushing

        /**
         * Selection of cat [Kaomoji]
         */
        public val Cats: com.bkahlert.kommons.kaomoji.categories.Cats = com.bkahlert.kommons.kaomoji.categories.Cats

        /**
         * Selection of celebrity [Kaomoji]
         */
        public val Celebrities: com.bkahlert.kommons.kaomoji.categories.Celebrities = com.bkahlert.kommons.kaomoji.categories.Celebrities

        /**
         * Selection of chasing [Kaomoji]
         */
        public val Chasing: com.bkahlert.kommons.kaomoji.categories.Chasing = com.bkahlert.kommons.kaomoji.categories.Chasing

        /**
         * Selection of confused [Kaomoji]
         */
        public val Confused: com.bkahlert.kommons.kaomoji.categories.Confused = com.bkahlert.kommons.kaomoji.categories.Confused

        /**
         * Selection of crying [Kaomoji]
         */
        public val Crying: com.bkahlert.kommons.kaomoji.categories.Crying = com.bkahlert.kommons.kaomoji.categories.Crying

        /**
         * Selection of cute [Kaomoji]
         */
        public val Cute: com.bkahlert.kommons.kaomoji.categories.Cute = com.bkahlert.kommons.kaomoji.categories.Cute

        /**
         * Selection of dancing [Kaomoji]
         */
        public val Dancing: com.bkahlert.kommons.kaomoji.categories.Dancing = com.bkahlert.kommons.kaomoji.categories.Dancing

        /**
         * Selection of depressed [Kaomoji]
         */
        public val Depressed: com.bkahlert.kommons.kaomoji.categories.Depressed = com.bkahlert.kommons.kaomoji.categories.Depressed

        /**
         * Selection of devil [Kaomoji]
         */
        public val Devils: com.bkahlert.kommons.kaomoji.categories.Devil = com.bkahlert.kommons.kaomoji.categories.Devil

        /**
         * Selection of disappointed [Kaomoji]
         */
        public val Disappointed: com.bkahlert.kommons.kaomoji.categories.Disappointed = com.bkahlert.kommons.kaomoji.categories.Disappointed

        /**
         * Selection of dog [Kaomoji]
         */
        public val Dog: com.bkahlert.kommons.kaomoji.categories.Dog = com.bkahlert.kommons.kaomoji.categories.Dog

        /**
         * Selection of drooling [Kaomoji]
         */
        public val Drooling: com.bkahlert.kommons.kaomoji.categories.Drooling = com.bkahlert.kommons.kaomoji.categories.Drooling

        /**
         * Selection of eating [Kaomoji]
         */
        public val Eating: com.bkahlert.kommons.kaomoji.categories.Eating = com.bkahlert.kommons.kaomoji.categories.Eating

        /**
         * Selection of evil [Kaomoji]
         */
        public val Evil: com.bkahlert.kommons.kaomoji.categories.Evil = com.bkahlert.kommons.kaomoji.categories.Evil

        /**
         * Selection of excited [Kaomoji]
         */
        public val Excited: com.bkahlert.kommons.kaomoji.categories.Excited = com.bkahlert.kommons.kaomoji.categories.Excited

        /**
         * Selection of eyes [Kaomoji]
         */
        public val Eyes: com.bkahlert.kommons.kaomoji.categories.Eyes = com.bkahlert.kommons.kaomoji.categories.Eyes

        /**
         * Selection of falling down [Kaomoji]
         */
        public val FallingDown: com.bkahlert.kommons.kaomoji.categories.FallingDown = com.bkahlert.kommons.kaomoji.categories.FallingDown

        /**
         * Selection of feces-related [Kaomoji]
         */
        public val Feces: com.bkahlert.kommons.kaomoji.categories.Feces = com.bkahlert.kommons.kaomoji.categories.Feces

        /**
         * Selection of feminine [Kaomoji]
         */
        public val Feminine: com.bkahlert.kommons.kaomoji.categories.Feminine = com.bkahlert.kommons.kaomoji.categories.Feminine

        /**
         * Selection of fish [Kaomoji]
         */
        public val Fish: com.bkahlert.kommons.kaomoji.categories.Fish = com.bkahlert.kommons.kaomoji.categories.Fish

        /**
         * Selection of fishing [Kaomoji]
         */
        public val Fishing: com.bkahlert.kommons.kaomoji.categories.Fishing = com.bkahlert.kommons.kaomoji.categories.Fishing

        /**
         * Selection of flower-related [Kaomoji]
         */
        public val Flower: com.bkahlert.kommons.kaomoji.categories.Flower = com.bkahlert.kommons.kaomoji.categories.Flower

        /**
         * Selection of funny [Kaomoji]
         */
        public val Funny: com.bkahlert.kommons.kaomoji.categories.Funny = com.bkahlert.kommons.kaomoji.categories.Funny

        /**
         * Selection of geek [Kaomoji]
         */
        public val Geek: com.bkahlert.kommons.kaomoji.categories.Geek = com.bkahlert.kommons.kaomoji.categories.Geek

        /**
         * Selection of [Kaomoji] with glasses.
         */
        public val Glasses: com.bkahlert.kommons.kaomoji.categories.Glasses = com.bkahlert.kommons.kaomoji.categories.Glasses

        /**
         * Selection of greeting [Kaomoji]
         */
        public val Greeting: com.bkahlert.kommons.kaomoji.categories.Greeting = com.bkahlert.kommons.kaomoji.categories.Greeting

        /**
         * Selection of grin [Kaomoji]
         */
        public val Grinning: com.bkahlert.kommons.kaomoji.categories.Grinning = com.bkahlert.kommons.kaomoji.categories.Grinning

        /**
         * Selection of gross [Kaomoji]
         */
        public val Gross: com.bkahlert.kommons.kaomoji.categories.Gross = com.bkahlert.kommons.kaomoji.categories.Gross

        /**
         * Selection of happy [Kaomoji]
         */
        public val Happy: com.bkahlert.kommons.kaomoji.categories.Happy = com.bkahlert.kommons.kaomoji.categories.Happy

        /**
         * Selection of helpless [Kaomoji]
         */
        public val Helpless: com.bkahlert.kommons.kaomoji.categories.Helpless = com.bkahlert.kommons.kaomoji.categories.Helpless

        /**
         * Selection of hero [Kaomoji]
         */
        public val Heroes: com.bkahlert.kommons.kaomoji.categories.Heroes = com.bkahlert.kommons.kaomoji.categories.Heroes

        /**
         * Selection of hide [Kaomoji]
         */
        public val Hide: com.bkahlert.kommons.kaomoji.categories.Hide = com.bkahlert.kommons.kaomoji.categories.Hide

        /**
         * Selection of hugging [Kaomoji]
         */
        public val Hugging: com.bkahlert.kommons.kaomoji.categories.Hugging = com.bkahlert.kommons.kaomoji.categories.Hugging

        /**
         * Selection of kissing [Kaomoji]
         */
        public val Kissing: com.bkahlert.kommons.kaomoji.categories.Kissing = com.bkahlert.kommons.kaomoji.categories.Kissing

        /**
         * Selection of laughing [Kaomoji]
         */
        public val Laughing: com.bkahlert.kommons.kaomoji.categories.Laughing = com.bkahlert.kommons.kaomoji.categories.Laughing

        /**
         * Selection of lenny face [Kaomoji]
         */
        public val LennyFace: com.bkahlert.kommons.kaomoji.categories.LennyFace = com.bkahlert.kommons.kaomoji.categories.LennyFace

        /**
         * Selection of love-related [Kaomoji]
         */
        public val Love: com.bkahlert.kommons.kaomoji.categories.Love = com.bkahlert.kommons.kaomoji.categories.Love

        /**
         * Selection of make up my mind [Kaomoji]
         */
        public val MakeUpMyMind: com.bkahlert.kommons.kaomoji.categories.MakeUpMyMind = com.bkahlert.kommons.kaomoji.categories.MakeUpMyMind

        /**
         * Selection of middle finger [Kaomoji]
         */
        public val MiddleFinger: com.bkahlert.kommons.kaomoji.categories.MiddleFinger = com.bkahlert.kommons.kaomoji.categories.MiddleFinger

        /**
         * Selection of money [Kaomoji]
         */
        public val Money: com.bkahlert.kommons.kaomoji.categories.Money = com.bkahlert.kommons.kaomoji.categories.Money

        /**
         * Selection of monkey [Kaomoji]
         */
        public val Monkey: com.bkahlert.kommons.kaomoji.categories.Monkey = com.bkahlert.kommons.kaomoji.categories.Monkey

        /**
         * Selection of musical [Kaomoji]
         */
        public val Musical: com.bkahlert.kommons.kaomoji.categories.Musical = com.bkahlert.kommons.kaomoji.categories.Musical

        /**
         * Selection of nervious [Kaomoji]
         */
        public val Nervious: com.bkahlert.kommons.kaomoji.categories.Nervious = com.bkahlert.kommons.kaomoji.categories.Nervious

        /**
         * Selection of peace sign [Kaomoji]
         */
        public val PeaceSign: com.bkahlert.kommons.kaomoji.categories.PeaceSign = com.bkahlert.kommons.kaomoji.categories.PeaceSign

        /**
         * Selection of pointing [Kaomoji]
         */
        public val Pointing: com.bkahlert.kommons.kaomoji.categories.Pointing = com.bkahlert.kommons.kaomoji.categories.Pointing

        /**
         * Selection of proud [Kaomoji]
         */
        public val Proud: com.bkahlert.kommons.kaomoji.categories.Proud = com.bkahlert.kommons.kaomoji.categories.Proud

        /**
         * Selection of punching [Kaomoji]
         */
        public val Punching: com.bkahlert.kommons.kaomoji.categories.Punching = com.bkahlert.kommons.kaomoji.categories.Punching

        /**
         * Selection of rabbit [Kaomoji]
         */
        public val Rabbits: com.bkahlert.kommons.kaomoji.categories.Rabbit = com.bkahlert.kommons.kaomoji.categories.Rabbit

        /**
         * Selection of rain-related [Kaomoji]
         */
        public val Rain: com.bkahlert.kommons.kaomoji.categories.Rain = com.bkahlert.kommons.kaomoji.categories.Rain

        /**
         * Selection of roger that [Kaomoji]
         */
        public val RogerThat: com.bkahlert.kommons.kaomoji.categories.RogerThat = com.bkahlert.kommons.kaomoji.categories.RogerThat

        /**
         * Selection of roll over [Kaomoji]
         */
        public val RollOver: com.bkahlert.kommons.kaomoji.categories.RollOver = com.bkahlert.kommons.kaomoji.categories.RollOver

        /**
         * Selection of running [Kaomoji]
         */
        public val Running: com.bkahlert.kommons.kaomoji.categories.Running = com.bkahlert.kommons.kaomoji.categories.Running

        /**
         * Selection of sad [Kaomoji]
         */
        public val Sad: com.bkahlert.kommons.kaomoji.categories.Sad = com.bkahlert.kommons.kaomoji.categories.Sad

        /**
         * Selection of salute [Kaomoji]
         */
        public val Salute: com.bkahlert.kommons.kaomoji.categories.Salute = com.bkahlert.kommons.kaomoji.categories.Salute

        /**
         * Selection of scared [Kaomoji]
         */
        public val Scared: com.bkahlert.kommons.kaomoji.categories.Scared = com.bkahlert.kommons.kaomoji.categories.Scared

        /**
         * Selection of screaming [Kaomoji]
         */
        public val Screaming: com.bkahlert.kommons.kaomoji.categories.Screaming = com.bkahlert.kommons.kaomoji.categories.Screaming

        /**
         * Selection of sheep [Kaomoji]
         */
        public val Sheep: com.bkahlert.kommons.kaomoji.categories.Sheep = com.bkahlert.kommons.kaomoji.categories.Sheep

        /**
         * Selection of shocked [Kaomoji]
         */
        public val Shocked: com.bkahlert.kommons.kaomoji.categories.Shocked = com.bkahlert.kommons.kaomoji.categories.Shocked

        /**
         * Selection of shrugging [Kaomoji]
         */
        public val Shrugging: com.bkahlert.kommons.kaomoji.categories.Shrugging = com.bkahlert.kommons.kaomoji.categories.Shrugging

        /**
         * Selection of shy [Kaomoji]
         */
        public val Shy: com.bkahlert.kommons.kaomoji.categories.Shy = com.bkahlert.kommons.kaomoji.categories.Shy

        /**
         * Selection of sleeping [Kaomoji]
         */
        public val Sleeping: com.bkahlert.kommons.kaomoji.categories.Sleeping = com.bkahlert.kommons.kaomoji.categories.Sleeping

        /**
         * Selection of smiling [Kaomoji]
         */
        public val Smiling: com.bkahlert.kommons.kaomoji.categories.Smiling = com.bkahlert.kommons.kaomoji.categories.Smiling

        /**
         * Selection of smoking [Kaomoji]
         */
        public val Smoking: com.bkahlert.kommons.kaomoji.categories.Smoking = com.bkahlert.kommons.kaomoji.categories.Smoking

        /**
         * Selection of sparkling [Kaomoji]
         */
        public val Sparkling: com.bkahlert.kommons.kaomoji.categories.Sparkling = com.bkahlert.kommons.kaomoji.categories.Sparkling

        /**
         * Selection of spinning [Kaomoji]
         */
        public val Spinning: com.bkahlert.kommons.kaomoji.categories.Spinning = com.bkahlert.kommons.kaomoji.categories.Spinning

        /**
         * Selection of stereo type [Kaomoji]
         */
        public val StereoTypes: com.bkahlert.kommons.kaomoji.categories.StereoTypes = com.bkahlert.kommons.kaomoji.categories.StereoTypes

        /**
         * Selection of surprised [Kaomoji]
         */
        public val Surprised: com.bkahlert.kommons.kaomoji.categories.Surprised = com.bkahlert.kommons.kaomoji.categories.Surprised

        /**
         * Selection of sweating [Kaomoji]
         */
        public val Sweating: com.bkahlert.kommons.kaomoji.categories.Sweating = com.bkahlert.kommons.kaomoji.categories.Sweating

        /**
         * Selection of table flipping [Kaomoji]
         */
        public val TableFlipping: com.bkahlert.kommons.kaomoji.categories.TableFlipping = com.bkahlert.kommons.kaomoji.categories.TableFlipping

        /**
         * Selection of take abow [Kaomoji]
         */
        public val TakeABow: com.bkahlert.kommons.kaomoji.categories.TakeABow = com.bkahlert.kommons.kaomoji.categories.TakeABow

        /**
         * Selection of thats it [Kaomoji]
         */
        public val ThatsIt: com.bkahlert.kommons.kaomoji.categories.ThatsIt = com.bkahlert.kommons.kaomoji.categories.ThatsIt

        /**
         * Selection of thumbs up [Kaomoji]
         */
        public val ThumbsUp: com.bkahlert.kommons.kaomoji.categories.ThumbsUp = com.bkahlert.kommons.kaomoji.categories.ThumbsUp

        /**
         * Selection of tired [Kaomoji]
         */
        public val Tired: com.bkahlert.kommons.kaomoji.categories.Tired = com.bkahlert.kommons.kaomoji.categories.Tired

        /**
         * Selection of trembling [Kaomoji]
         */
        public val Trembling: com.bkahlert.kommons.kaomoji.categories.Trembling = com.bkahlert.kommons.kaomoji.categories.Trembling

        /**
         * Selection of try my best [Kaomoji]
         */
        public val TryMyBest: com.bkahlert.kommons.kaomoji.categories.TryMyBest = com.bkahlert.kommons.kaomoji.categories.TryMyBest

        /**
         * Selection of TV-related [Kaomoji]
         */
        public val TV: com.bkahlert.kommons.kaomoji.categories.TV = com.bkahlert.kommons.kaomoji.categories.TV

        /**
         * Selection of upset [Kaomoji]
         */
        public val Upset: com.bkahlert.kommons.kaomoji.categories.Upset = com.bkahlert.kommons.kaomoji.categories.Upset

        /**
         * Selection of vomitting [Kaomoji]
         */
        public val Vomitting: com.bkahlert.kommons.kaomoji.categories.Vomitting = com.bkahlert.kommons.kaomoji.categories.Vomitting

        /**
         * Selection of weapon-related [Kaomoji]
         */
        public val Weapons: com.bkahlert.kommons.kaomoji.categories.Weapons = com.bkahlert.kommons.kaomoji.categories.Weapons

        /**
         * Selection of weird [Kaomoji]
         */
        public val Weird: com.bkahlert.kommons.kaomoji.categories.Weird = com.bkahlert.kommons.kaomoji.categories.Weird

        /**
         * Selection of whale [Kaomoji]
         */
        public val Whales: com.bkahlert.kommons.kaomoji.categories.Whales = com.bkahlert.kommons.kaomoji.categories.Whales

        /**
         * Selection of why [Kaomoji]
         */
        public val Why: com.bkahlert.kommons.kaomoji.categories.Why = com.bkahlert.kommons.kaomoji.categories.Why

        /**
         * Selection of winking [Kaomoji]
         */
        public val Winking: com.bkahlert.kommons.kaomoji.categories.Winking = com.bkahlert.kommons.kaomoji.categories.Winking

        /**
         * Selection of wizard [Kaomoji]
         */
        public val Wizards: com.bkahlert.kommons.kaomoji.categories.Wizards = com.bkahlert.kommons.kaomoji.categories.Wizards

        /**
         * Selection of writing [Kaomoji]
         */
        public val Writing: com.bkahlert.kommons.kaomoji.categories.Writing = com.bkahlert.kommons.kaomoji.categories.Writing
    }
}
