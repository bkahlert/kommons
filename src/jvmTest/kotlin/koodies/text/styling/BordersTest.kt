package koodies.text.styling

import koodies.io.path.containsOnlyCharacters
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class BordersTest {

    @TestFactory
    fun `should provide extended member function with corresponding names to serve as an overview`() =
        Borders.values().flatMap { border: Borders ->
            val matrix = border.matrix
            listOf(
                dynamicTest("${border.name}\n$matrix") {
                    val staticallyRendered = border.name.wrapWithBorder(matrix, 2, 4, ANSI.termColors.hsv(270, 50, 50))

                    val memberFun = "".draw.border::class.members.single { it.name == border.name.decapitalize() }
                    val renderedMember = memberFun.call(border.name.draw.border, 2, 4, ANSI.termColors.hsv(270, 50, 50))

                    expectThat(staticallyRendered)
                        .isEqualTo(renderedMember.toString())
                        .get { removeEscapeSequences() }.containsOnlyCharacters((matrix + border.name).toCharArray())
                }
            )
        }


    @TestFactory
    fun `should border centered text`(): List<DynamicTest> {
        val string = """
                   foo
              bar baz
        """.trimIndent()

        return listOf(
            string.wrapWithBorder("╭─╮\n│*│\n╰─╯", 0, 0),
            string.lines().wrapWithBorder("╭─╮\n│*│\n╰─╯", 0, 0)).map {
            dynamicTest(it) {
                expectThat(it).isEqualTo("""
                    ╭───────╮
                    │**foo**│
                    │bar baz│
                    ╰───────╯
                """.trimIndent())
            }
        }
    }

    @TestFactory
    fun `should border centered text with padding and margin`(): List<DynamicTest> {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        return listOf(
            string.wrapWithBorder("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4),
            string.lines().wrapWithBorder("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4)).map {
            dynamicTest(it) {
                expectThat(it).isEqualTo("""
                    *********************
                    *********************
                    ****╭───────────╮****
                    ****│***********│****
                    ****│****foo****│****
                    ****│**bar baz**│****
                    ****│***********│****
                    ****╰───────────╯****
                    *********************
                    *********************
                """.trimIndent())
            }
        }
    }
}

