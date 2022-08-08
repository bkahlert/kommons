package com.bkahlert.kommons.kaomoji

import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.columns
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Isolated
import java.util.stream.Stream

@Isolated
class KaomojiTest {

    @Test fun fishing() = testAll {
        Kaomoji.random().fishing(Kaomoji.Fish.`❮°«⠶＞˝`) shouldEndWith "o/￣￣￣❮°«⠶＞˝"
        Kaomoji.Shrugging.first().fishing() shouldStartWith "┐(´д｀)o/￣￣￣"
    }

    @Nested
    inner class Thinking {

        private val kaomoji = Kaomoji.Bears.first()
        private val blank = " ".repeat(kaomoji.columns)

        @TestFactory fun `should render empty`() = testEach(null, "", "   ") {
            kaomoji.thinking(it) shouldBe """
                    $blank  ̣ ˱ ❨ ( … )
                    ・㉨・
                """.trimIndent()
        }

        @Test fun `should render single line`() = testAll {
            kaomoji.thinking("oh no") shouldBe """
                    $blank  ̣ ˱ ❨ ( oh no )
                    ・㉨・
                """.trimIndent()
        }

        @Test fun `should render two lines`() = testAll {
            kaomoji.thinking("oh no 1${LF}oh no 2") shouldBe """
                    $blank       ⎛ oh no 1 ⎞
                    $blank  ̣ ˱ ❨ ⎝ oh no 2 ⎠
                    ・㉨・
                """.trimIndent()
        }

        @Test fun `should render multi line`() = testAll {
            kaomoji.thinking("oh no 1${LF}oh no 2${LF}oh no 3") shouldBe """
                    $blank       ⎛ oh no 1 ⎞
                    $blank       ⎜ oh no 2 ⎟
                    $blank  ̣ ˱ ❨ ⎝ oh no 3 ⎠
                    ・㉨・
                """.trimIndent()
        }

        @Test fun `should render lines of different length`() = testAll {
            kaomoji.thinking("123${LF}${LF}1234567890${LF}1234") shouldBe """
                    $blank       ⎛ 123        ⎞
                    $blank       ⎜            ⎟
                    $blank       ⎜ 1234567890 ⎟
                    $blank  ̣ ˱ ❨ ⎝ 1234       ⎠
                    ・㉨・
                """.trimIndent()
        }

        @AnsiRequiring @Test
        fun `should render ANSI`() = testAll {
            kaomoji.thinking("${"123".ansi.brightBlue}${LF}${"".ansi.yellow.bold}${LF}1234567890${LF}1234".ansi.underline.done) shouldBe """
                    $blank       ⎛ [4m[94m123[24;39m        ⎞
                    $blank       ⎜ [4m[24m           ⎟
                    $blank       ⎜ [4m1234567890[24m ⎟
                    $blank  ̣ ˱ ❨ ⎝ [4m1234[24m       ⎠
                    ・㉨・
                """.trimIndent()
        }
    }

    @TestFactory fun `should have categories`() = testEach(
        Kaomoji.Angry,
        Kaomoji.Babies, Kaomoji.BadMood, Kaomoji.Bears, Kaomoji.Begging, Kaomoji.Blushing,
        Kaomoji.Cats, Kaomoji.Celebrities, Kaomoji.Chasing, Kaomoji.Confused, Kaomoji.Crying, Kaomoji.Cute,
        Kaomoji.Dancing, Kaomoji.Depressed, Kaomoji.Devils, Kaomoji.Disappointed, Kaomoji.Dog, Kaomoji.Drooling,
        Kaomoji.Eating, Kaomoji.Evil, Kaomoji.Excited,
        Kaomoji.FallingDown, Kaomoji.Feces, Kaomoji.Feminine, Kaomoji.Fish, Kaomoji.Fishing, Kaomoji.Flower, Kaomoji.Funny,
        Kaomoji.Geek, Kaomoji.Glasses, Kaomoji.Greeting, Kaomoji.Grinning, Kaomoji.Gross,
        Kaomoji.Happy, Kaomoji.Helpless, Kaomoji.Heroes, Kaomoji.Hide, Kaomoji.Hugging,
        Kaomoji.Kissing,
        Kaomoji.Laughing, Kaomoji.LennyFace, Kaomoji.Love,
        Kaomoji.MakeUpMyMind, Kaomoji.MiddleFinger, Kaomoji.Money, Kaomoji.Monkey, Kaomoji.Musical,
        Kaomoji.Nervious,
        Kaomoji.PeaceSign, Kaomoji.Pointing, Kaomoji.Proud, Kaomoji.Punching,
        Kaomoji.Rabbits, Kaomoji.Rain, Kaomoji.RogerThat, Kaomoji.RollOver, Kaomoji.Running,
        Kaomoji.Sad, Kaomoji.Salute, Kaomoji.Scared, Kaomoji.Screaming, Kaomoji.Sheep, Kaomoji.Shocked, Kaomoji.Shrugging, Kaomoji.Shy, Kaomoji.Sleeping,
        Kaomoji.Smiling, Kaomoji.Smoking, Kaomoji.Sparkling, Kaomoji.Spinning, Kaomoji.StereoTypes, Kaomoji.Surprised, Kaomoji.Sweating,
        Kaomoji.TableFlipping, Kaomoji.TakeABow, Kaomoji.ThatsIt, Kaomoji.ThumbsUp, Kaomoji.Tired, Kaomoji.Trembling, Kaomoji.TryMyBest, Kaomoji.TV,
        Kaomoji.Eyes, Kaomoji.Upset,
        Kaomoji.Vomitting,
        Kaomoji.Weapons, Kaomoji.Weird, Kaomoji.Whales, Kaomoji.Why, Kaomoji.Winking, Kaomoji.Wizards, Kaomoji.Writing,
    ) { category ->
        category.random().shouldBeInstanceOf<Kaomoji>()
    }

    @Nested
    inner class CompanionObject {

        @Test fun empty() = testAll {
            Kaomoji.EMPTY.toString().shouldBeEmpty()
            Kaomoji.Shrugging.first().fishing(Kaomoji.EMPTY).toString() shouldBe "┐(´д｀)o/￣￣￣"
        }

        @Test fun random() = testAll {
            Kaomoji.random().toString().shouldNotBeEmpty()
        }

        @TestFactory
        fun `should create random Kaomoji from Generator category`(): Stream<DynamicTest> = testEach(*Generator.values()) { category ->
            category.random().toString().shouldNotBeEmpty()
        }

        @Nested
        inner class Categories {

            @Test
            fun `should use manually specified form`() {
                Kaomoji.Angry.`-`д´-`.toString() shouldBe "-`д´-"
            }

            @Test
            fun `should parse automatically`() {
                Kaomoji.Angry.`-`д´-` should {
                    withClue("left arm") { it.leftArm shouldBe "-" }
                    withClue("right arm") { it.rightArm shouldBe "-" }
                    withClue("left eye") { it.leftEye shouldBe "`" }
                    withClue("right eye") { it.rightEye shouldBe "´" }
                    withClue("mouth") { it.mouth shouldBe "д" }
                }
            }

            @Test
            fun `should be enumerable`() {
                Kaomoji.Angry.subList(2, 5).joinToString { "$it" } shouldBe "눈_눈, ಠ⌣ಠ, ಠ▃ಠ"
            }

            @Test
            fun `should pick random from specified kaomoji`() {
                Kaomoji.random(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()) should {
                    listOf(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()).shouldContain(it)
                }
            }

            @Test
            fun `should pick random from specified category`() {
                Kaomoji.Angry.random().toString().shouldNotBeEmpty()
            }

            @Test
            fun `should pick random from specified categories`() {
                Kaomoji.random(Kaomoji.Chasing, Kaomoji.Screaming) should {
                    listOf(*Kaomoji.Chasing.toTypedArray(), *Kaomoji.Screaming.toTypedArray()).shouldContain(it)
                }
            }
        }
    }
}
