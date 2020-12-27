package koodies.text.styling

import koodies.concurrent.process.IO
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.matchesCurlyPattern
import koodies.test.output.Columns
import koodies.text.repeat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class BoxesTest {
    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render FAIL`() {
        logLine { IO.Type.ERR typed Boxes.FAIL.toString() }
        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴BoxesTest ➜ should render FAIL{}
            │   
            │   ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
            │   ████▌▄▌▄▐▐▌█████
            │   ████▌▄▌▄▐▐▌▀████
            │   ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should render sphere box`() {
        logLine { IO.Type.META typed Boxes.SPHERICAL("SPHERICAL") }
        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴BoxesTest ➜ should render sphere box{}
            │   
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${'\u00A0'.repeat(3)}SPHERICAL${'\u00A0'.repeat(3)}  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render single line sphere box`() {
        logLine { IO.Type.META typed Boxes.SINGLE_LINE_SPHERICAL("SINGLE LINE SPHERICAL") }
        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴BoxesTest ➜ should render single line sphere box{}
            │   
            │    ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render wide pillars`() {
        logLine { IO.Type.META typed Boxes.WIDE_PILLARS("WIDE PILLARS") }
        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴BoxesTest ➜ should render wide pillars{}
            │   
            │   █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(150) InMemoryLogger.`should render pillars`() {
        logLine { IO.Type.META typed Boxes.PILLARS("PILLARS") }
        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴BoxesTest ➜ should render pillars{}
            │   
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
        """.trimIndent())
    }
}
