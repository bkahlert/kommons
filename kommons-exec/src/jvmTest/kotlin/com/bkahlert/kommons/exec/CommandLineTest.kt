package com.bkahlert.kommons.exec

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.string
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.testsOld
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.milliseconds

class CommandLineTest {

    @Test
    fun `should run`() {
        val command = CommandLine("echo", "test")
        expectThat(command) {
            string.continuationsRemoved.isEqualTo("'echo' 'test'")
            evaluatesTo("test")
        }
    }

    @Test
    fun `should run with more arguments`() {
        val command = CommandLine("echo", "one", "two", "three")
        expectThat(command) {
            string.continuationsRemoved.isEqualTo("'echo' 'one' 'two' 'three'")
            evaluatesTo("one two three")
        }
    }

    @Nested
    inner class CompanionObject {

        @TestFactory
        fun `should parse single line`() = testEachOld(
            "echo Hello",
            "'echo' 'Hello'",
        ) {
            expecting { CommandLine.parse(it).commandLineParts.toList() } that {
                containsExactly("echo", "Hello")
            }
        }

        @TestFactory
        fun `should parse multi line`() = testEachOld(
            """
                echo \
                Hello
            """.trimIndent(),
            """
                'echo' \
                'Hello'
            """.trimIndent(),
        ) {
            expecting { CommandLine.parse(it).commandLineParts.toList() } that {
                containsExactly("echo", "Hello")
            }
        }

        @TestFactory
        fun `should ignore trailing new line`() = testEachOld(
            """
                echo Hello

            """.trimIndent(),
            """
                'echo' 'Hello'

            """.trimIndent(),
            """
                echo \
                Hello

            """.trimIndent(),
            """
                'echo' \
                'Hello'

            """.trimIndent(),
        )
        {
            expecting { CommandLine.parse(it).commandLineParts.toList() } that {
                containsExactly("echo", "Hello")
            }
        }

        @TestFactory
        fun `should parse empty line`() = testEachOld(
            """
                echo \
                \
                Hello
            """.trimIndent(),
            """
                'echo' \
                \
                'Hello'
            """.trimIndent(),
        ) {
            expecting { CommandLine.parse(it).commandLineParts.toList() } that {
                containsExactly("echo", "Hello")
            }
        }
    }

    @Nested
    inner class CommandLineProperty {

        @Test
        fun `should return command line`() {
            expectThat(CommandLine("command", "arg1", "arg2").shellCommand)
                .isEqualTo("'command' 'arg1' 'arg2'")
        }

        @Test
        fun `should quote parts with spaces`() {
            expectThat(CommandLine("com mand", "arg 1").shellCommand)
                .isEqualTo("'com mand' 'arg 1'")
        }

        @Test
        fun `should quote parts with tabs`() {
            expectThat(CommandLine("com\tmand", "arg\t1").shellCommand)
                .isEqualTo("'com\tmand' 'arg\t1'")
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `should not expand unquoted parameters`() {
            val command = CommandLine("echo", "\$HOME")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("'echo' '\$HOME'")
                evaluated {
                    not { output.isEqualTo(SystemLocations.Home.pathString) }
                    exitCodeOrNull.isEqualTo(0)
                }
            }
        }

        @Test
        fun `should not expand quoted parameters`() {
            val command = CommandLine("echo", "\\\$HOME")
            expectThat(command) {
                string.continuationsRemoved.isEqualTo("'echo' '\\\$HOME'")
                evaluated {
                    not { output.isEqualTo(SystemLocations.Home.pathString) }
                    exitCodeOrNull.isEqualTo(0)
                }
            }
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `should output formatted`() {
            expectThat(CommandLine("command", "-a", "--bee", "c", "x y z".quoted)).toStringIsEqualTo(
                """
            'command' \
            '-a' \
            '--bee' \
            'c' \
            '"x y z"'
        """.trimIndent()
            )
        }

        @Test
        fun `should handle whitespaces correctly command`() {
            expectThat(CommandLine("command", " - a", "    ", "c c", "x y z".quoted)).toStringIsEqualTo(
                """
            'command' \
            ' - a' \
            '    ' \
            'c c' \
            '"x y z"'
        """.trimIndent()
            )
        }

        @Test
        fun `should handle nesting`() {
            expectThat(
                CommandLine(
                    "command",
                    "-a",
                    "--bee",
                    CommandLine("command", "-a", "--bee", "c", "x y z".quoted).toString(),
                    "x y z".quoted
                )
            ).toStringIsEqualTo(
                """
            'command' \
            '-a' \
            '--bee' \
            ''"'"'command'"'"' \
            '"'"'-a'"'"' \
            '"'"'--bee'"'"' \
            '"'"'c'"'"' \
            '"'"'"x y z"'"'"'' \
            '"x y z"'
        """.trimIndent()
            )
        }
    }

    @Nested
    inner class Quoting {

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
                string.continuationsRemoved.isEqualTo("'echo' '\$HOME'")
            }
        }
    }

    @TestFactory
    fun `should quote quotes correctly`() = testsOld {
        val script = ShellScript {
            shebang("/bin/bash")
            """
                    echo "double quotes"
                    echo 'single quotes'

                """.trimIndent()
        }

        val commandLine = CommandLine("echo", script.toString())

        expecting { commandLine.commandLineParts.toList() } that {
            containsExactly(
                "echo", """
                    #!/bin/bash
                    echo "double quotes"
                    echo 'single quotes'

                    """.trimIndent()
            )
        }

        expecting { commandLine.shellCommand } that {
            isEqualTo(
                """
                    'echo' '#!/bin/bash
                    echo "double quotes"
                    echo '"'"'single quotes'"'"'
                    '
                """.trimIndent()
            )
        }

        expecting { commandLine.multiLineShellCommand } that {
            isEqualTo(
                """
                    'echo' \
                    '#!/bin/bash
                    echo "double quotes"
                    echo '"'"'single quotes'"'"'
                    '
                """.trimIndent()
            )
        }

        val output = commandLine.exec().io.output.ansiRemoved
        expecting { output } that {
            isEqualTo(
                """
                #!/bin/bash
                echo "double quotes"
                echo 'single quotes'

            """.trimIndent()
            )
        }

        expecting { ShellScript { output }.exec() } that {
            io.output.ansiRemoved.isEqualTo(
                """
                double quotes
                single quotes
                """.trimIndent()
            )
        }
    }

    @Nested
    inner class Content {

        @Test
        fun `should provide content`() {
            CommandLine(
                "!ls", "-lisa",
                "!mkdir", "-p", "/shared",
            ).content shouldMatchGlob "!ls -lisa !mkdir -p /shared"
        }
    }
}

val <T : CharSequence> Assertion.Builder<T>.continuationsRemoved: DescribeableBuilder<String>
    get() = get("continuation removed %s") { replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<Exec>
    get() = get("evaluated %s") { toExec(false, emptyMap(), SystemLocations.Temp, null).process() }

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