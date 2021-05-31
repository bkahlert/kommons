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

    private val blank: String by lazy { ansi.hidden.done }

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
        val lines = subject.lines()
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
        public val Angry: koodies.kaomoji.categories.Angry = koodies.kaomoji.categories.Angry

        /**
         * Selection of baby [Kaomoji]
         */
        public val Babies: koodies.kaomoji.categories.Babies = koodies.kaomoji.categories.Babies

        /**
         * Selection of bad mood [Kaomoji]
         */
        public val BadMood: koodies.kaomoji.categories.BadMood = koodies.kaomoji.categories.BadMood

        /**
         * Selection of bear [Kaomoji]
         */
        public val Bears: koodies.kaomoji.categories.Bears = koodies.kaomoji.categories.Bears

        /**
         * Selection of begging [Kaomoji]
         */
        public val Begging: koodies.kaomoji.categories.Begging = koodies.kaomoji.categories.Begging

        /**
         * Selection of blushing [Kaomoji]
         */
        public val Blushing: koodies.kaomoji.categories.Blushing = koodies.kaomoji.categories.Blushing

        /**
         * Selection of cat [Kaomoji]
         */
        public val Cats: koodies.kaomoji.categories.Cats = koodies.kaomoji.categories.Cats

        /**
         * Selection of celebrity [Kaomoji]
         */
        public val Celebrities: koodies.kaomoji.categories.Celebrities = koodies.kaomoji.categories.Celebrities

        /**
         * Selection of chasing [Kaomoji]
         */
        public val Chasing: koodies.kaomoji.categories.Chasing = koodies.kaomoji.categories.Chasing

        /**
         * Selection of confused [Kaomoji]
         */
        public val Confused: koodies.kaomoji.categories.Confused = koodies.kaomoji.categories.Confused

        /**
         * Selection of crying [Kaomoji]
         */
        public val Crying: koodies.kaomoji.categories.Crying = koodies.kaomoji.categories.Crying

        /**
         * Selection of cute [Kaomoji]
         */
        public val Cute: koodies.kaomoji.categories.Cute = koodies.kaomoji.categories.Cute

        /**
         * Selection of dancing [Kaomoji]
         */
        public val Dancing: koodies.kaomoji.categories.Dancing = koodies.kaomoji.categories.Dancing

        /**
         * Selection of depressed [Kaomoji]
         */
        public val Depressed: koodies.kaomoji.categories.Depressed = koodies.kaomoji.categories.Depressed

        /**
         * Selection of devil [Kaomoji]
         */
        public val Devils: koodies.kaomoji.categories.Devil = koodies.kaomoji.categories.Devil

        /**
         * Selection of disappointed [Kaomoji]
         */
        public val Disappointed: koodies.kaomoji.categories.Disappointed = koodies.kaomoji.categories.Disappointed

        /**
         * Selection of dog [Kaomoji]
         */
        public val Dog: koodies.kaomoji.categories.Dog = koodies.kaomoji.categories.Dog

        /**
         * Selection of drooling [Kaomoji]
         */
        public val Drooling: koodies.kaomoji.categories.Drooling = koodies.kaomoji.categories.Drooling

        /**
         * Selection of eating [Kaomoji]
         */
        public val Eating: koodies.kaomoji.categories.Eating = koodies.kaomoji.categories.Eating

        /**
         * Selection of evil [Kaomoji]
         */
        public val Evil: koodies.kaomoji.categories.Evil = koodies.kaomoji.categories.Evil

        /**
         * Selection of excited [Kaomoji]
         */
        public val Excited: koodies.kaomoji.categories.Excited = koodies.kaomoji.categories.Excited

        /**
         * Selection of falling down [Kaomoji]
         */
        public val FallingDown: koodies.kaomoji.categories.FallingDown = koodies.kaomoji.categories.FallingDown

        /**
         * Selection of feces-related [Kaomoji]
         */
        public val Feces: koodies.kaomoji.categories.Feces = koodies.kaomoji.categories.Feces

        /**
         * Selection of feminine [Kaomoji]
         */
        public val Feminine: koodies.kaomoji.categories.Feminine = koodies.kaomoji.categories.Feminine

        /**
         * Selection of fish [Kaomoji]
         */
        public val Fish: koodies.kaomoji.categories.Fish = koodies.kaomoji.categories.Fish

        /**
         * Selection of fishing [Kaomoji]
         */
        public val Fishing: koodies.kaomoji.categories.Fishing = koodies.kaomoji.categories.Fishing

        /**
         * Selection of flower-related [Kaomoji]
         */
        public val Flower: koodies.kaomoji.categories.Flower = koodies.kaomoji.categories.Flower

        /**
         * Selection of funny [Kaomoji]
         */
        public val Funny: koodies.kaomoji.categories.Funny = koodies.kaomoji.categories.Funny

        /**
         * Selection of geek [Kaomoji]
         */
        public val Geek: koodies.kaomoji.categories.Geek = koodies.kaomoji.categories.Geek

        /**
         * Selection of [Kaomoji] with glasses.
         */
        public val Glasses: koodies.kaomoji.categories.Glasses = koodies.kaomoji.categories.Glasses

        /**
         * Selection of greeting [Kaomoji]
         */
        public val Greeting: koodies.kaomoji.categories.Greeting = koodies.kaomoji.categories.Greeting

        /**
         * Selection of grin [Kaomoji]
         */
        public val Grinning: koodies.kaomoji.categories.Grinning = koodies.kaomoji.categories.Grinning

        /**
         * Selection of gross [Kaomoji]
         */
        public val Gross: koodies.kaomoji.categories.Gross = koodies.kaomoji.categories.Gross

        /**
         * Selection of happy [Kaomoji]
         */
        public val Happy: koodies.kaomoji.categories.Happy = koodies.kaomoji.categories.Happy

        /**
         * Selection of helpless [Kaomoji]
         */
        public val Helpless: koodies.kaomoji.categories.Helpless = koodies.kaomoji.categories.Helpless

        /**
         * Selection of hero [Kaomoji]
         */
        public val Heroes: koodies.kaomoji.categories.Heroes = koodies.kaomoji.categories.Heroes

        /**
         * Selection of hide [Kaomoji]
         */
        public val Hide: koodies.kaomoji.categories.Hide = koodies.kaomoji.categories.Hide

        /**
         * Selection of hugging [Kaomoji]
         */
        public val Hugging: koodies.kaomoji.categories.Hugging = koodies.kaomoji.categories.Hugging

        /**
         * Selection of kissing [Kaomoji]
         */
        public val Kissing: koodies.kaomoji.categories.Kissing = koodies.kaomoji.categories.Kissing

        /**
         * Selection of laughing [Kaomoji]
         */
        public val Laughing: koodies.kaomoji.categories.Laughing = koodies.kaomoji.categories.Laughing

        /**
         * Selection of lenny face [Kaomoji]
         */
        public val LennyFace: koodies.kaomoji.categories.LennyFace = koodies.kaomoji.categories.LennyFace

        /**
         * Selection of love-related [Kaomoji]
         */
        public val Love: koodies.kaomoji.categories.Love = koodies.kaomoji.categories.Love

        /**
         * Selection of magical [Kaomoji]
         */
        public val Magical: koodies.kaomoji.categories.Magical = koodies.kaomoji.categories.Magical

        /**
         * Selection of make up my mind [Kaomoji]
         */
        public val MakeUpMyMind: koodies.kaomoji.categories.MakeUpMyMind = koodies.kaomoji.categories.MakeUpMyMind

        /**
         * Selection of middle finger [Kaomoji]
         */
        public val MiddleFinger: koodies.kaomoji.categories.MiddleFinger = koodies.kaomoji.categories.MiddleFinger

        /**
         * Selection of money [Kaomoji]
         */
        public val Money: koodies.kaomoji.categories.Money = koodies.kaomoji.categories.Money

        /**
         * Selection of monkey [Kaomoji]
         */
        public val Monkey: koodies.kaomoji.categories.Monkey = koodies.kaomoji.categories.Monkey

        /**
         * Selection of musical [Kaomoji]
         */
        public val Musical: koodies.kaomoji.categories.Musical = koodies.kaomoji.categories.Musical

        /**
         * Selection of nervious [Kaomoji]
         */
        public val Nervious: koodies.kaomoji.categories.Nervious = koodies.kaomoji.categories.Nervious

        /**
         * Selection of peace sign [Kaomoji]
         */
        public val PeaceSign: koodies.kaomoji.categories.PeaceSign = koodies.kaomoji.categories.PeaceSign

        /**
         * Selection of pointing [Kaomoji]
         */
        public val Pointing: koodies.kaomoji.categories.Pointing = koodies.kaomoji.categories.Pointing

        /**
         * Selection of proud [Kaomoji]
         */
        public val Proud: koodies.kaomoji.categories.Proud = koodies.kaomoji.categories.Proud

        /**
         * Selection of punching [Kaomoji]
         */
        public val Punching: koodies.kaomoji.categories.Punching = koodies.kaomoji.categories.Punching

        /**
         * Selection of rabbit [Kaomoji]
         */
        public val Rabbits: koodies.kaomoji.categories.Rabbit = koodies.kaomoji.categories.Rabbit

        /**
         * Selection of rain-related [Kaomoji]
         */
        public val Rain: koodies.kaomoji.categories.Rain = koodies.kaomoji.categories.Rain

        /**
         * Selection of roger that [Kaomoji]
         */
        public val RogerThat: koodies.kaomoji.categories.RogerThat = koodies.kaomoji.categories.RogerThat

        /**
         * Selection of roll over [Kaomoji]
         */
        public val RollOver: koodies.kaomoji.categories.RollOver = koodies.kaomoji.categories.RollOver

        /**
         * Selection of running [Kaomoji]
         */
        public val Running: koodies.kaomoji.categories.Running = koodies.kaomoji.categories.Running

        /**
         * Selection of sad [Kaomoji]
         */
        public val Sad: koodies.kaomoji.categories.Sad = koodies.kaomoji.categories.Sad

        /**
         * Selection of salute [Kaomoji]
         */
        public val Salute: koodies.kaomoji.categories.Salute = koodies.kaomoji.categories.Salute

        /**
         * Selection of scared [Kaomoji]
         */
        public val Scared: koodies.kaomoji.categories.Scared = koodies.kaomoji.categories.Scared

        /**
         * Selection of screaming [Kaomoji]
         */
        public val Screaming: koodies.kaomoji.categories.Screaming = koodies.kaomoji.categories.Screaming

        /**
         * Selection of sheep [Kaomoji]
         */
        public val Sheep: koodies.kaomoji.categories.Sheep = koodies.kaomoji.categories.Sheep

        /**
         * Selection of shocked [Kaomoji]
         */
        public val Shocked: koodies.kaomoji.categories.Shocked = koodies.kaomoji.categories.Shocked

        /**
         * Selection of shrugging [Kaomoji]
         */
        public val Shrugging: koodies.kaomoji.categories.Shrugging = koodies.kaomoji.categories.Shrugging

        /**
         * Selection of shy [Kaomoji]
         */
        public val Shy: koodies.kaomoji.categories.Shy = koodies.kaomoji.categories.Shy

        /**
         * Selection of sleeping [Kaomoji]
         */
        public val Sleeping: koodies.kaomoji.categories.Sleeping = koodies.kaomoji.categories.Sleeping

        /**
         * Selection of smiling [Kaomoji]
         */
        public val Smiling: koodies.kaomoji.categories.Smiling = koodies.kaomoji.categories.Smiling

        /**
         * Selection of smoking [Kaomoji]
         */
        public val Smoking: koodies.kaomoji.categories.Smoking = koodies.kaomoji.categories.Smoking

        /**
         * Selection of sparkling [Kaomoji]
         */
        public val Sparkling: koodies.kaomoji.categories.Sparkling = koodies.kaomoji.categories.Sparkling

        /**
         * Selection of spinning [Kaomoji]
         */
        public val Spinning: koodies.kaomoji.categories.Spinning = koodies.kaomoji.categories.Spinning

        /**
         * Selection of stereo type [Kaomoji]
         */
        public val StereoTypes: koodies.kaomoji.categories.StereoTypes = koodies.kaomoji.categories.StereoTypes

        /**
         * Selection of surprised [Kaomoji]
         */
        public val Surprised: koodies.kaomoji.categories.Surprised = koodies.kaomoji.categories.Surprised

        /**
         * Selection of sweating [Kaomoji]
         */
        public val Sweating: koodies.kaomoji.categories.Sweating = koodies.kaomoji.categories.Sweating

        /**
         * Selection of table flipping [Kaomoji]
         */
        public val TableFlipping: koodies.kaomoji.categories.TableFlipping = koodies.kaomoji.categories.TableFlipping

        /**
         * Selection of take abow [Kaomoji]
         */
        public val TakeABow: koodies.kaomoji.categories.TakeABow = koodies.kaomoji.categories.TakeABow

        /**
         * Selection of thats it [Kaomoji]
         */
        public val ThatsIt: koodies.kaomoji.categories.ThatsIt = koodies.kaomoji.categories.ThatsIt

        /**
         * Selection of thumbs up [Kaomoji]
         */
        public val ThumbsUp: koodies.kaomoji.categories.ThumbsUp = koodies.kaomoji.categories.ThumbsUp

        /**
         * Selection of tired [Kaomoji]
         */
        public val Tired: koodies.kaomoji.categories.Tired = koodies.kaomoji.categories.Tired

        /**
         * Selection of trembling [Kaomoji]
         */
        public val Trembling: koodies.kaomoji.categories.Trembling = koodies.kaomoji.categories.Trembling

        /**
         * Selection of try my best [Kaomoji]
         */
        public val TryMyBest: koodies.kaomoji.categories.TryMyBest = koodies.kaomoji.categories.TryMyBest

        /**
         * Selection of TV-related [Kaomoji]
         */
        public val TV: koodies.kaomoji.categories.TV = koodies.kaomoji.categories.TV

        /**
         * Selection of unicode [Kaomoji]
         */
        public val Unicode: koodies.kaomoji.categories.Unicode = koodies.kaomoji.categories.Unicode

        /**
         * Selection of upset [Kaomoji]
         */
        public val Upset: koodies.kaomoji.categories.Upset = koodies.kaomoji.categories.Upset

        /**
         * Selection of vomitting [Kaomoji]
         */
        public val Vomitting: koodies.kaomoji.categories.Vomitting = koodies.kaomoji.categories.Vomitting

        /**
         * Selection of weapon-related [Kaomoji]
         */
        public val Weapons: koodies.kaomoji.categories.Weapons = koodies.kaomoji.categories.Weapons

        /**
         * Selection of weird [Kaomoji]
         */
        public val Weird: koodies.kaomoji.categories.Weird = koodies.kaomoji.categories.Weird

        /**
         * Selection of whale [Kaomoji]
         */
        public val Whales: koodies.kaomoji.categories.Whales = koodies.kaomoji.categories.Whales

        /**
         * Selection of why [Kaomoji]
         */
        public val Why: koodies.kaomoji.categories.Why = koodies.kaomoji.categories.Why

        /**
         * Selection of winking [Kaomoji]
         */
        public val Winking: koodies.kaomoji.categories.Winking = koodies.kaomoji.categories.Winking

        /**
         * Selection of wizard [Kaomoji]
         */
        public val Wizards: koodies.kaomoji.categories.Wizards = koodies.kaomoji.categories.Wizards

        /**
         * Selection of writing [Kaomoji]
         */
        public val Writing: koodies.kaomoji.categories.Writing = koodies.kaomoji.categories.Writing
    }
}
