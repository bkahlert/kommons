package koodies.kaomoji

import koodies.test.AnsiRequired
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import koodies.text.columns
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isContainedIn
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.startsWith

@Isolated
class KaomojiTest {

    @Nested
    inner class Fishing {

        @Test
        fun `should be created with random fisher and specified fish`() {
            expecting { Kaomoji.random().fishing(Kaomoji.Fish.`❮°«⠶＞˝`) } that { endsWith("o/￣￣￣❮°«⠶＞˝") }
        }

        @Test
        fun `should be created with specified fisher and random fish`() {
            expecting { Kaomoji.Shrugging.first().fishing() } that { startsWith("┐(´д｀)o/￣￣￣") }
        }
    }

    @Nested
    inner class Thinking {

        private val kaomoji = Kaomoji.Bears.first()
        private val blank = " ".repeat(kaomoji.toString().columns)

        @TestFactory
        fun `should render empty`() = testEach(null, "", "   ") {
            expecting { kaomoji.thinking(it) } that {
                isEqualTo("""
                    $blank  ̣ ˱ ❨ ( … )
                    ・㉨・
                """.trimIndent())
            }
        }

        @Test
        fun `should render single line`() {
            expecting { kaomoji.thinking("oh no") } that {
                isEqualTo("""
                    $blank  ̣ ˱ ❨ ( oh no )
                    ・㉨・
                """.trimIndent())
            }
        }

        @Test
        fun `should render two lines`() {
            expecting { kaomoji.thinking("oh no 1${LF}oh no 2") } that {
                isEqualTo("""
                    $blank       ⎛ oh no 1 ⎞
                    $blank  ̣ ˱ ❨ ⎝ oh no 2 ⎠
                    ・㉨・
                """.trimIndent())
            }
        }

        @Test
        fun `should render multi line`() {
            expecting { kaomoji.thinking("oh no 1${LF}oh no 2${LF}oh no 3") } that {
                isEqualTo("""
                    $blank       ⎛ oh no 1 ⎞
                    $blank       ⎜ oh no 2 ⎟
                    $blank  ̣ ˱ ❨ ⎝ oh no 3 ⎠
                    ・㉨・
                """.trimIndent())
            }
        }

        @Test
        fun `should render lines of different length`() {
            expecting { kaomoji.thinking("123${LF}${LF}1234567890${LF}1234") } that {
                isEqualTo("""
                    $blank       ⎛ 123        ⎞
                    $blank       ⎜            ⎟
                    $blank       ⎜ 1234567890 ⎟
                    $blank  ̣ ˱ ❨ ⎝ 1234       ⎠
                    ・㉨・
                """.trimIndent())
            }
        }

        @AnsiRequired @Test
        fun `should render ANSI`() {
            expecting {
                kaomoji.thinking("${"123".ansi.brightBlue}${LF}${"".ansi.yellow.bold}${LF}1234567890${LF}1234".ansi.underline.done)
            } that {
                isEqualTo("""
                                 ⎛ [4m[94m123[24;39m        ⎞
                                 ⎜ [4m[24m           ⎟
                                 ⎜ [4m1234567890[24m ⎟
                            ̣ ˱ ❨ ⎝ [4m1234[24m       ⎠
                    ・㉨・
                """.trimIndent())
            }
        }
    }

    @TestFactory
    fun `should have categories`() = testEach(
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
        expecting { category.random() } that { isA<Kaomoji>() }
    }

    @Nested
    inner class CompanionObject {

        @Nested
        inner class EmptyKaomoji {
            @Test
            fun `should have empty kaomoji`() {
                expectThat(Kaomoji.EMPTY).toStringIsEqualTo("")
            }

            @Test
            fun `should have render as empty string`() {
                expectThat(Kaomoji.Shrugging.first().fishing(Kaomoji.EMPTY)).toStringIsEqualTo("┐(´д｀)o/￣￣￣")
            }
        }

        @TestFactory
        fun `should create random Kaomoji`() = testEach(
            Kaomoji.random(),
            Kaomoji.random(),
            Kaomoji.random(),
        ) { kaomoji ->
            kaomoji asserting { isA<Kaomoji>().isNotEmpty() }
        }

        @TestFactory
        fun `should create random Kaomoji from Generator category`(): List<DynamicContainer> = testEach(*Generator.values()) { category ->
            expecting { category.random() } that { isA<Kaomoji>().isNotEmpty() }
        }

        @Nested
        inner class Categories {

            @Test
            fun `should use manually specified form`() {
                expecting { Kaomoji.Angry.`(`A´)` } that { toStringIsEqualTo("(`A´)") }
            }

            @Test
            fun `should parse automatically`() {
                expecting { Kaomoji.Angry.`(`A´)` } that {
                    get("left arm") { leftArm }.isEqualTo("(")
                    get("right arm") { rightArm }.isEqualTo(")")
                    get("left eye") { leftEye }.isEqualTo("`")
                    get("right eye") { rightEye }.isEqualTo("´")
                    get("mouth") { mouth }.isEqualTo("A")
                }
            }

            @Test
            fun `should be enumerable`() {
                expecting { Kaomoji.Angry.subList(2, 5).joinToString { "$it" } } that { isEqualTo("눈_눈, ಠ⌣ಠ, ಠ▃ಠ") }
            }

            @Test
            fun `should pick random from specified kaomoji`() {
                expecting { Kaomoji.random(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()) } that {
                    isContainedIn(listOf(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()))
                }
            }

            @Test
            fun `should pick random from specified category`() {
                expecting { Kaomoji.Angry.random() } that { isA<Kaomoji>().isNotEmpty() }
            }

            @Test
            fun `should pick random from specified categories`() {
                expecting { Kaomoji.random(Kaomoji.Chasing, Kaomoji.Screaming) } that {
                    isContainedIn(listOf(*Kaomoji.Chasing.toTypedArray(), *Kaomoji.Screaming.toTypedArray()))
                }
            }
        }
    }
}
