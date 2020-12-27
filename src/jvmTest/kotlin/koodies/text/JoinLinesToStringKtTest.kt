package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class JoinLinesToStringKtTest {

    @Test
    fun `should join list to multiline string`() {
        val actual = listOf("line1", "line2").joinLinesToString("prefix-", "-postfix") { "LINE " + it.drop(4) }
        expectThat(actual).isEqualTo("""
                prefix-LINE 1
                LINE 2-postfix
            """.trimIndent())
    }

    @Test
    fun `should join sequence to multiline string`() {
        val actual = sequenceOf("line1", "line2").joinLinesToString("prefix-", "-postfix") { "LINE " + it.drop(4) }
        expectThat(actual).isEqualTo("""
                prefix-LINE 1
                LINE 2-postfix
            """.trimIndent())
    }
}
