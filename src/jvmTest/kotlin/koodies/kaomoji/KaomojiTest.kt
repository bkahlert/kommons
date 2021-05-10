package koodies.kaomoji

import koodies.runtime.isIntelliJ
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isContainedIn
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.startsWith

class KaomojiTest {

    @Nested
    inner class Fishing {

        @Test
        fun `should be created with random fisher and specified fish`() {
            expectThat(Kaomoji.random().fishing(Kaomoji.Fish.`❮°«⠶＞˝`)).endsWith("o/￣￣￣❮°«⠶＞˝")
        }

        @Test
        fun `should be created with specified fisher and random fish`() {
            expectThat(Kaomoji.Shrugging.first().fishing()).startsWith("┐(´д｀)o/￣￣￣")
        }
    }

    @Nested
    inner class Thinking {

        private val kaomoji = Kaomoji.Bears.first()
        private val left0 = if (isIntelliJ) "    " else kaomoji.ansi.hidden

        @TestFactory
        fun `should render empty`() = testEach(null, "", "   ") {
            expecting { kaomoji.thinking(it) } that {
                isEqualTo("""
                $left0   ͚͔˱ ❨ ( … )
                ・㉨・ ˙
            """.trimIndent())
            }
        }

        @Test
        fun `should render single line`() {
            expectThat(kaomoji.thinking("oh no")).isEqualTo("""
                $left0   ͚͔˱ ❨ ( oh no )
                ・㉨・ ˙
            """.trimIndent())
        }

        @Test
        fun `should render two lines`() {
            expectThat(kaomoji.thinking("oh no 1${LF}oh no 2")).isEqualTo("""
                $left0       ⎛ oh no 1 ⎞
                $left0   ͚͔˱ ❨ ⎝ oh no 2 ⎠
                ・㉨・ ˙
            """.trimIndent())
        }

        @Test
        fun `should render multi line`() {
            expectThat(kaomoji.thinking("oh no 1${LF}oh no 2${LF}oh no 3")).isEqualTo("""
                $left0       ⎛ oh no 1 ⎞
                $left0       ⎜ oh no 2 ⎟
                $left0   ͚͔˱ ❨ ⎝ oh no 3 ⎠
                ・㉨・ ˙
            """.trimIndent())
        }

        @Test
        fun `should render lines of different length`() {
            expectThat(kaomoji.thinking("123${LF}${LF}1234567890")).isEqualTo("""
                $left0       ⎛ 123        ⎞
                $left0       ⎜            ⎟
                $left0   ͚͔˱ ❨ ⎝ 1234567890 ⎠
                ・㉨・ ˙
            """.trimIndent())
        }
    }

    @Nested
    inner class CompanionObject {

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
