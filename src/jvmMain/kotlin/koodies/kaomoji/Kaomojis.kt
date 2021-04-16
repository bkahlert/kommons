@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")

package koodies.kaomoji

import koodies.kaomoji.Kaomojis.Generator.Companion.removeRightArm
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.colorize
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

public object Kaomojis {
    @Suppress("unused")
    public enum class Generator(
        public val leftArm: List<String>,
        public val rightArm: List<String>,
        public val leftEye: List<String>,
        public val rightEye: List<String>,
        public val mouth: List<String>,
    ) {
        INDIFFERENCE(
            leftArm = listOf("ヽ", "┐", "╮", "ᕕ", "¯\\_"),
            rightArm = listOf("ノ", "┌", "╭", "ᕗ", "_/¯"),
            leftEye = listOf("ー", " ´ ", "︶", "￣", "´", " ˘ ", "‘"),
            rightEye = listOf("ー", " ` ", "︶", "￣", "´", " ˘ ", "` "),
            mouth = listOf("_", "ヘ", "～", "д", "▽", "ヮ", "ー", "︿", "､")),
        JOY(
            leftArm = listOf("╰", "＼", "٩", "<"),
            rightArm = listOf("ﾉ", "ノ", "o", "／"),
            leftEye = listOf("▔", "^", "¯", "☆"),
            rightEye = listOf("▔", "^", "¯", "☆"),
            mouth = listOf("▽", "ω", "ヮ", "∀")),
        LOVE(
            leftArm = listOf("", "♡╰", "ヽ", "♡＼", "٩", "❤ "),
            rightArm = listOf("", "ノ", "♡", "╯♡", " ♡", " ❤", "/ ♡", "ノ～ ♡", "۶"),
            leftEye = listOf("─", "´ ", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", "´• ", "≧"),
            rightEye = listOf("─", " `", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", " •`", "≦"),
            mouth = listOf("з", "_", "‿‿", "ω", "︶", "◡", "▽", "ε", "∀", "ᵕ", "‿", "³")),
        SADNESS(
            leftArm = listOf("", "o", ".･ﾟﾟ･", "。゜゜", "｡･ﾟﾟ*", "｡･ﾟ", ".｡･ﾟﾟ･", "｡ﾟ", "･ﾟ･", "｡ﾟ･ "),
            rightArm = listOf("", "o", "･ﾟﾟ･.", " ゜゜。", "*ﾟﾟ･｡", "･｡", "･ﾟﾟ･｡.", "･ﾟ･", "･ﾟ｡"),
            leftEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", ">", "｡•́", "╯"),
            rightEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", "<。", "•̀｡", "<、"),
            mouth = listOf("_", "ヘ", "ω", "﹏", "Д", "︿", "-ω-", "︵", "╭╮", "Ｏ", "><")),
        ;

        public fun random(
            fixedLeftArm: String = leftArm.random(),
            fixedLeftEye: String = leftEye.random(),
            fixedMouth: String = mouth.random(),
            fixedRightEye: String = rightEye.random(),
            fixedRightArm: String = rightArm.random(),
        ): String = "$fixedLeftArm$fixedLeftEye$fixedMouth$fixedRightEye$fixedRightArm"

        public companion object {
            public val leftArms: List<String> = values().flatMap { it.leftArm }
            public val rightArms: List<String> = values().flatMap { it.rightArm }
            public val leftEyes: List<String> = values().flatMap { it.leftEye }
            public val rightEyes: List<String> = values().flatMap { it.rightEye }
            public val mouths: List<String> = values().flatMap { it.mouth }

            public fun CharSequence.removeRightArm(): CharSequence {
                val rightArm = rightArms.dropWhile { !this.endsWith(it) }
                return if (rightArm.isNotEmpty()) this.removeSuffix(rightArm.first()) else this
            }
        }
    }

    public fun random(): String = with(Generator) { listOf(leftArms, leftEyes, mouths, rightEyes, rightArms).joinToString("") { it.random() } }

    public data class Kaomoji(
        private val template: String,
        private val leftArmRange: IntRange? = null,
        private val rightArmRange: IntRange? = null,
        private val leftEyeRange: IntRange? = null,
        private val rightEyeRange: IntRange? = null,
        private val mouthRange: IntRange? = null,
        private val wandRange: IntRange? = null,
    ) : CharSequence {
        val leftArm: CharSequence = leftArmRange?.let { template.subSequence(it) } ?: ""
        val rightArm: CharSequence = rightArmRange?.let { template.subSequence(it) } ?: ""
        val leftEye: CharSequence = leftEyeRange?.let { template.subSequence(it) } ?: ""
        val rightEye: CharSequence = rightEyeRange?.let { template.subSequence(it) } ?: ""
        val mouth: CharSequence = mouthRange?.let { template.subSequence(it) } ?: ""
        val wand: CharSequence = wandRange?.let { template.subSequence(it) } ?: ""

        public fun random(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(leftArmRange, leftEyeRange, mouthRange, rightEyeRange, rightArmRange)
            return listOfNotNull.foldRight(template) { intRange, acc ->
                acc.substring(0 until intRange.first) + Generator.mouths.random() + acc.subSequence(intRange.last, acc.lastIndex)
            }
        }

        public fun withMagic(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(wandRange)
            return listOfNotNull.fold(template) { acc, intRange ->
                acc.substring(0 until intRange.first) + acc.substring(intRange).colorize() + acc.subSequence(intRange.last, acc.lastIndex + 1)
            }
        }

        /**
         * Returns a fishing [Kaomoji] of the form:
         *
         * ```
         * （♯▼皿▼）o/￣￣￣<゜)))彡
         * ```
         */
        public fun fishing(fish: Kaomoji = (Fish + Whales).random()): String {
            val fishingRod = "/￣￣￣"
            val fishingArm = "o"
            val notFishingKaomoji = this.removeSuffix(fishingRod)
            val armLessFisher = notFishingKaomoji.removeRightArm()
            return "$armLessFisher$fishingArm$fishingRod$fish"
        }

        public operator fun getValue(kaomojis: Any, property: KProperty<*>): Kaomoji = this
        override val length: Int get() = toString().length
        override fun get(index: Int): Char = toString().get(index)
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
        override fun toString(): String = wandRange?.let { copy(template = withMagic()).random() } ?: template
    }

    @Suppress("KDocMissingDocumentation", "ObjectPropertyName", "NonAsciiCharacters")
    public val `(＃￣_￣)o︠・━・・━・━━・━☆`: Kaomoji by five(mouth = 3..4, rightArm = 6..7, wand = 8..16)

    public fun five(
        leftArm: IntRange? = null, rightArm: IntRange? = null, leftEye: IntRange? = null, rightEye: IntRange? = null, mouth: IntRange? = null,
        wand: IntRange? = null,
    ): PropertyDelegateProvider<Any, Kaomoji> =
        PropertyDelegateProvider { _, property -> Kaomoji(property.name, leftArm, leftEye, rightEye, rightArm, mouth, wand) }


    @Suppress("unused")
    public val Wizards: List<CharSequence> = listOf(
        "(ﾉ>ω<)ﾉ :｡･:*:･ﾟ’★,｡･:*:･ﾟ’☆",
        `(＃￣_￣)o︠・━・・━・━━・━☆`,
        "(/￣‿￣)/~~☆’.･.･:★’.･.･:☆",
        "(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･:≡( ε:)",
        "(ノ ˘_˘)ノ ζζζ  ζζζ  ζζζ",
        "(ノ°∀°)ノ⌒･*:.｡. .｡.:*･゜ﾟ･*☆",
        "(⊃｡•́‿•̀｡)⊃━✿✿✿✿✿✿",
        "ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚",
    )

    /**
     * Returns a thinking [Kaomoji] of the form:
     *
     * ```
     *           ͚͔˱ ❨ ( something )
     * (^～^) ˙
     * ```
     */
    public fun CharSequence.thinking(value: String): String {
        val kaomoji = this
        val thinkLine = "${kaomoji.ansi.hidden.done}   ͚͔˱ ❨ ( $value )"
        return "$thinkLine\n$kaomoji ˙"
    }

    @Suppress("KDocMissingDocumentation", "unused")
    public val Angry: koodies.kaomoji.categories.Angry = koodies.kaomoji.categories.Angry

    @Suppress("KDocMissingDocumentation", "unused")
    public val BadMood: koodies.kaomoji.categories.BadMood = koodies.kaomoji.categories.BadMood

    @Suppress("KDocMissingDocumentation", "unused")
    public val Bear: koodies.kaomoji.categories.Bear = koodies.kaomoji.categories.Bear

    @Suppress("KDocMissingDocumentation", "unused")
    public val Beg: koodies.kaomoji.categories.Beg = koodies.kaomoji.categories.Beg

    @Suppress("KDocMissingDocumentation", "unused")
    public val Blush: koodies.kaomoji.categories.Blush = koodies.kaomoji.categories.Blush

    @Suppress("KDocMissingDocumentation", "unused")
    public val Cat: koodies.kaomoji.categories.Cat = koodies.kaomoji.categories.Cat

    @Suppress("KDocMissingDocumentation", "unused")
    public val Confused: koodies.kaomoji.categories.Confused = koodies.kaomoji.categories.Confused

    @Suppress("KDocMissingDocumentation", "unused")
    public val Cry: koodies.kaomoji.categories.Cry = koodies.kaomoji.categories.Cry

    @Suppress("KDocMissingDocumentation", "unused")
    public val Cute: koodies.kaomoji.categories.Cute = koodies.kaomoji.categories.Cute

    @Suppress("KDocMissingDocumentation", "unused")
    public val Dance: koodies.kaomoji.categories.Dance = koodies.kaomoji.categories.Dance

    @Suppress("KDocMissingDocumentation", "unused")
    public val Depressed: koodies.kaomoji.categories.Depressed = koodies.kaomoji.categories.Depressed

    @Suppress("KDocMissingDocumentation", "unused")
    public val Devil: koodies.kaomoji.categories.Devil = koodies.kaomoji.categories.Devil

    @Suppress("KDocMissingDocumentation", "unused")
    public val Disappointed: koodies.kaomoji.categories.Disappointed = koodies.kaomoji.categories.Disappointed

    @Suppress("KDocMissingDocumentation", "unused")
    public val Drool: koodies.kaomoji.categories.Drool = koodies.kaomoji.categories.Drool

    @Suppress("KDocMissingDocumentation", "unused")
    public val Eat: koodies.kaomoji.categories.Eat = koodies.kaomoji.categories.Eat

    @Suppress("KDocMissingDocumentation", "unused")
    public val Evil: koodies.kaomoji.categories.Evil = koodies.kaomoji.categories.Evil

    @Suppress("KDocMissingDocumentation", "unused")
    public val Excited: koodies.kaomoji.categories.Excited = koodies.kaomoji.categories.Excited

    @Suppress("KDocMissingDocumentation", "unused")
    public val FallDown: koodies.kaomoji.categories.FallDown = koodies.kaomoji.categories.FallDown

    @Suppress("KDocMissingDocumentation", "unused")
    public val Feces: koodies.kaomoji.categories.Feces = koodies.kaomoji.categories.Feces

    @Suppress("KDocMissingDocumentation", "unused")
    public val Feminine: koodies.kaomoji.categories.Feminine = koodies.kaomoji.categories.Feminine

    @Suppress("KDocMissingDocumentation", "unused")
    public val FlipTable: koodies.kaomoji.categories.FlipTable = koodies.kaomoji.categories.FlipTable

    @Suppress("KDocMissingDocumentation", "unused")
    public val Flower: koodies.kaomoji.categories.Flower = koodies.kaomoji.categories.Flower

    @Suppress("KDocMissingDocumentation", "unused")
    public val Funny: koodies.kaomoji.categories.Funny = koodies.kaomoji.categories.Funny

    @Suppress("KDocMissingDocumentation", "unused")
    public val Glasses: koodies.kaomoji.categories.Glasses = koodies.kaomoji.categories.Glasses

    @Suppress("KDocMissingDocumentation", "unused")
    public val Grin: koodies.kaomoji.categories.Grin = koodies.kaomoji.categories.Grin

    @Suppress("KDocMissingDocumentation", "unused")
    public val Gross: koodies.kaomoji.categories.Gross = koodies.kaomoji.categories.Gross

    @Suppress("KDocMissingDocumentation", "unused")
    public val Happy: koodies.kaomoji.categories.Happy = koodies.kaomoji.categories.Happy

    @Suppress("KDocMissingDocumentation", "unused")
    public val Heart: koodies.kaomoji.categories.Heart = koodies.kaomoji.categories.Heart

    @Suppress("KDocMissingDocumentation", "unused")
    public val Hello: koodies.kaomoji.categories.Hello = koodies.kaomoji.categories.Hello

    @Suppress("KDocMissingDocumentation", "unused")
    public val Helpless: koodies.kaomoji.categories.Helpless = koodies.kaomoji.categories.Helpless

    @Suppress("KDocMissingDocumentation", "unused")
    public val Hide: koodies.kaomoji.categories.Hide = koodies.kaomoji.categories.Hide

    @Suppress("KDocMissingDocumentation", "unused")
    public val Hug: koodies.kaomoji.categories.Hug = koodies.kaomoji.categories.Hug

    @Suppress("KDocMissingDocumentation", "unused")
    public val Kiss: koodies.kaomoji.categories.Kiss = koodies.kaomoji.categories.Kiss

    @Suppress("KDocMissingDocumentation", "unused")
    public val Laugh: koodies.kaomoji.categories.Laugh = koodies.kaomoji.categories.Laugh

    @Suppress("KDocMissingDocumentation", "unused")
    public val LennyFace: koodies.kaomoji.categories.LennyFace = koodies.kaomoji.categories.LennyFace

    @Suppress("KDocMissingDocumentation", "unused")
    public val Love: koodies.kaomoji.categories.Love = koodies.kaomoji.categories.Love

    @Suppress("KDocMissingDocumentation", "unused")
    public val Magic: koodies.kaomoji.categories.Magic = koodies.kaomoji.categories.Magic

    @Suppress("KDocMissingDocumentation", "unused")
    public val MakeUpMyMind: koodies.kaomoji.categories.MakeUpMyMind = koodies.kaomoji.categories.MakeUpMyMind

    @Suppress("KDocMissingDocumentation", "unused")
    public val MiddleFinger: koodies.kaomoji.categories.MiddleFinger = koodies.kaomoji.categories.MiddleFinger

    @Suppress("KDocMissingDocumentation", "unused")
    public val Monkey: koodies.kaomoji.categories.Monkey = koodies.kaomoji.categories.Monkey

    @Suppress("KDocMissingDocumentation", "unused")
    public val Music: koodies.kaomoji.categories.Music = koodies.kaomoji.categories.Music

    @Suppress("KDocMissingDocumentation", "unused")
    public val Nervious: koodies.kaomoji.categories.Nervious = koodies.kaomoji.categories.Nervious

    @Suppress("KDocMissingDocumentation", "unused")
    public val PeaceSign: koodies.kaomoji.categories.PeaceSign = koodies.kaomoji.categories.PeaceSign

    @Suppress("KDocMissingDocumentation", "unused")
    public val Proud: koodies.kaomoji.categories.Proud = koodies.kaomoji.categories.Proud

    @Suppress("KDocMissingDocumentation", "unused")
    public val Punch: koodies.kaomoji.categories.Punch = koodies.kaomoji.categories.Punch

    @Suppress("KDocMissingDocumentation", "unused")
    public val Rabbit: koodies.kaomoji.categories.Rabbit = koodies.kaomoji.categories.Rabbit

    @Suppress("KDocMissingDocumentation", "unused")
    public val RogerThat: koodies.kaomoji.categories.RogerThat = koodies.kaomoji.categories.RogerThat

    @Suppress("KDocMissingDocumentation", "unused")
    public val RollOver: koodies.kaomoji.categories.RollOver = koodies.kaomoji.categories.RollOver

    @Suppress("KDocMissingDocumentation", "unused")
    public val Run: koodies.kaomoji.categories.Run = koodies.kaomoji.categories.Run

    @Suppress("KDocMissingDocumentation", "unused")
    public val Sad: koodies.kaomoji.categories.Sad = koodies.kaomoji.categories.Sad

    @Suppress("KDocMissingDocumentation", "unused")
    public val Salute: koodies.kaomoji.categories.Salute = koodies.kaomoji.categories.Salute

    @Suppress("KDocMissingDocumentation", "unused")
    public val Scared: koodies.kaomoji.categories.Scared = koodies.kaomoji.categories.Scared

    @Suppress("KDocMissingDocumentation", "unused")
    public val Sheep: koodies.kaomoji.categories.Sheep = koodies.kaomoji.categories.Sheep

    @Suppress("KDocMissingDocumentation", "unused")
    public val Shocked: koodies.kaomoji.categories.Shocked = koodies.kaomoji.categories.Shocked

    @Suppress("KDocMissingDocumentation", "unused")
    public val Shrug: koodies.kaomoji.categories.Shrug = koodies.kaomoji.categories.Shrug

    @Suppress("KDocMissingDocumentation", "unused")
    public val Shy: koodies.kaomoji.categories.Shy = koodies.kaomoji.categories.Shy

    @Suppress("KDocMissingDocumentation", "unused")
    public val Sleep: koodies.kaomoji.categories.Sleep = koodies.kaomoji.categories.Sleep

    @Suppress("KDocMissingDocumentation", "unused")
    public val Smile: koodies.kaomoji.categories.Smile = koodies.kaomoji.categories.Smile

    @Suppress("KDocMissingDocumentation", "unused")
    public val Sparkle: koodies.kaomoji.categories.Sparkle = koodies.kaomoji.categories.Sparkle

    @Suppress("KDocMissingDocumentation", "unused")
    public val Spin: koodies.kaomoji.categories.Spin = koodies.kaomoji.categories.Spin

    @Suppress("KDocMissingDocumentation", "unused")
    public val Surprised: koodies.kaomoji.categories.Surprised = koodies.kaomoji.categories.Surprised

    @Suppress("KDocMissingDocumentation", "unused")
    public val Sweat: koodies.kaomoji.categories.Sweat = koodies.kaomoji.categories.Sweat

    @Suppress("KDocMissingDocumentation", "unused")
    public val TakeABow: koodies.kaomoji.categories.TakeABow = koodies.kaomoji.categories.TakeABow

    @Suppress("KDocMissingDocumentation", "unused")
    public val ThatsIt: koodies.kaomoji.categories.ThatsIt = koodies.kaomoji.categories.ThatsIt

    @Suppress("KDocMissingDocumentation", "unused")
    public val ThumbsUp: koodies.kaomoji.categories.ThumbsUp = koodies.kaomoji.categories.ThumbsUp

    @Suppress("KDocMissingDocumentation", "unused")
    public val Tired: koodies.kaomoji.categories.Tired = koodies.kaomoji.categories.Tired

    @Suppress("KDocMissingDocumentation", "unused")
    public val Tremble: koodies.kaomoji.categories.Tremble = koodies.kaomoji.categories.Tremble

    @Suppress("KDocMissingDocumentation", "unused")
    public val TryMyBest: koodies.kaomoji.categories.TryMyBest = koodies.kaomoji.categories.TryMyBest

    @Suppress("KDocMissingDocumentation", "unused")
    public val Unicode: koodies.kaomoji.categories.Unicode = koodies.kaomoji.categories.Unicode

    @Suppress("KDocMissingDocumentation", "unused")
    public val Upset: koodies.kaomoji.categories.Upset = koodies.kaomoji.categories.Upset

    @Suppress("KDocMissingDocumentation", "unused")
    public val Vomit: koodies.kaomoji.categories.Vomit = koodies.kaomoji.categories.Vomit

    @Suppress("KDocMissingDocumentation", "unused")
    public val Weird: koodies.kaomoji.categories.Weird = koodies.kaomoji.categories.Weird

    @Suppress("KDocMissingDocumentation", "unused")
    public val Wink: koodies.kaomoji.categories.Wink = koodies.kaomoji.categories.Wink

    @Suppress("KDocMissingDocumentation", "unused")
    public val Writing: koodies.kaomoji.categories.Writing = koodies.kaomoji.categories.Writing

    @Suppress("KDocMissingDocumentation", "unused")
    public val Smoking: koodies.kaomoji.categories.Smoking = koodies.kaomoji.categories.Smoking

    @Suppress("KDocMissingDocumentation", "unused")
    public val Rain: koodies.kaomoji.categories.Rain = koodies.kaomoji.categories.Rain

    @Suppress("KDocMissingDocumentation", "unused")
    public val TV: koodies.kaomoji.categories.TV = koodies.kaomoji.categories.TV

    @Suppress("KDocMissingDocumentation", "unused")
    public val Fishing: koodies.kaomoji.categories.Fishing = koodies.kaomoji.categories.Fishing

    @Suppress("KDocMissingDocumentation", "unused")
    public val Fish: koodies.kaomoji.categories.Fish = koodies.kaomoji.categories.Fish

    @Suppress("KDocMissingDocumentation", "unused")
    public val Whales: koodies.kaomoji.categories.Whales = koodies.kaomoji.categories.Whales

    @Suppress("KDocMissingDocumentation", "unused")
    public val Weapons: koodies.kaomoji.categories.Weapons = koodies.kaomoji.categories.Weapons

    @Suppress("KDocMissingDocumentation", "unused")
    public val Babies: koodies.kaomoji.categories.Babies = koodies.kaomoji.categories.Babies

    @Suppress("KDocMissingDocumentation", "unused")
    public val Money: koodies.kaomoji.categories.Money = koodies.kaomoji.categories.Money

    @Suppress("KDocMissingDocumentation", "unused")
    public val Screaming: koodies.kaomoji.categories.Screaming = koodies.kaomoji.categories.Screaming

    @Suppress("KDocMissingDocumentation", "unused")
    public val Why: koodies.kaomoji.categories.Why = koodies.kaomoji.categories.Why

    @Suppress("KDocMissingDocumentation", "unused")
    public val Geeks: koodies.kaomoji.categories.Geeks = koodies.kaomoji.categories.Geeks

    @Suppress("KDocMissingDocumentation", "unused")
    public val Pointing: koodies.kaomoji.categories.Pointing = koodies.kaomoji.categories.Pointing

    @Suppress("KDocMissingDocumentation", "unused")
    public val Chasing: koodies.kaomoji.categories.Chasing = koodies.kaomoji.categories.Chasing

    @Suppress("KDocMissingDocumentation", "unused")
    public val Celebrities: koodies.kaomoji.categories.Celebrities = koodies.kaomoji.categories.Celebrities

    @Suppress("KDocMissingDocumentation", "unused")
    public val Heroes: koodies.kaomoji.categories.Heroes = koodies.kaomoji.categories.Heroes

    @Suppress("KDocMissingDocumentation", "unused")
    public val Dogs: koodies.kaomoji.categories.Dogs = koodies.kaomoji.categories.Dogs

    @Suppress("KDocMissingDocumentation", "unused")
    public val StereoTypes: koodies.kaomoji.categories.StereoTypes = koodies.kaomoji.categories.StereoTypes

    /**
     * Returns a random fishing [Kaomoji] of the form:
     *
     * ```
     * （♯▼皿▼）o/￣￣￣<゜)))彡
     * ```
     */
    public fun fishing(fish: Kaomoji = (Fish + Whales).random()): String =
        Fishing.random().fishing(fish)
}
