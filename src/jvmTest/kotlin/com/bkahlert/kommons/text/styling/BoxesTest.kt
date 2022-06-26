package com.bkahlert.kommons.text.styling

import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.text.repeat
import com.bkahlert.kommons.tracing.TestSpanScope
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class BoxesTest {

    @Test
    fun TestSpanScope.`should render FAIL`() {
        log(Boxes.FAIL.toString())
        rendered() shouldMatchGlob """
            ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
            ████▌▄▌▄▐▐▌█████
            ████▌▄▌▄▐▐▌▀████
            ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
        """.trimIndent()
    }

    @Test
    fun TestSpanScope.`should render sphere box`() {
        log(Boxes.SPHERICAL("SPHERICAL\nlong ... l ... i ... n ... e"))
        val pad = Unicode.NBSP.repeat(9)
        rendered() shouldMatchGlob """
              █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}SPHERICAL${pad}${Unicode.NBSP}  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
              █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
        """.trimIndent()
    }

    @Test
    fun TestSpanScope.`should render single line sphere box`() {
        log(
            Boxes.SINGLE_LINE_SPHERICAL(
                "SINGLE LINE SPHERICAL$LF" +
                    "long ... l ... i ... n ... e"
            )
        )
        rendered().lines().shouldContainExactly(
            " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁     SINGLE LINE SPHERICAL      ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  ",
            " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  long ... l ... i ... n ... e  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  ",
        )
    }

    @Test
    fun TestSpanScope.`should render wide pillars`() {
        log(
            Boxes.WIDE_PILLARS(
                "WIDE PILLARS$LF" +
                    "long ... l ... i ... n ... e"
            )
        )
        val pad = Unicode.NBSP.repeat(8)
        rendered().lines().shouldContainExactly(
            "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ ${pad}WIDE PILLARS$pad  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █",
            "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ long ... l ... i ... n ... e  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █",
        )
    }

    @Test
    fun TestSpanScope.`should render pillars`() {
        log(
            Boxes.PILLARS(
                "PILLARS$LF" +
                    "long ... l ... i ... n ... e"
            )
        )
        val pad = Unicode.NBSP.repeat(10)
        rendered().lines().shouldContainExactly(
            "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}PILLARS${pad}${Unicode.NBSP}  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █",
            "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █",
        )
    }
}
