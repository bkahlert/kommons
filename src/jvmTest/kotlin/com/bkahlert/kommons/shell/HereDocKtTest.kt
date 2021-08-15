package com.bkahlert.kommons.shell

import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.text.lines
import com.bkahlert.kommons.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEqualTo
import strikt.assertions.isNotBlank
import strikt.assertions.isNotEqualTo

class HereDocKtTest {

    @Test
    fun `should create here document using HERE- delimiter`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc()
        expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
    }

    @Nested
    inner class ParameterSubstitution {

        @Nested
        inner class Enabled {

            private val hereDoc = HereDoc(commands = arrayOf("\$HOME"), delimiter = "TEST")

            @Test
            fun `should not change delimiter`() {
                expectThat(hereDoc).lines().first().isEqualTo("<<TEST")
            }

            @Test
            fun `should substitute parameters`() {
                expectThat(ShellScript {
                    !"cat $hereDoc"
                }.exec().io.output.ansiRemoved) {
                    isNotBlank()
                    isNotEqualTo("\$HOME")
                }
            }
        }

        @Nested
        inner class Disabled {

            private val hereDoc = HereDoc(commands = arrayOf("\$HOME"), delimiter = "TEST", substituteParameters = false)

            @Test
            fun `should not single quote delimiter`() {
                expectThat(hereDoc).lines().first().isEqualTo("<<'TEST'")
            }

            @Test
            fun `should not substitute parameters`() {
                expectThat(ShellScript {
                    !"cat $hereDoc"
                }.exec().io.output.ansiRemoved) {
                    isEqualTo("\$HOME")
                }
            }
        }
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
    inner class CompanionObject {

        @Test
        fun `should find delimiters`() {
            val text = "random string " +
                HereDoc("command a-1", "command a-2", delimiter = "DELIMITER-A") +
                " random string" +
                HereDoc("command b-1", "command b-1", delimiter = "DELIMITER-B")

            expectThat(HereDoc.findAllDelimiters(text)).containsExactly("DELIMITER-A", "DELIMITER-B")
        }

        @Test
        fun `should build`() {
            val hereDoc = HereDoc {
                delimiter = "DELIMITER"
                substituteParameters = false
                +"command a-1"
                +"command a-2"
            }
            expectThat(hereDoc)
                .isEqualTo(HereDoc("command a-1", "command a-2", delimiter = "DELIMITER", substituteParameters = false))
        }
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
    }
}
