package koodies.text.styling

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

