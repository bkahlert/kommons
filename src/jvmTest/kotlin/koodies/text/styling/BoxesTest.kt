package koodies.text.styling

import koodies.text.LineSeparators.LF
import koodies.text.Unicode.NBSP
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.repeat
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Test
import strikt.assertions.containsExactly

class BoxesTest {

    @Test
    fun TestSpan.`should render FAIL`() {
        log(Boxes.FAIL.toString())
        expectThatRendered().matchesCurlyPattern("""
            ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
            ████▌▄▌▄▐▐▌█████
            ████▌▄▌▄▐▐▌▀████
            ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render sphere box`() {
        log(Boxes.SPHERICAL("SPHERICAL\nlong ... l ... i ... n ... e"))
        val pad = NBSP.repeat(9)
        expectThatRendered().matchesCurlyPattern("""
              █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}SPHERICAL${pad}$NBSP  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
              █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render single line sphere box`() {
        log(Boxes.SINGLE_LINE_SPHERICAL("SINGLE LINE SPHERICAL$LF" +
            "long ... l ... i ... n ... e"))
        expectThatRendered().lines().containsExactly(
            " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁     SINGLE LINE SPHERICAL      ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  ",
            " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  long ... l ... i ... n ... e  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  ",
        )
    }

    @Test
    fun TestSpan.`should render wide pillars`() {
        log(Boxes.WIDE_PILLARS("WIDE PILLARS$LF" +
            "long ... l ... i ... n ... e"))
        val pad = NBSP.repeat(8)
        expectThatRendered().lines().containsExactly(
            "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ ${pad}WIDE PILLARS$pad  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █",
            "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ long ... l ... i ... n ... e  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █",
        )
    }

    @Test
    fun TestSpan.`should render pillars`() {
        log(Boxes.PILLARS("PILLARS$LF" +
            "long ... l ... i ... n ... e"))
        val pad = NBSP.repeat(10)
        expectThatRendered().lines().containsExactly(
            "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}PILLARS${pad}$NBSP  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █",
            "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █",
        )
    }
}
