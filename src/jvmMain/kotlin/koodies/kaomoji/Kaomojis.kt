@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")

package koodies.kaomoji

import koodies.kaomoji.Kaomojis.Generator.Companion.removeRightArm
import koodies.terminal.ANSI
import koodies.terminal.AnsiFormats.hidden
import koodies.terminal.colorize
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

object Kaomojis {
    @Suppress("unused")
    enum class Generator(
        val leftArm: List<String>,
        val rightArm: List<String>,
        val leftEye: List<String>,
        val rightEye: List<String>,
        val mouth: List<String>,
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

        fun random(
            fixedLeftArm: String = leftArm.random(),
            fixedLeftEye: String = leftEye.random(),
            fixedMouth: String = mouth.random(),
            fixedRightEye: String = rightEye.random(),
            fixedRightArm: String = rightArm.random(),
        ): String = "$fixedLeftArm$fixedLeftEye$fixedMouth$fixedRightEye$fixedRightArm"

        companion object {
            val leftArms: List<String> = values().flatMap { it.leftArm }
            val rightArms: List<String> = values().flatMap { it.rightArm }
            val leftEyes: List<String> = values().flatMap { it.leftEye }
            val rightEyes: List<String> = values().flatMap { it.rightEye }
            val mouths: List<String> = values().flatMap { it.mouth }

            fun CharSequence.removeRightArm(): CharSequence {
                val rightArm = rightArms.dropWhile { !this.endsWith(it) }
                return if (rightArm.isNotEmpty()) this.removeSuffix(rightArm.first()) else this
            }
        }
    }

    fun random(): String = with(Generator) { listOf(leftArms, leftEyes, mouths, rightEyes, rightArms).joinToString("") { it.random() } }

    data class Kaomoji(
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

        fun random(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(leftArmRange, leftEyeRange, mouthRange, rightEyeRange, rightArmRange)
            return listOfNotNull.foldRight(template) { intRange, acc ->
                acc.substring(0 until intRange.first) + Generator.mouths.random() + acc.subSequence(intRange.last, acc.lastIndex)
            }
        }

        fun withMagic(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(wandRange)
            return listOfNotNull.fold(template) { acc, intRange ->
                acc.substring(0 until intRange.first) + ANSI.termColors.colorize(acc.substring(intRange)) + acc.subSequence(intRange.last, acc.lastIndex + 1)
            }
        }

        /**
         * Returns a fishing [Kaomoji] of the form:
         *
         * ```
         * （♯▼皿▼）o/￣￣￣<゜)))彡
         * ```
         */
        fun fishing(fish: Kaomoji = (Fish + Whales).random()): String {
            val fishingRod = "/￣￣￣"
            val fishingArm = "o"
            val notFishingKaomoji = this.removeSuffix(fishingRod)
            val armLessFisher = notFishingKaomoji.removeRightArm()
            return "$armLessFisher$fishingArm$fishingRod$fish"
        }

        operator fun getValue(kaomojis: Any, property: KProperty<*>): Kaomoji = this
        override val length: Int get() = toString().length
        override fun get(index: Int): Char = toString().get(index)
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
        override fun toString(): String = wandRange?.let { copy(template = withMagic()).random() } ?: template
    }

    @Suppress("KDocMissingDocumentation", "ObjectPropertyName", "NonAsciiCharacters")
    val `(＃￣_￣)o︠・━・・━・━━・━☆`: Kaomoji by five(mouth = 3..4, rightArm = 6..7, wand = 8..16)

    fun five(
        leftArm: IntRange? = null, rightArm: IntRange? = null, leftEye: IntRange? = null, rightEye: IntRange? = null, mouth: IntRange? = null,
        wand: IntRange? = null,
    ): PropertyDelegateProvider<Any, Kaomoji> =
        PropertyDelegateProvider { _, property -> Kaomoji(property.name, leftArm, leftEye, rightEye, rightArm, mouth, wand) }


    @Suppress("unused")
    val Wizards: List<CharSequence> = listOf(
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
    fun CharSequence.thinking(value: String): String {
        val kaomoji = this
        val thinkLine = "${kaomoji.hidden()}   ͚͔˱ ❨ ( $value )"
        return "$thinkLine\n$kaomoji ˙"
    }

    @Suppress("KDocMissingDocumentation", "unused")
    val Angry = koodies.kaomoji.categories.Angry

    @Suppress("KDocMissingDocumentation", "unused")
    val BadMood = koodies.kaomoji.categories.BadMood

    @Suppress("KDocMissingDocumentation", "unused")
    val Bear = koodies.kaomoji.categories.Bear

    @Suppress("KDocMissingDocumentation", "unused")
    val Beg = koodies.kaomoji.categories.Beg

    @Suppress("KDocMissingDocumentation", "unused")
    val Blush = koodies.kaomoji.categories.Blush

    @Suppress("KDocMissingDocumentation", "unused")
    val Cat = koodies.kaomoji.categories.Cat

    @Suppress("KDocMissingDocumentation", "unused")
    val Confused = koodies.kaomoji.categories.Confused

    @Suppress("KDocMissingDocumentation", "unused")
    val Cry = koodies.kaomoji.categories.Cry

    @Suppress("KDocMissingDocumentation", "unused")
    val Cute = koodies.kaomoji.categories.Cute

    @Suppress("KDocMissingDocumentation", "unused")
    val Dance = koodies.kaomoji.categories.Dance

    @Suppress("KDocMissingDocumentation", "unused")
    val Depressed = koodies.kaomoji.categories.Depressed

    @Suppress("KDocMissingDocumentation", "unused")
    val Devil = koodies.kaomoji.categories.Devil

    @Suppress("KDocMissingDocumentation", "unused")
    val Disappointed = koodies.kaomoji.categories.Disappointed

    @Suppress("KDocMissingDocumentation", "unused")
    val Drool = koodies.kaomoji.categories.Drool

    @Suppress("KDocMissingDocumentation", "unused")
    val Eat = koodies.kaomoji.categories.Eat

    @Suppress("KDocMissingDocumentation", "unused")
    val Evil = koodies.kaomoji.categories.Evil

    @Suppress("KDocMissingDocumentation", "unused")
    val Excited = koodies.kaomoji.categories.Excited

    @Suppress("KDocMissingDocumentation", "unused")
    val FallDown = koodies.kaomoji.categories.FallDown

    @Suppress("KDocMissingDocumentation", "unused")
    val Feces = koodies.kaomoji.categories.Feces

    @Suppress("KDocMissingDocumentation", "unused")
    val Feminine = koodies.kaomoji.categories.Feminine

    @Suppress("KDocMissingDocumentation", "unused")
    val FlipTable = koodies.kaomoji.categories.FlipTable

    @Suppress("KDocMissingDocumentation", "unused")
    val Flower = koodies.kaomoji.categories.Flower

    @Suppress("KDocMissingDocumentation", "unused")
    val Funny = koodies.kaomoji.categories.Funny

    @Suppress("KDocMissingDocumentation", "unused")
    val Glasses = koodies.kaomoji.categories.Glasses

    @Suppress("KDocMissingDocumentation", "unused")
    val Grin = koodies.kaomoji.categories.Grin

    @Suppress("KDocMissingDocumentation", "unused")
    val Gross = koodies.kaomoji.categories.Gross

    @Suppress("KDocMissingDocumentation", "unused")
    val Happy = koodies.kaomoji.categories.Happy

    @Suppress("KDocMissingDocumentation", "unused")
    val Heart = koodies.kaomoji.categories.Heart

    @Suppress("KDocMissingDocumentation", "unused")
    val Hello = koodies.kaomoji.categories.Hello

    @Suppress("KDocMissingDocumentation", "unused")
    val Helpless = koodies.kaomoji.categories.Helpless

    @Suppress("KDocMissingDocumentation", "unused")
    val Hide = koodies.kaomoji.categories.Hide

    @Suppress("KDocMissingDocumentation", "unused")
    val Hug = koodies.kaomoji.categories.Hug

    @Suppress("KDocMissingDocumentation", "unused")
    val Kiss = koodies.kaomoji.categories.Kiss

    @Suppress("KDocMissingDocumentation", "unused")
    val Laugh = koodies.kaomoji.categories.Laugh

    @Suppress("KDocMissingDocumentation", "unused")
    val LennyFace = koodies.kaomoji.categories.LennyFace

    @Suppress("KDocMissingDocumentation", "unused")
    val Love = koodies.kaomoji.categories.Love

    @Suppress("KDocMissingDocumentation", "unused")
    val Magic = koodies.kaomoji.categories.Magic

    @Suppress("KDocMissingDocumentation", "unused")
    val MakeUpMyMind = koodies.kaomoji.categories.MakeUpMyMind

    @Suppress("KDocMissingDocumentation", "unused")
    val MiddleFinger = koodies.kaomoji.categories.MiddleFinger

    @Suppress("KDocMissingDocumentation", "unused")
    val Monkey = koodies.kaomoji.categories.Monkey

    @Suppress("KDocMissingDocumentation", "unused")
    val Music = koodies.kaomoji.categories.Music

    @Suppress("KDocMissingDocumentation", "unused")
    val Nervious = koodies.kaomoji.categories.Nervious

    @Suppress("KDocMissingDocumentation", "unused")
    val PeaceSign = koodies.kaomoji.categories.PeaceSign

    @Suppress("KDocMissingDocumentation", "unused")
    val Proud = koodies.kaomoji.categories.Proud

    @Suppress("KDocMissingDocumentation", "unused")
    val Punch = koodies.kaomoji.categories.Punch

    @Suppress("KDocMissingDocumentation", "unused")
    val Rabbit = koodies.kaomoji.categories.Rabbit

    @Suppress("KDocMissingDocumentation", "unused")
    val RogerThat = koodies.kaomoji.categories.RogerThat

    @Suppress("KDocMissingDocumentation", "unused")
    val RollOver = koodies.kaomoji.categories.RollOver

    @Suppress("KDocMissingDocumentation", "unused")
    val Run = koodies.kaomoji.categories.Run

    @Suppress("KDocMissingDocumentation", "unused")
    val Sad = koodies.kaomoji.categories.Sad

    @Suppress("KDocMissingDocumentation", "unused")
    val Salute = koodies.kaomoji.categories.Salute

    @Suppress("KDocMissingDocumentation", "unused")
    val Scared = koodies.kaomoji.categories.Scared

    @Suppress("KDocMissingDocumentation", "unused")
    val Sheep = koodies.kaomoji.categories.Sheep

    @Suppress("KDocMissingDocumentation", "unused")
    val Shocked = koodies.kaomoji.categories.Shocked

    @Suppress("KDocMissingDocumentation", "unused")
    val Shrug = koodies.kaomoji.categories.Shrug

    @Suppress("KDocMissingDocumentation", "unused")
    val Shy = koodies.kaomoji.categories.Shy

    @Suppress("KDocMissingDocumentation", "unused")
    val Sleep = koodies.kaomoji.categories.Sleep

    @Suppress("KDocMissingDocumentation", "unused")
    val Smile = koodies.kaomoji.categories.Smile

    @Suppress("KDocMissingDocumentation", "unused")
    val Sparkle = koodies.kaomoji.categories.Sparkle

    @Suppress("KDocMissingDocumentation", "unused")
    val Spin = koodies.kaomoji.categories.Spin

    @Suppress("KDocMissingDocumentation", "unused")
    val Surprised = koodies.kaomoji.categories.Surprised

    @Suppress("KDocMissingDocumentation", "unused")
    val Sweat = koodies.kaomoji.categories.Sweat

    @Suppress("KDocMissingDocumentation", "unused")
    val TakeABow = koodies.kaomoji.categories.TakeABow

    @Suppress("KDocMissingDocumentation", "unused")
    val ThatsIt = koodies.kaomoji.categories.ThatsIt

    @Suppress("KDocMissingDocumentation", "unused")
    val ThumbsUp = koodies.kaomoji.categories.ThumbsUp

    @Suppress("KDocMissingDocumentation", "unused")
    val Tired = koodies.kaomoji.categories.Tired

    @Suppress("KDocMissingDocumentation", "unused")
    val Tremble = koodies.kaomoji.categories.Tremble

    @Suppress("KDocMissingDocumentation", "unused")
    val TryMyBest = koodies.kaomoji.categories.TryMyBest

    @Suppress("KDocMissingDocumentation", "unused")
    val Unicode = koodies.kaomoji.categories.Unicode

    @Suppress("KDocMissingDocumentation", "unused")
    val Upset = koodies.kaomoji.categories.Upset

    @Suppress("KDocMissingDocumentation", "unused")
    val Vomit = koodies.kaomoji.categories.Vomit

    @Suppress("KDocMissingDocumentation", "unused")
    val Weird = koodies.kaomoji.categories.Weird

    @Suppress("KDocMissingDocumentation", "unused")
    val Wink = koodies.kaomoji.categories.Wink

    @Suppress("KDocMissingDocumentation", "unused")
    val Writing = koodies.kaomoji.categories.Writing

    @Suppress("KDocMissingDocumentation", "unused")
    val Smoking = koodies.kaomoji.categories.Smoking

    @Suppress("KDocMissingDocumentation", "unused")
    val Rain = koodies.kaomoji.categories.Rain

    @Suppress("KDocMissingDocumentation", "unused")
    val TV = koodies.kaomoji.categories.TV

    @Suppress("KDocMissingDocumentation", "unused")
    val Fishing = koodies.kaomoji.categories.Fishing

    @Suppress("KDocMissingDocumentation", "unused")
    val Fish = koodies.kaomoji.categories.Fish

    @Suppress("KDocMissingDocumentation", "unused")
    val Whales = koodies.kaomoji.categories.Whales

    @Suppress("KDocMissingDocumentation", "unused")
    val Weapons = koodies.kaomoji.categories.Weapons

    @Suppress("KDocMissingDocumentation", "unused")
    val Babies = koodies.kaomoji.categories.Babies

    @Suppress("KDocMissingDocumentation", "unused")
    val Money = koodies.kaomoji.categories.Money

    @Suppress("KDocMissingDocumentation", "unused")
    val Screaming = koodies.kaomoji.categories.Screaming

    @Suppress("KDocMissingDocumentation", "unused")
    val Why = koodies.kaomoji.categories.Why

    @Suppress("KDocMissingDocumentation", "unused")
    val Geeks = koodies.kaomoji.categories.Geeks

    @Suppress("KDocMissingDocumentation", "unused")
    val Pointing = koodies.kaomoji.categories.Pointing

    @Suppress("KDocMissingDocumentation", "unused")
    val Chasing = koodies.kaomoji.categories.Chasing

    @Suppress("KDocMissingDocumentation", "unused")
    val Celebrities = koodies.kaomoji.categories.Celebrities

    @Suppress("KDocMissingDocumentation", "unused")
    val Heroes = koodies.kaomoji.categories.Heroes

    @Suppress("KDocMissingDocumentation", "unused")
    val Dogs = koodies.kaomoji.categories.Dogs

    @Suppress("KDocMissingDocumentation", "unused")
    val StereoTypes = koodies.kaomoji.categories.StereoTypes

    /**
     * Returns a random fishing [Kaomoji] of the form:
     *
     * ```
     * （♯▼皿▼）o/￣￣￣<゜)))彡
     * ```
     */
    fun fishing(fish: Kaomoji = (Fish + Whales).random()): String =
        Fishing.random().fishing(fish)
}
