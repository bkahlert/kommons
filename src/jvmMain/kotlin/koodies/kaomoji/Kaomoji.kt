package koodies.kaomoji

import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.CodePoint
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.asCodePointSequence
import koodies.text.charCount
import koodies.text.codePointCount
import koodies.text.maxLength
import koodies.text.padEnd
import koodies.text.takeIfNotBlank
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

    public val leftArm: CharSequence get() = kaomojiString.subSequence(leftArmRange)
    public val leftEye: CharSequence get() = kaomojiString.subSequence(leftEyeRange)
    public val mouth: CharSequence get() = kaomojiString.subSequence(mouthRange)
    public val rightEye: CharSequence get() = kaomojiString.subSequence(rightEyeRange)
    public val rightArm: CharSequence get() = kaomojiString.subSequence(rightArmRange)
    public val accessory: CharSequence get() = kaomojiString.subSequence(accessoryRange)
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

    private val blank: String by lazy { ansi.hidden.done }

    /**
     * Returns a thinking [Kaomoji] of the form:
     *
     * ```
     *           ͚͔˱ ❨ ( something )
     * (^～^) ˙
     * ```
     */
    public fun thinking(text: String? = null): String {
        val subject = text?.takeIfNotBlank()
        val lines = subject.lines()
        return when (lines.size) {
            0 -> {
                """
                    $blank   ͚͔˱ ❨ ( … )
                    $this ˙
                """.trimIndent()
            }
            1 -> {
                """
                    $blank   ͚͔˱ ❨ ( $subject )
                    $this ˙
                """.trimIndent()
            }
            2 -> {
                val max = lines.maxLength()
                """
                    $blank       ⎛ ${lines.first().padEnd(max)} ⎞
                    $blank   ͚͔˱ ❨ ⎝ ${lines.last().padEnd(max)} ⎠
                    $this ˙
                """.trimIndent()
            }
            else -> {
                val max = lines.maxLength()
                "$blank       ⎛ ${lines.first().padEnd(max)} ⎞$LF" +
                    lines.drop(1).dropLast(1).joinToString("") { "$blank       ⎜ ${it.padEnd(max)} ⎟$LF" } +
                    "$blank   ͚͔˱ ❨ ⎝ ${lines.last().padEnd(max)} ⎠$LF" +
                    this + " ˙"
            }
        }
    }

    override val length: Int get() = toString().length
    override fun get(index: Int): Char = toString().get(index)
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
    override fun toString(): String = string
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kaomoji

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int = string.hashCode()


    @Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")
    public companion object {

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
        @Suppress("unused") public val Angry: koodies.kaomoji.categories.Angry = koodies.kaomoji.categories.Angry

        /**
         * Selection of bad mood [Kaomoji]
         */
        @Suppress("unused") public val BadMood: koodies.kaomoji.categories.BadMood = koodies.kaomoji.categories.BadMood

        /**
         * Selection of baby [Kaomoji]
         */
        @Suppress("unused") public val Babies: koodies.kaomoji.categories.Babies = koodies.kaomoji.categories.Babies

        /**
         * Selection of bear [Kaomoji]
         */
        @Suppress("unused") public val Bears: koodies.kaomoji.categories.Bears = koodies.kaomoji.categories.Bears

        /**
         * Selection of begging [Kaomoji]
         */
        @Suppress("unused") public val Begging: koodies.kaomoji.categories.Begging = koodies.kaomoji.categories.Begging

        /**
         * Selection of blushing [Kaomoji]
         */
        @Suppress("unused") public val Blushing: koodies.kaomoji.categories.Blushing = koodies.kaomoji.categories.Blushing

        /**
         * Selection of cat [Kaomoji]
         */
        @Suppress("unused") public val Cats: koodies.kaomoji.categories.Cats = koodies.kaomoji.categories.Cats

        /**
         * Selection of confused [Kaomoji]
         */
        @Suppress("unused") public val Confused: koodies.kaomoji.categories.Confused = koodies.kaomoji.categories.Confused

        /**
         * Selection of crying [Kaomoji]
         */
        @Suppress("unused") public val Crying: koodies.kaomoji.categories.Crying = koodies.kaomoji.categories.Crying

        /**
         * Selection of cute [Kaomoji]
         */
        @Suppress("unused") public val Cute: koodies.kaomoji.categories.Cute = koodies.kaomoji.categories.Cute

        /**
         * Selection of dancing [Kaomoji]
         */
        @Suppress("unused") public val Dancing: koodies.kaomoji.categories.Dance = koodies.kaomoji.categories.Dance

        /**
         * Selection of depressed [Kaomoji]
         */
        @Suppress("unused") public val Depressed: koodies.kaomoji.categories.Depressed = koodies.kaomoji.categories.Depressed

        /**
         * Selection of devil [Kaomoji]
         */
        @Suppress("unused") public val Devils: koodies.kaomoji.categories.Devil = koodies.kaomoji.categories.Devil

        /**
         * Selection of disappointed [Kaomoji]
         */
        @Suppress("unused") public val Disappointed: koodies.kaomoji.categories.Disappointed = koodies.kaomoji.categories.Disappointed

        /**
         * Selection of drooling [Kaomoji]
         */
        @Suppress("unused") public val Drooling: koodies.kaomoji.categories.Drooling = koodies.kaomoji.categories.Drooling

        /**
         * Selection of eating [Kaomoji]
         */
        @Suppress("unused") public val Eating: koodies.kaomoji.categories.Eating = koodies.kaomoji.categories.Eating

        /**
         * Selection of evil [Kaomoji]
         */
        @Suppress("unused") public val Evil: koodies.kaomoji.categories.Evil = koodies.kaomoji.categories.Evil

        /**
         * Selection of excited [Kaomoji]
         */
        @Suppress("unused") public val Excited: koodies.kaomoji.categories.Excited = koodies.kaomoji.categories.Excited

        /**
         * Selection of falling down [Kaomoji]
         */
        @Suppress("unused") public val FallingDown: koodies.kaomoji.categories.FallingDown = koodies.kaomoji.categories.FallingDown

        /**
         * Selection of feces-related [Kaomoji]
         */
        @Suppress("unused") public val Feces: koodies.kaomoji.categories.Feces = koodies.kaomoji.categories.Feces

        /**
         * Selection of feminine [Kaomoji]
         */
        @Suppress("unused") public val Feminine: koodies.kaomoji.categories.Feminine = koodies.kaomoji.categories.Feminine

        /**
         * Selection of table flipping [Kaomoji]
         */
        @Suppress("unused") public val TableFlipping: koodies.kaomoji.categories.TableFlipping = koodies.kaomoji.categories.TableFlipping

        /**
         * Selection of flower-related [Kaomoji]
         */
        @Suppress("unused") public val Flower: koodies.kaomoji.categories.Flower = koodies.kaomoji.categories.Flower

        /**
         * Selection of funny [Kaomoji]
         */
        @Suppress("unused") public val Funny: koodies.kaomoji.categories.Funny = koodies.kaomoji.categories.Funny

        /**
         * Selection of [Kaomoji] with glasses.
         */
        @Suppress("unused") public val Glasses: koodies.kaomoji.categories.Glasses = koodies.kaomoji.categories.Glasses

        /**
         * Selection of grin [Kaomoji]
         */
        @Suppress("unused") public val Grinning: koodies.kaomoji.categories.Grinning = koodies.kaomoji.categories.Grinning

        /**
         * Selection of gross [Kaomoji]
         */
        @Suppress("unused") public val Gross: koodies.kaomoji.categories.Gross = koodies.kaomoji.categories.Gross

        /**
         * Selection of happy [Kaomoji]
         */
        @Suppress("unused") public val Happy: koodies.kaomoji.categories.Happy = koodies.kaomoji.categories.Happy

        /**
         * Selection of greeting [Kaomoji]
         */
        @Suppress("unused") public val Greeting: koodies.kaomoji.categories.Greeting = koodies.kaomoji.categories.Greeting

        /**
         * Selection of helpless [Kaomoji]
         */
        @Suppress("unused") public val Helpless: koodies.kaomoji.categories.Helpless = koodies.kaomoji.categories.Helpless

        /**
         * Selection of hide [Kaomoji]
         */
        @Suppress("unused") public val Hide: koodies.kaomoji.categories.Hide = koodies.kaomoji.categories.Hide

        /**
         * Selection of hugging [Kaomoji]
         */
        @Suppress("unused") public val Hugging: koodies.kaomoji.categories.Hugging = koodies.kaomoji.categories.Hugging

        /**
         * Selection of kissing [Kaomoji]
         */
        @Suppress("unused") public val Kissing: koodies.kaomoji.categories.Kissing = koodies.kaomoji.categories.Kissing

        /**
         * Selection of laughing [Kaomoji]
         */
        @Suppress("unused") public val Laughing: koodies.kaomoji.categories.Laughing = koodies.kaomoji.categories.Laughing

        /**
         * Selection of lenny face [Kaomoji]
         */
        @Suppress("unused") public val LennyFace: koodies.kaomoji.categories.LennyFace = koodies.kaomoji.categories.LennyFace

        /**
         * Selection of love-related [Kaomoji]
         */
        @Suppress("unused") public val Love: koodies.kaomoji.categories.Love = koodies.kaomoji.categories.Love

        /**
         * Selection of magical [Kaomoji]
         */
        @Suppress("unused") public val Magical: koodies.kaomoji.categories.Magical = koodies.kaomoji.categories.Magical

        /**
         * Selection of make up my mind [Kaomoji]
         */
        @Suppress("unused") public val MakeUpMyMind: koodies.kaomoji.categories.MakeUpMyMind = koodies.kaomoji.categories.MakeUpMyMind

        /**
         * Selection of middle finger [Kaomoji]
         */
        @Suppress("unused") public val MiddleFinger: koodies.kaomoji.categories.MiddleFinger = koodies.kaomoji.categories.MiddleFinger

        /**
         * Selection of monkey [Kaomoji]
         */
        @Suppress("unused") public val Monkey: koodies.kaomoji.categories.Monkey = koodies.kaomoji.categories.Monkey

        /**
         * Selection of musical [Kaomoji]
         */
        @Suppress("unused") public val Musical: koodies.kaomoji.categories.Musical = koodies.kaomoji.categories.Musical

        /**
         * Selection of nervious [Kaomoji]
         */
        @Suppress("unused") public val Nervious: koodies.kaomoji.categories.Nervious = koodies.kaomoji.categories.Nervious

        /**
         * Selection of peace sign [Kaomoji]
         */
        @Suppress("unused") public val PeaceSign: koodies.kaomoji.categories.PeaceSign = koodies.kaomoji.categories.PeaceSign

        /**
         * Selection of proud [Kaomoji]
         */
        @Suppress("unused") public val Proud: koodies.kaomoji.categories.Proud = koodies.kaomoji.categories.Proud

        /**
         * Selection of punching [Kaomoji]
         */
        @Suppress("unused") public val Punching: koodies.kaomoji.categories.Punching = koodies.kaomoji.categories.Punching

        /**
         * Selection of rabbit [Kaomoji]
         */
        @Suppress("unused") public val Rabbits: koodies.kaomoji.categories.Rabbit = koodies.kaomoji.categories.Rabbit

        /**
         * Selection of roger that [Kaomoji]
         */
        @Suppress("unused") public val RogerThat: koodies.kaomoji.categories.RogerThat = koodies.kaomoji.categories.RogerThat

        /**
         * Selection of roll over [Kaomoji]
         */
        @Suppress("unused") public val RollOver: koodies.kaomoji.categories.RollOver = koodies.kaomoji.categories.RollOver

        /**
         * Selection of running [Kaomoji]
         */
        @Suppress("unused") public val Running: koodies.kaomoji.categories.Running = koodies.kaomoji.categories.Running

        /**
         * Selection of sad [Kaomoji]
         */
        @Suppress("unused") public val Sad: koodies.kaomoji.categories.Sad = koodies.kaomoji.categories.Sad

        /**
         * Selection of salute [Kaomoji]
         */
        @Suppress("unused") public val Salute: koodies.kaomoji.categories.Salute = koodies.kaomoji.categories.Salute

        /**
         * Selection of scared [Kaomoji]
         */
        @Suppress("unused") public val Scared: koodies.kaomoji.categories.Scared = koodies.kaomoji.categories.Scared

        /**
         * Selection of sheep [Kaomoji]
         */
        @Suppress("unused") public val Sheep: koodies.kaomoji.categories.Sheep = koodies.kaomoji.categories.Sheep

        /**
         * Selection of shocked [Kaomoji]
         */
        @Suppress("unused") public val Shocked: koodies.kaomoji.categories.Shocked = koodies.kaomoji.categories.Shocked

        /**
         * Selection of shrugging [Kaomoji]
         */
        @Suppress("unused") public val Shrugging: koodies.kaomoji.categories.Shrugging = koodies.kaomoji.categories.Shrugging

        /**
         * Selection of shy [Kaomoji]
         */
        @Suppress("unused") public val Shy: koodies.kaomoji.categories.Shy = koodies.kaomoji.categories.Shy

        /**
         * Selection of sleeping [Kaomoji]
         */
        @Suppress("unused") public val Sleeping: koodies.kaomoji.categories.Sleeping = koodies.kaomoji.categories.Sleeping

        /**
         * Selection of smiling [Kaomoji]
         */
        @Suppress("unused") public val Smiling: koodies.kaomoji.categories.Smiling = koodies.kaomoji.categories.Smiling

        /**
         * Selection of sparkling [Kaomoji]
         */
        @Suppress("unused") public val Sparkling: koodies.kaomoji.categories.Sparkling = koodies.kaomoji.categories.Sparkling

        /**
         * Selection of spinning [Kaomoji]
         */
        @Suppress("unused") public val Spinning: koodies.kaomoji.categories.Spinning = koodies.kaomoji.categories.Spinning

        /**
         * Selection of surprised [Kaomoji]
         */
        @Suppress("unused") public val Surprised: koodies.kaomoji.categories.Surprised = koodies.kaomoji.categories.Surprised

        /**
         * Selection of sweating [Kaomoji]
         */
        @Suppress("unused") public val Sweating: koodies.kaomoji.categories.Sweating = koodies.kaomoji.categories.Sweating

        /**
         * Selection of take abow [Kaomoji]
         */
        @Suppress("unused") public val TakeABow: koodies.kaomoji.categories.TakeABow = koodies.kaomoji.categories.TakeABow

        /**
         * Selection of thats it [Kaomoji]
         */
        @Suppress("unused") public val ThatsIt: koodies.kaomoji.categories.ThatsIt = koodies.kaomoji.categories.ThatsIt

        /**
         * Selection of thumbs up [Kaomoji]
         */
        @Suppress("unused") public val ThumbsUp: koodies.kaomoji.categories.ThumbsUp = koodies.kaomoji.categories.ThumbsUp

        /**
         * Selection of tired [Kaomoji]
         */
        @Suppress("unused") public val Tired: koodies.kaomoji.categories.Tired = koodies.kaomoji.categories.Tired

        /**
         * Selection of trembling [Kaomoji]
         */
        @Suppress("unused") public val Trembling: koodies.kaomoji.categories.Trembling = koodies.kaomoji.categories.Trembling

        /**
         * Selection of try my best [Kaomoji]
         */
        @Suppress("unused") public val TryMyBest: koodies.kaomoji.categories.TryMyBest = koodies.kaomoji.categories.TryMyBest

        /**
         * Selection of unicode [Kaomoji]
         */
        @Suppress("unused") public val Unicode: koodies.kaomoji.categories.Unicode = koodies.kaomoji.categories.Unicode

        /**
         * Selection of upset [Kaomoji]
         */
        @Suppress("unused") public val Upset: koodies.kaomoji.categories.Upset = koodies.kaomoji.categories.Upset

        /**
         * Selection of vomitting [Kaomoji]
         */
        @Suppress("unused") public val Vomitting: koodies.kaomoji.categories.Vomitting = koodies.kaomoji.categories.Vomitting

        /**
         * Selection of weird [Kaomoji]
         */
        @Suppress("unused") public val Weird: koodies.kaomoji.categories.Weird = koodies.kaomoji.categories.Weird

        /**
         * Selection of winking [Kaomoji]
         */
        @Suppress("unused") public val Winking: koodies.kaomoji.categories.Winking = koodies.kaomoji.categories.Winking

        /**
         * Selection of writing [Kaomoji]
         */
        @Suppress("unused") public val Writing: koodies.kaomoji.categories.Writing = koodies.kaomoji.categories.Writing

        /**
         * Selection of smoking [Kaomoji]
         */
        @Suppress("unused") public val Smoking: koodies.kaomoji.categories.Smoking = koodies.kaomoji.categories.Smoking

        /**
         * Selection of rain-related [Kaomoji]
         */
        @Suppress("unused") public val Rain: koodies.kaomoji.categories.Rain = koodies.kaomoji.categories.Rain

        /**
         * Selection of TV-related [Kaomoji]
         */
        @Suppress("unused") public val TV: koodies.kaomoji.categories.TV = koodies.kaomoji.categories.TV

        /**
         * Selection of fishing [Kaomoji]
         */
        @Suppress("unused") public val Fishing: koodies.kaomoji.categories.Fishing = koodies.kaomoji.categories.Fishing

        /**
         * Selection of fish [Kaomoji]
         */
        @Suppress("unused") public val Fish: koodies.kaomoji.categories.Fish = koodies.kaomoji.categories.Fish

        /**
         * Selection of whale [Kaomoji]
         */
        @Suppress("unused") public val Whales: koodies.kaomoji.categories.Whales = koodies.kaomoji.categories.Whales

        /**
         * Selection of weapon-related [Kaomoji]
         */
        @Suppress("unused") public val Weapons: koodies.kaomoji.categories.Weapons = koodies.kaomoji.categories.Weapons

        /**
         * Selection of money [Kaomoji]
         */
        @Suppress("unused") public val Money: koodies.kaomoji.categories.Money = koodies.kaomoji.categories.Money

        /**
         * Selection of screaming [Kaomoji]
         */
        @Suppress("unused") public val Screaming: koodies.kaomoji.categories.Screaming = koodies.kaomoji.categories.Screaming

        /**
         * Selection of why [Kaomoji]
         */
        @Suppress("unused") public val Why: koodies.kaomoji.categories.Why = koodies.kaomoji.categories.Why

        /**
         * Selection of geek [Kaomoji]
         */
        @Suppress("unused") public val Geek: koodies.kaomoji.categories.Geek = koodies.kaomoji.categories.Geek

        /**
         * Selection of pointing [Kaomoji]
         */
        @Suppress("unused") public val Pointing: koodies.kaomoji.categories.Pointing = koodies.kaomoji.categories.Pointing

        /**
         * Selection of chasing [Kaomoji]
         */
        @Suppress("unused") public val Chasing: koodies.kaomoji.categories.Chasing = koodies.kaomoji.categories.Chasing

        /**
         * Selection of celebrity [Kaomoji]
         */
        @Suppress("unused") public val Celebrities: koodies.kaomoji.categories.Celebrities = koodies.kaomoji.categories.Celebrities

        /**
         * Selection of hero [Kaomoji]
         */
        @Suppress("unused") public val Heroes: koodies.kaomoji.categories.Heroes = koodies.kaomoji.categories.Heroes

        /**
         * Selection of dog [Kaomoji]
         */
        @Suppress("unused") public val Dog: koodies.kaomoji.categories.Dog = koodies.kaomoji.categories.Dog

        /**
         * Selection of stereo type [Kaomoji]
         */
        @Suppress("unused") public val StereoTypes: koodies.kaomoji.categories.StereoTypes = koodies.kaomoji.categories.StereoTypes

        /**
         * Selection of wizard [Kaomoji]
         */
        @Suppress("unused") public val Wizards: koodies.kaomoji.categories.Wizards = koodies.kaomoji.categories.Wizards
    }
}
