package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty


class MatchKtTest {

    @Test
    fun `should fill placeholder on match`() {
        val input = """! perl -i.bak -pe 's|(?<=the-user:)[^:]*|crypt("the-password","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' /some/where/in/the/shadow"""

        val actual = input.match("""! perl -i.{} -pe 's|(?<={}:)[^:]*|crypt("{}","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' {}/shadow""")

        expectThat(actual).containsExactly("bak", "the-user", "the-password", "{}", "/some/where/in/the")
    }

    @Test
    fun `should not fill placeholder on mismatch`() {
        val input = """! perl -i.bak -pe 's|(?<=the-user:)[^:]*|crypt("the-password","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' /some/where/in/the/shadow"""

        val actual = input.match("""! perl harbor -i.{} -pe 's|(?<={}:)[^:]*|crypt("{}","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' {}/shadow""")

        expectThat(actual).isEmpty()
    }
}
