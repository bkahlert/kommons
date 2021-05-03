package koodies.text.styling

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo


class AlignmentsKtTest {

    @Test
    fun `should center text`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.center('|')
        expectThat(actual).isEqualTo("""
            ||foo||
            bar baz
        """.trimIndent())
    }

    @Test
    fun `should center string collection`() {
        val string = listOf("     foo", "  bar baz ")
        val actual = string.center('X')
        expectThat(actual).containsExactly("XXfooXX", "bar baz")
    }
}
