package koodies.text.styling

import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.output.Columns
import koodies.text.LineSeparators.LF
import koodies.text.Unicode.NBSP
import koodies.text.matchesCurlyPattern
import koodies.text.repeat
import org.junit.jupiter.api.Test

class BoxesTest {
    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render FAIL`() {
        logLine { Boxes.FAIL.toString() }
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴BoxesTest ➜ should render FAIL{}
            │
            │   ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
            │   ████▌▄▌▄▐▐▌█████
            │   ████▌▄▌▄▐▐▌▀████
            │   ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should render sphere box`() {
        logLine { Boxes.SPHERICAL("SPHERICAL\nlong ... l ... i ... n ... e") }
        val pad = NBSP.repeat(9)
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴BoxesTest ➜ should render sphere box{}
            │
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}SPHERICAL${pad}$NBSP  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏                ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render single line sphere box`() {
        logLine {
            Boxes.SINGLE_LINE_SPHERICAL("SINGLE LINE SPHERICAL$LF" +
                "long ... l ... i ... n ... e")
        }
        val pad = NBSP.repeat(3)
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴BoxesTest ➜ should render single line sphere box{}
            │
            │    ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  ${pad}SINGLE LINE SPHERICAL${pad}$NBSP  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  
            │    ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  long ... l ... i ... n ... e  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render wide pillars`() {
        logLine {
            Boxes.WIDE_PILLARS("WIDE PILLARS$LF" +
                "long ... l ... i ... n ... e")
        }
        val pad = NBSP.repeat(8)
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴BoxesTest ➜ should render wide pillars{}
            │
            │   █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ ${pad}WIDE PILLARS$pad  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
            │   █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ long ... l ... i ... n ... e  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render pillars`() {
        logLine {
            Boxes.PILLARS("PILLARS$LF" +
                "long ... l ... i ... n ... e")
        }
        val pad = NBSP.repeat(10)
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴BoxesTest ➜ should render pillars{}
            │
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${pad}PILLARS${pad}$NBSP  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ long ... l ... i ... n ... e  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
