package koodies.exec

import koodies.io.path.Locations
import koodies.test.string
import koodies.test.toStringIsEqualTo
import koodies.text.matchesCurlyPattern
import koodies.text.quoted
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.time.milliseconds

class CommandLineTest {

    @Test
    fun `should run`() {
        val command = CommandLine("echo", "test")
        expectThat(command) {
            string.continuationsRemoved.isEqualTo("echo test")
            evaluatesTo("test")
        }
    }

    @Test
    fun `should run with more arguments`() {
        val command = CommandLine("echo", "one", "two", "three")
        expectThat(command) {
            string.continuationsRemoved.isEqualTo("echo one two three")
            evaluatesTo("one two three")
        }
    }

    @Nested
    inner class CompanionObject {

        @Test
        fun `should parse single line`() {
            expectThat(CommandLine.parse("""
                echo Hello
            """.trimIndent()).commandLineParts.toList())
                .containsExactly("echo", "Hello")
        }

        @Test
        fun `should parse multi line`() {
            expectThat(CommandLine.parse("""
                echo \
                Hello
            """.trimIndent()).commandLineParts.toList())
                .containsExactly("echo", "Hello")
        }

        @Test
        fun `should parse empty line`() {
            expectThat(CommandLine.parse("""
                echo \
                \
                Hello
            """.trimIndent()).commandLineParts.toList())
                .containsExactly("echo", "Hello")
        }
    }

    @Nested
    inner class CommandLineProperty {

        @Test
        fun `should return command line`() {
            expectThat(CommandLine("command", "arg1", "arg2").shellCommand)
                .isEqualTo("command arg1 arg2")
        }

        @Test
        fun `should quote parts with spaces`() {
            expectThat(CommandLine("com mand", "arg 1").shellCommand)
                .isEqualTo("\"com mand\" \"arg 1\"")
        }

        @Test
        fun `should quote parts with tabs`() {
            expectThat(CommandLine("com\tmand", "arg\t1").shellCommand)
                .isEqualTo("\"com\tmand\" \"arg\t1\"")
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `should not expand unquoted parameters`() {
            val command = CommandLine("echo", "\$HOME")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("echo \$HOME")
                evaluated {
                    not { output.isEqualTo(System.getProperty("user.home")) }
                    exitCodeOrNull.isEqualTo(0)
                }
            }
        }

        @Test
        fun `should not expand quoted parameters`() {
            val command = CommandLine("echo", "\\\$HOME")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("echo \\\$HOME")
                evaluated {
                    not { output.isEqualTo(System.getProperty("user.home")) }
                    exitCodeOrNull.isEqualTo(0)
                }
            }
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `should output formatted`() {
            expectThat(CommandLine("command", "-a", "--bee", "c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            c \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle whitespaces correctly command`() {
            expectThat(CommandLine("command", " - a", "    ", "c c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            "- a" \
             \
            "c c" \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle nesting`() {
            expectThat(CommandLine(
                "command",
                "-a",
                "--bee",
                CommandLine("command", "-a", "--bee", "c", "x y z".quoted).toString(),
                "x y z".quoted)
            ).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            "command \
            -a \
            --bee \
            c \
            \"x y z\"" \
            "x y z"
        """.trimIndent())
        }
    }

    @Nested
    inner class Quoting {

        @Test
        fun `should not quote unnecessarily`() {
            val command = CommandLine("echo", "Hello")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("echo Hello")
                evaluatesTo("Hello")
            }
        }

        @Test
        fun `should quote on whitespaces`() {
            val command = CommandLine("echo", "Hello World!")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("echo \"Hello World!\"")
                evaluatesTo("Hello World!")
            }
        }

        @Test
        fun `should not substitute parameters`() {
            val command = CommandLine("echo", "\$HOME")
            expectThat(command) {
                evaluatesTo("\$HOME")
            }
        }

        @Test
        fun `should not escape parameters`() {
            val command = CommandLine("echo", "\$HOME")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("echo \$HOME")
            }
        }
    }

    @Nested
    inner class Nesting {

        private fun Assertion.Builder<Exec>.outputParsedAsCommandLine() =
            get { CommandLine.parse(io.output.ansiRemoved) }

        @Test
        fun `should produce runnable output`() {
            val nestedCommand = CommandLine("echo", "Hello").toString()
            val command = CommandLine("echo", nestedCommand)
            expectThat(command) {
                toStringIsEqualTo("""
                    echo \
                    "echo \
                    Hello"
                """.trimIndent())
                evaluated {
                    output.isEqualTo("""
                        echo \
                        Hello
                    """.trimIndent())
                    outputParsedAsCommandLine().evaluatesTo("Hello")
                }
            }
        }

        @Test
        fun `should produce runnable quoted output`() {
            val nestedCommand = CommandLine("echo", "Hello World!").toString()
            val command = CommandLine("echo", nestedCommand)
            expectThat(command) {
                toStringIsEqualTo("""
                    echo \
                    "echo \
                    \"Hello World!\""
                """.trimIndent())
                evaluated {
                    output.isEqualTo("""
                        echo \
                        "Hello World!"
                    """.trimIndent())
                    outputParsedAsCommandLine().evaluatesTo("Hello World!")
                }
            }
        }

        @Test
        fun `should produce runnable single quoted output`() {
            val nestedCommand = CommandLine("echo", "'Hello World!'").toString()
            val command = CommandLine("echo", nestedCommand)
            expectThat(command) {
                toStringIsEqualTo("""
                    echo \
                    "echo \
                    \"'Hello World!'\""
                """.trimIndent())
                evaluated {
                    output.isEqualTo("""
                        echo \
                        "'Hello World!'"
                    """.trimIndent())
                    outputParsedAsCommandLine().evaluatesTo("'Hello World!'")
                }
            }
        }
    }

    @Nested
    inner class Rendering {

        @Test
        fun `should provide summary`() {
            expectThat(CommandLine(
                "!ls", "-lisa",
                "!mkdir", "-p", "/shared",
            ).summary).matchesCurlyPattern("!ls -lisa !mkdir -p /shared")
        }
    }
}

val <T : CharSequence> Assertion.Builder<T>.continuationsRemoved: DescribeableBuilder<String>
    get() = get("continuation removed %s") { replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<Exec>
    get() = get("evaluated %s") {
        toExec(false, emptyMap(), Locations.Temp, null).process({ sync }, Processors.noopProcessor())
    }

fun Assertion.Builder<CommandLine>.evaluated(block: Assertion.Builder<Exec>.() -> Unit) =
    evaluated.block()

val Assertion.Builder<Exec>.output
    get() = get("output of type IO.Output %s") { io.output.ansiRemoved }

val <P : Exec> Assertion.Builder<P>.exitCodeOrNull
    get() = get("exit value %s") { exitCodeOrNull }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String) {
    with(evaluated) {
        io.output.ansiRemoved.isEqualTo(expectedOutput)
        50.milliseconds.sleep()
    }
}
