package koodies.shell

import koodies.test.toStringIsEqualTo
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class HereDocKtTest {

    @Test
    fun `should create here document using given prefix and line separator`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc("MY-PREFIX", "␤")
        expectThat(hereDoc).toStringIsEqualTo("<<MY-PREFIX␤line 1␤line 2␤MY-PREFIX")
    }

    @Test
    fun `should create here document using HERE- prefix and line feed by default`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc()
        expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
    }

    @Test
    fun `should accept empty list`() {
        val hereDoc = listOf<String>().toHereDoc()
        expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            HERE-{}
        """.trimIndent())
    }

    @Nested
    inner class Support {
        @Test
        fun `for Array`() {
            val hereDoc = arrayOf("line 1", "line 2").toHereDoc()
            expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }

        @Test
        fun `for Iterable`() {
            val hereDoc = listOf("line 1", "line 2").asIterable().toHereDoc()
            expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }
    }

    @Nested
    inner class VarargConstructor {
        @Test
        fun `should take unnamed arguments as lines `() {
            val hereDoc = HereDoc("line 1", "line 2")
            expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }


        @Test
        fun `should take named arguments as such`() {
            val hereDoc = HereDoc("line 1", "line 2", label = "test")
            expectThat(hereDoc).matchesCurlyPattern("""
            <<test
            line 1
            line 2
            test
        """.trimIndent())
        }
    }
}
