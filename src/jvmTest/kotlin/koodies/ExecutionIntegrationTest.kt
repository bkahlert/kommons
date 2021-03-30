package koodies

import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.logged
import koodies.concurrent.process.output
import koodies.concurrent.script
import koodies.debug.CapturedOutput
import koodies.io.path.Locations
import koodies.io.path.Locations.ls
import koodies.io.path.deleteRecursively
import koodies.io.path.tempDir
import koodies.shell.ShellScript
import koodies.test.HtmlFile
import koodies.test.SystemIoExclusive
import koodies.test.copyTo
import koodies.text.ANSI
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.escapeSequencesRemoved
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.pollCatching
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import kotlin.io.path.exists
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(SAME_THREAD) // TODO update readme.md
@SystemIoExclusive
class ExecutionIntegrationTest {

    @Test
    fun `should process`() {
        // simply create a command line
        val commandLine = CommandLine("echo", "Hello, World!") check {
            workingDirectory { isEqualTo(Locations.WorkingDirectory) }
            commandLine { isEqualTo("echo \"Hello, World!\"") }
        }

        // build a process
        val process = commandLine.toProcess() check {
            started { isFalse() }
        }

        // run the process by requesting its output
        process.output() check {
            this { isEqualTo("Hello, World!") }
        }

        // all possible context information available
        process check {
            logged {
                matchesCurlyPattern("""
                    Executing echo "Hello, World!"
                    Hello, World!
                    Process {} terminated successfully at {}
                """.trimIndent())
            }
            exitValue { isEqualTo(0) }
            successful { isTrue() }
        }
    }

    @Test
    fun `should script`(consoleOutput: CapturedOutput) {
        // simply create a shell script
        val shellScript = ShellScript {
            !"echo 'Hello, World!'"
            !"echo 'Hello, Back!'"
        } check {
            // can be run like a process
            (toProcess().output().lines()) { containsExactly("Hello, World!", "Hello, Back!") }
        }

        // can also be executed with builder
        var counter = 0
        shellScript.execute {
            { io ->
                if (io is IO.OUT) counter++
            }
        } check {
            successful { isTrue() }
        }

        counter { isEqualTo(2) }
        consoleOutput.out { contains("Script(name=null;content=echo 'Hello, World!';echo 'Hello, Back!'}) ✔︎") }
    }

    @Test
    fun `should output`(consoleOutput: CapturedOutput) {

        // if output is too boring, you can customize it
        ShellScript {
            !"echo 'Countdown!'"
            (10 downTo 0).forEach { !"echo '$it'" }
            !"echo 'Take Off'"
        }.execute {
            block {
                val arrow = "->".red()
                caption { "countdown" }
                contentFormatter { Formatter { "$arrow $it" } }
                decorationFormatter { ANSI.Colors.brightRed }
                border { yes }
            }
            null
        }

        consoleOutput.out {
            matchesCurlyPattern("""
                ╭──╴countdown
                │   
                │   -> Executing {}
                │   -> {}
                │   -> Countdown!
                │   -> 10
                │   -> 9
                │   -> 8
                │   -> 7
                │   -> 6
                │   -> 5
                │   -> 4
                │   -> 3
                │   -> 2
                │   -> 1
                │   -> 0
                │   -> Take Off
                │   -> Process {} terminated successfully at {}
                │
                ╰──╴✔︎
            """.trimIndent())
        }
    }

    @Test
    fun `should handle errors`(consoleOutput: CapturedOutput) {

        // and if something goes wrong an exception is thrown
        ShellScript {
            !"echo 'Countdown!'"
            (10 downTo 0).forEach { !"echo '$it'" }
            !"echo 'Take Off'"
        }.execute {
            expectedExitValue { -1 }
            processing { async } // ❗️
            null
        }

        // the exception can't be caught during async exec
        // luckily it prints to the console as a fallback
        // with the run script and the complete output linked
        pollCatching {
            (consoleOutput.out.escapeSequencesRemoved.lines()) {
                any { matchesCurlyPattern("⌛️ ➜ A dump has been written to{}") }
                any { matchesCurlyPattern("⌛️ ϟ ProcessExecutionException: Process {} terminated with exit code 0. Expected -1. at{}") }
            }
        }.every(500.milliseconds).forAtMost(5.seconds) { fail { "No exception logged" } }
    }

    @Test
    fun `should be simple`() {
        tempDir().apply {

            runCatching { script { !"cat sample.html" } }.exceptionOrNull() check {
                (message.toString().toLowerCase()) {
                    contains("sample.html")
                    contains("exit code")
                    contains("expected 0")
                }
            }

            HtmlFile.copyTo(resolve("sample.html"))
            val process = script { !"cat sample.html" }

            val content = process.output()
            content check {
                length { isEqualTo(HtmlFile.text.length) }
            }

            ls().map { it.fileName } check {
                size { isEqualTo(6) }
                this {
                    any { toStringMatchesCurlyPattern("koodies.dump.{}.log") }
                    any { toStringMatchesCurlyPattern("koodies.dump.{}.no-ansi.log") }
                    any { toStringMatchesCurlyPattern("koodies.process.{}.sh") }
                    any { toStringMatchesCurlyPattern("sample.html") }
                }
            }

            deleteRecursively()
        } check {
            (exists()) { isFalse() }
        }
    }
}


private operator fun <T> T.invoke(block: Assertion.Builder<T>.() -> Unit): T =
    also { expectThat(this, block) }

private infix fun <T> T?.check(block: T.() -> Unit): T =
    checkNotNull(this).also { it.block() }
