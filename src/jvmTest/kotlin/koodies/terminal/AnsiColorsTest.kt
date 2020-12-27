package koodies.terminal

import koodies.terminal.AnsiColors.black
import koodies.terminal.AnsiColors.blue
import koodies.terminal.AnsiColors.brightBlue
import koodies.terminal.AnsiColors.brightCyan
import koodies.terminal.AnsiColors.brightGreen
import koodies.terminal.AnsiColors.brightMagenta
import koodies.terminal.AnsiColors.brightRed
import koodies.terminal.AnsiColors.brightWhite
import koodies.terminal.AnsiColors.brightYellow
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiColors.gray
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.magenta
import koodies.terminal.AnsiColors.red
import koodies.terminal.AnsiColors.white
import koodies.terminal.AnsiColors.yellow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AnsiColorsTest {

    @Test
    fun `should colorize magenta on cyan`() {
        expectThat("magenta on cyan".magenta { cyan }).isEqualTo((ANSI.termColors.magenta on ANSI.termColors.cyan)("magenta on cyan"))
    }

    @Test
    fun `should colorize black`() {
        expectThat("black".black()).isEqualTo(ANSI.termColors.black("black"))
    }

    @Test
    fun `should colorize red`() {
        expectThat("red".red()).isEqualTo(ANSI.termColors.red("red"))
    }

    @Test
    fun `should colorize green`() {
        expectThat("green".green()).isEqualTo(ANSI.termColors.green("green"))
    }

    @Test
    fun `should colorize yellow`() {
        expectThat("yellow".yellow()).isEqualTo(ANSI.termColors.yellow("yellow"))
    }

    @Test
    fun `should colorize blue`() {
        expectThat("blue".blue()).isEqualTo(ANSI.termColors.blue("blue"))
    }

    @Test
    fun `should colorize magenta`() {
        expectThat("magenta".magenta()).isEqualTo(ANSI.termColors.magenta("magenta"))
    }

    @Test
    fun `should colorize cyan`() {
        expectThat("cyan".cyan()).isEqualTo(ANSI.termColors.cyan("cyan"))
    }

    @Test
    fun `should colorize white`() {
        expectThat("white".white()).isEqualTo(ANSI.termColors.white("white"))
    }

    @Test
    fun `should colorize gray`() {
        expectThat("gray".gray()).isEqualTo(ANSI.termColors.gray("gray"))
    }

    @Test
    fun `should colorize brightRed`() {
        expectThat("brightRed".brightRed()).isEqualTo(ANSI.termColors.brightRed("brightRed"))
    }

    @Test
    fun `should colorize brightGreen`() {
        expectThat("brightGreen".brightGreen()).isEqualTo(ANSI.termColors.brightGreen("brightGreen"))
    }

    @Test
    fun `should colorize brightYellow`() {
        expectThat("brightYellow".brightYellow()).isEqualTo(ANSI.termColors.brightYellow("brightYellow"))
    }

    @Test
    fun `should colorize brightBlue`() {
        expectThat("brightBlue".brightBlue()).isEqualTo(ANSI.termColors.brightBlue("brightBlue"))
    }

    @Test
    fun `should colorize brightMagenta`() {
        expectThat("brightMagenta".brightMagenta()).isEqualTo(ANSI.termColors.brightMagenta("brightMagenta"))
    }

    @Test
    fun `should colorize brightCyan`() {
        expectThat("brightCyan".brightCyan()).isEqualTo(ANSI.termColors.brightCyan("brightCyan"))
    }

    @Test
    fun `should colorize brightWhite`() {
        expectThat("brightWhite".brightWhite()).isEqualTo(ANSI.termColors.brightWhite("brightWhite"))
    }
}
