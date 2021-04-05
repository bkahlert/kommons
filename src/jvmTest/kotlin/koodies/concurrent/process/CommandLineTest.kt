package koodies.concurrent.process

import koodies.concurrent.toManagedProcess
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.matchesCurlyPattern
import koodies.text.quoted
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.time.milliseconds

@Execution(CONCURRENT)
class CommandLineTest {

    @TestFactory
    fun `should have reasonable defaults`() = testEach(
        CommandLine(emptyList(), emptyMap(), Locations.WorkingDirectory, "command", emptyList()) to listOf(
            CommandLine(emptyMap(), Locations.WorkingDirectory, "command"),
            CommandLine(Locations.WorkingDirectory, "command"),
            CommandLine("command"),
        ),
        CommandLine(emptyList(), emptyMap(), Locations.WorkingDirectory, "command", listOf("--flag", "-abc", "-d", "arg")) to listOf(
            CommandLine(emptyMap(), Locations.WorkingDirectory, "command", "--flag", "-abc", "-d", "arg"),
            CommandLine(Locations.WorkingDirectory, "command", "--flag", "-abc", "-d", "arg"),
            CommandLine("command", "--flag", "-abc", "-d", "arg"),
        ),
        CommandLine(emptyList(), emptyMap(), Locations.Temp, "command", listOf("--flag", "-abc", "-d", "arg")) to listOf(
            CommandLine(emptyMap(), Locations.Temp, "command", "--flag", "-abc", "-d", "arg"),
            CommandLine(Locations.Temp, "command", "--flag", "-abc", "-d", "arg"),
            CommandLine("command", "--flag", "-abc", "-d", "arg"),
        ),
        CommandLine(emptyList(), mapOf("env1" to "val1"), Locations.Temp, "command", listOf("--flag", "-abc", "-d", "arg")) to listOf(
            CommandLine(mapOf("env1" to "val1"), Locations.Temp, "command", "--flag", "-abc", "-d", "arg"),
        ),
    ) { (commandLine, variants) ->
        variants.forEach { expect { it }.that { isEqualTo(commandLine) } }
    }

    @Test
    fun `should run`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val command = CommandLine(emptyMap(), this, "echo", "test")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo test")
            evaluatesTo("test")
        }
    }

    @Test
    fun `should run with more arguments`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val command = CommandLine(emptyMap(), this, "echo", "one", "two", "three")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo one two three")
            evaluatesTo("one two three")
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `should expand`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val command = CommandLine(emptyMap(), this, "echo", "\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \$HOME")
                evaluatesTo(System.getProperty("user.home"))
            }
        }

        @Test
        fun `should not expand`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val command = CommandLine(emptyMap(), this, "echo", "\\\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \\\$HOME")
                evaluated {
                    not { output.isEqualTo(System.getProperty("user.home")) }
                    exitState.isEqualTo(0)
                }
            }
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `should output formatted`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(CommandLine(emptyMap(), this, "command", "-a", "--bee", "c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            c \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle whitespaces correctly command`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(CommandLine(emptyMap(), this, "command", " - a", "    ", "c c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            "- a" \
             \
            "c c" \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle nesting`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(CommandLine(emptyMap(), this,
                "command",
                "-a",
                "--bee",
                CommandLine(emptyMap(), this, "command", "-a", "--bee", "c", "x y z".quoted).toString(),
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
        fun `should not quote unnecessarily`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val command = CommandLine(emptyMap(), this, "echo", "Hello")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo Hello")
                evaluatesTo("Hello")
            }
        }

        @Test
        fun `should quote on whitespaces`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val command = CommandLine(emptyMap(), this, "echo", "Hello World!")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"Hello World!\"")
                evaluatesTo("Hello World!")
            }
        }

        @Test
        fun `should support single quotes`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val command = CommandLine(emptyMap(), this, "echo", "'\$HOME'")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo '\$HOME'")
                evaluatesTo("\$HOME")
            }
        }
    }

    @Nested
    inner class Nesting {

        @Test
        fun `should produce runnable output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "Hello")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo Hello\"")
                evaluated {
                    output.isEqualTo("echo Hello")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("Hello")
                }
            }
        }

        @Test
        fun `should produce runnable quoted output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "Hello World!")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"Hello World!\\\"\"")
                evaluated {
                    output.isEqualTo("echo \"Hello World!\"")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("Hello World!")
                }
            }
        }

        @Test
        fun `should produce runnable single quoted output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "'Hello World!'")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"'Hello World!'\\\"\"")
                evaluated {
                    output.isEqualTo("echo \"'Hello World!'\"")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("'Hello World!'")
                }
            }
        }
    }

    @Nested
    inner class Rendering {

        @Nested
        inner class IncludedFiles {
            private fun commandLine(vararg paths: Path) = CommandLine(
                emptyMap(), Locations.Temp,
                "basename",
                *paths.map { it.asString() }.toTypedArray()
            )

            @Test
            fun `should contain all files`() {
                expectThat(commandLine(
                    Locations.HomeDirectory,
                    Locations.WorkingDirectory,
                ).includedFiles).containsExactly(
                    Locations.HomeDirectory,
                    Locations.WorkingDirectory,
                )
            }

            @Test
            fun `should hide root`() {
                expectThat(commandLine(
                    Locations.HomeDirectory.root,
                    Locations.WorkingDirectory,
                ).includedFiles).containsExactly(
                    Locations.WorkingDirectory,
                )
            }
        }

        @Test
        fun `should provide summary`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(CommandLine(
                emptyMap(), this,
                "!ls", "-lisa",
                "!mkdir", "-p", "/shared",
                "!mkdir", "-p", "/shared/guestfish.shared/boot",
                "-copy-out", "/boot/cmdline.txt", "/shared/guestfish.shared/boot",
                "!mkdir", "-p", "/shared/guestfish.shared/non",
                "-copy-out", "/non/existing.txt", "/shared/guestfish.shared/non",
            ).summary).matchesCurlyPattern("!ls -lisa !mkdir -p /shared ! … /shared/guestfish.shared/non")
        }
    }
}

val Assertion.Builder<CommandLine>.continuationsRemoved
    get() = get("continuation removed %s") { toString().replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<ManagedProcess>
    get() = get("evaluated %s") {
        toManagedProcess().process({ sync }, Processors.noopProcessor())
    }

fun Assertion.Builder<CommandLine>.evaluated(block: Assertion.Builder<ManagedProcess>.() -> Unit) =
    evaluated.block()

val Assertion.Builder<ManagedProcess>.output
    get() = get("output %s") { output() }

val Assertion.Builder<IOLog>.out
    get() = get("output of type OUT %s") { getCopy().filterIsInstance<IO.OUT>().joinToString(LineSeparators.LF) }

val <P : ManagedProcess> Assertion.Builder<P>.exitState
    get() = get("exit value %s") { exitValue }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String) {
    with(evaluated) {
        output.isEqualTo(expectedOutput)
        50.milliseconds.sleep()
    }
}
