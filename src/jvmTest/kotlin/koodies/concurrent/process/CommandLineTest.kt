package koodies.concurrent.process

import koodies.concurrent.output
import koodies.io.path.asString
import koodies.io.path.randomPath
import koodies.test.matchesCurlyPattern
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.quoted
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class CommandLineTest {

    @Nested
    inner class Equality {
        @Test
        fun `should equal based on command and arguments`() = withTempDir {
            val cmdLine1 = CommandLine(emptyMap(), this, "/bin/command", "arg1", "arg 2")
            val cmdLine2 = CommandLine(emptyMap(), this, Path.of("/bin/command"), "arg1", "arg 2")
            expectThat(cmdLine1).isEqualTo(cmdLine2)
        }
    }

    @Nested
    inner class LazyStartedProcess {

        @Test
        fun `should not start on its own`() = withTempDir {
            val (_, file) = createLazyFileCreatingProcess()
            2.seconds.sleep()
            expectThat(file).not { exists() }
        }

        @Test
        fun `should start if accessed`() = withTempDir {
            val (process, file) = createLazyFileCreatingProcess()
            process.start()
            2.seconds.sleep()
            expectThat(file).exists()
        }

        @Test
        fun `should start if processed`() = withTempDir {
            val (process, file) = createLazyFileCreatingProcess()
            process.silentlyProcess()
            2.seconds.sleep()
            expectThat(file).exists()
        }
    }

    @Test
    fun `should run`() = withTempDir {
        val command = CommandLine(emptyMap(), this, "echo", "test")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo test")
            evaluatesTo("test", 0)
        }
    }

    @Test
    fun `should run with more arguments`() = withTempDir {
        val command = CommandLine(emptyMap(), this, "echo", "one", "two", "three")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo one two three")
            evaluatesTo("one two three", 0)
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `should expand`() = withTempDir {
            val command = CommandLine(emptyMap(), this, "echo", "\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \$HOME")
                evaluatesTo(System.getProperty("user.home"), 0)
            }
        }

        @Test
        fun `should not expand`() = withTempDir {
            val command = CommandLine(emptyMap(), this, "echo", "\\\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \\\$HOME")
                evaluated {
                    not { output.isEqualTo(System.getProperty("user.home")) }
                    exitValue.isEqualTo(0)
                }
            }
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `should output formatted`() = withTempDir {
            expectThat(CommandLine(emptyMap(), this, "command", "-a", "--bee", "c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            c \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle whitespaces correctly command`() = withTempDir {
            expectThat(CommandLine(emptyMap(), this, "command", " - a", "    ", "c c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            "- a" \
             \
            "c c" \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle nesting`() = withTempDir {
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
        fun `should not quote unnecessarily`() = withTempDir {
            val command = CommandLine(emptyMap(), this, "echo", "Hello")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo Hello")
                evaluatesTo("Hello", 0)
            }
        }

        @Test
        fun `should quote on whitespaces`() = withTempDir {
            val command = CommandLine(emptyMap(), this, "echo", "Hello World!")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"Hello World!\"")
                evaluatesTo("Hello World!", 0)
            }
        }

        @Test
        fun `should support single quotes`() = withTempDir {
            val command = CommandLine(emptyMap(), this, "echo", "'\$HOME'")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo '\$HOME'")
                evaluatesTo("\$HOME", 0)
            }
        }
    }

    @Nested
    inner class Nesting {

        @Test
        fun `should produce runnable output`() = withTempDir {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "Hello")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo Hello\"")
                evaluated {
                    output.isEqualTo("echo Hello")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("Hello", 0)
                }
            }
        }

        @Test
        fun `should produce runnable quoted output`() = withTempDir {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "Hello World!")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"Hello World!\\\"\"")
                evaluated {
                    output.isEqualTo("echo \"Hello World!\"")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("Hello World!", 0)
                }
            }
        }

        @Test
        fun `should produce runnable single quoted output`() = withTempDir {
            val nestedCommand = CommandLine(emptyMap(), this, "echo", "'Hello World!'")
            val command = CommandLine(emptyMap(), this, "echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"'Hello World!'\\\"\"")
                evaluated {
                    output.isEqualTo("echo \"'Hello World!'\"")
                    log.out.get { CommandLine.parse(this, this@withTempDir) }.evaluatesTo("'Hello World!'", 0)
                }
            }
        }
    }

    @Test
    fun `should provide summary`() = withTempDir {
        expectThat(CommandLine(
            emptyMap(), this,
            "!ls", "-lisa",
            "!mkdir", "-p", "/shared",
            "!mkdir", "-p", "/shared/guestfish.shared/boot",
            "-copy-out", "/boot/cmdline.txt", "/shared/guestfish.shared/boot",
            "!mkdir", "-p", "/shared/guestfish.shared/non",
            "-copy-out", "/non/existing.txt", "/shared/guestfish.shared/non",
        ).summary).matchesCurlyPattern("◀◀ lisa  ◀ mkdir  ◀ …  ◀ mkdir")
    }
}

val Assertion.Builder<CommandLine>.continuationsRemoved
    get() = get("continuation removed %s") { toString().replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<ManagedProcess>
    get() = get("evaluated %s") {
        toManagedProcess(expectedExitValue = null).processSynchronously()
    }

fun Assertion.Builder<CommandLine>.evaluated(block: Assertion.Builder<ManagedProcess>.() -> Unit) =
    evaluated.block()

val Assertion.Builder<ManagedProcess>.output
    get() = get("output %s") { output() }

val Assertion.Builder<IOLog>.out
    get() = get("output of type OUT %s") { logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) }

val <P : ManagedProcess> Assertion.Builder<P>.exitValue
    get() = get("exit value %s") { exitValue }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String, expectedExitValue: Int) {
    with(evaluated) {
        output.isEqualTo(expectedOutput)
        50.milliseconds.sleep()
        exitValue.isEqualTo(expectedExitValue)
    }
}

fun Path.createLazyFileCreatingProcess(): Pair<ManagedProcess, Path> {
    val nonExistingFile = randomPath(extension = ".txt")
    val fileCreatingCommandLine = CommandLine(emptyMap(), this, "touch", nonExistingFile.asString())
    return fileCreatingCommandLine.toManagedProcess() to nonExistingFile
}
