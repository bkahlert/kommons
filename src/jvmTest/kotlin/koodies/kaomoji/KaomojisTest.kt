package koodies.kaomoji

import koodies.kaomoji.Kaomojis.fishing
import koodies.kaomoji.Kaomojis.thinking
import koodies.runtime.isIntelliJ
import koodies.terminal.AnsiFormats.hidden
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.asCodePointSequence
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.startsWith

@Execution(SAME_THREAD)
class KaomojisTest {

    @RepeatedTest(10)
    fun `should create random Kaomoji`() {
        val kaomoji = Kaomojis.random()
        expectThat(kaomoji).get { asCodePointSequence().count() }.isGreaterThanOrEqualTo(3)
    }

    @TestFactory
    fun `should create random Kaomoji from`() =
        Kaomojis.Generator.values().testEach("{}") { category ->
            val kaomoji = category.random()
            expect { kaomoji.asCodePointSequence().count() }.that { isGreaterThanOrEqualTo(3) }
        }

    @RepeatedTest(10)
    fun `should create random dogs`() = (0 until 10).map {
        val kaomoji = Kaomojis.Dogs.random()
        expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
    }

    @RepeatedTest(10)
    fun `should create random wizards`() = (0 until 10).map {
        val kaomoji = Kaomojis.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
    }

    @Nested
    inner class RandomThinkingKaomoji {
        @Test
        fun `should create thinking Kaomoji`() {
            val hidden = if (isIntelliJ) "    " else "・㉨・".ansi.hidden
            expectThat(Kaomojis.Bear[0].thinking("oh no")).isEqualTo("""
                $hidden   ͚͔˱ ❨ ( oh no )
                ・㉨・ ˙
            """.trimIndent())
        }
    }

    @Nested
    inner class RandomFishingKaomoji {
        @Test
        fun `should be created with random fisher and specified fish`() {
            expectThat(fishing(Kaomojis.Fish.`❮°«⠶＞˝`)).endsWith("o/￣￣￣❮°«⠶＞˝")
        }

        @Test
        fun `should be created with specified fisher and random fish`() {
            expectThat(Kaomojis.Shrug[0].fishing()).startsWith("┐(´д｀)o/￣￣￣")
        }
    }

    @Nested
    inner class Categories {

        @Test
        fun `should use manually specified form`() {
            val kaomoji = Kaomojis.Angry.`(`A´)`
            expectThat(kaomoji).toStringIsEqualTo("(`A´)")
        }

        @Test
        fun `should parse automatically`() {
            val kaomoji = Kaomojis.Angry.`(`A´)`
            expectThat(kaomoji) {
                get("left arm") { leftArm }.isEqualTo("(")
                get("right arm") { rightArm }.isEqualTo(")")
                get("left eye") { leftEye }.isEqualTo("`")
                get("right eye") { rightEye }.isEqualTo("´")
                get("mouth") { mouth }.isEqualTo("A")
            }
        }

        @Test
        fun `should be enumerable`() {
            expectThat(Kaomojis.Angry.subList(2, 5).joinToString { "$it" })
                .isEqualTo("눈_눈, ಠ⌣ಠ, ಠ▃ಠ")
        }
    }
}
