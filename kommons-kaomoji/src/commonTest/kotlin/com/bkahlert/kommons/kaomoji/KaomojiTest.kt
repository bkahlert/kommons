package com.bkahlert.kommons.kaomoji

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators.LF
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class KaomojiTest {

    @Test fun empty() = testAll {
        Kaomoji.EMPTY.toString().shouldBeEmpty()
        Kaomoji.Shrugging.first().fishing(Kaomoji.EMPTY).toString() shouldBe "┐(´д｀)o/￣￣￣"
    }

    @Test fun explicit_specification() = testAll {
        Kaomoji.Angry.`（▽д▽）`.toString() shouldBe "（▽д▽）"
        Kaomoji.Angry.`-`д´-`.toString() shouldBe "-`д´-"
    }

    @Test fun parts() {
        Kaomoji.Angry.`-`д´-` should {
            withClue("left arm") { it.leftArm shouldBe "-" }
            withClue("left eye") { it.leftEye shouldBe "`" }
            withClue("mouth") { it.mouth shouldBe "д" }
            withClue("right eye") { it.rightEye shouldBe "´" }
            withClue("right arm") { it.rightArm shouldBe "-" }
            withClue("accessory") { it.accessory shouldBe "" }
        }
    }

    @Test fun random() = testAll {
        Kaomoji.random().toString().shouldNotBeEmpty()

        Kaomoji.random(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()) should {
            listOf(Kaomoji.Chasing.first(), Kaomoji.Screaming.first()).shouldContain(it)
        }

        Kaomoji.random(Kaomoji.Chasing, Kaomoji.Screaming) should {
            listOf(*Kaomoji.Chasing.toTypedArray(), *Kaomoji.Screaming.toTypedArray()).shouldContain(it)
        }
    }

    @Test fun fishing() = testAll {
        Kaomoji.random().fishing(Kaomoji.Fish.`❮°«⠶＞˝`) shouldEndWith "o/￣￣￣❮°«⠶＞˝"
        Kaomoji.Shrugging.first().fishing() shouldStartWith "┐(´д｀)o/￣￣￣"
    }

    @Test fun thinking() = testAll {
        val kaomoji = Kaomoji.Bears.first()
        val blank = " ".repeat(kaomoji.columns)

        listOf(null, "").forAll {
            kaomoji.thinking(it) shouldBe """
                    $blank  ̣ ˱ ❨ ( … )
                    ・㉨・
                """.trimIndent()
        }

        kaomoji.thinking("oh no") shouldBe """
                    $blank  ̣ ˱ ❨ ( oh no )
                    ・㉨・
                """.trimIndent()

        kaomoji.thinking("oh no 1${LF}oh no 2") shouldBe """
                    $blank       ⎛ oh no 1 ⎞
                    $blank  ̣ ˱ ❨ ⎝ oh no 2 ⎠
                    ・㉨・
                """.trimIndent()

        kaomoji.thinking("oh no 1${LF}oh no 2${LF}oh no 3") shouldBe """
                    $blank       ⎛ oh no 1 ⎞
                    $blank       ⎜ oh no 2 ⎟
                    $blank  ̣ ˱ ❨ ⎝ oh no 3 ⎠
                    ・㉨・
                """.trimIndent()

        kaomoji.thinking("123${LF}${LF}1234567890${LF}1234") shouldBe """
                    $blank       ⎛ 123        ⎞
                    $blank       ⎜            ⎟
                    $blank       ⎜ 1234567890 ⎟
                    $blank  ̣ ˱ ❨ ⎝ 1234       ⎠
                    ・㉨・
                """.trimIndent()

        kaomoji.thinking("\u001B[4m\u001B[94m123\u001B[24;39m\n\u001B[4m\u001B[24m\n\u001B[4m1234567890\u001B[24m\n\u001B[4m1234\u001B[24m") shouldBe """
                    $blank       ⎛ [4m[94m123[24;39m        ⎞
                    $blank       ⎜ [4m[24m           ⎟
                    $blank       ⎜ [4m1234567890[24m ⎟
                    $blank  ̣ ˱ ❨ ⎝ [4m1234[24m       ⎠
                    ・㉨・
                """.trimIndent()
    }

    @Test fun all() = Kaomoji.testAll {
        it.toString().shouldNotBeEmpty()
    }
}
