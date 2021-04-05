package koodies

import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.err
import koodies.concurrent.process.io
import koodies.concurrent.process.merged
import koodies.concurrent.process.output
import koodies.concurrent.script
import koodies.debug.CapturedOutput
import koodies.docker.DockerImage
import koodies.docker.executeDockerized
import koodies.io.path.Locations
import koodies.io.path.Locations.ls
import koodies.io.path.deleteRecursively
import koodies.io.path.tempDir
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.shell.ShellScript
import koodies.test.HtmlFile
import koodies.test.SystemIoExclusive
import koodies.test.UniqueId
import koodies.test.copyTo
import koodies.test.withTempDir
import koodies.text.ANSI
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.lines
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.count
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import kotlin.io.path.exists

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
            io.merged.ansiRemoved {
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
                border = SOLID
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

    // TODO exit state
//    @Test
//    fun `should handle errors`(consoleOutput: CapturedOutput) {
//
//        // and if something goes wrong an exception is thrown
//        ShellScript {
//            !"echo 'Countdown!'"
//            (10 downTo 0).forEach { !"echo '$it'" }
//            !"echo 'Take Off'"
//        }.execute {
//            processing { async } // ❗️
//            null
//        }
//
//        // the exception can't be caught during async exec
//        // luckily it prints to the console as a fallback
//        // with the run script and the complete output linked
//        pollCatching {
//            (consoleOutput.out.ansiRemoved.lines()) {
//                any { matchesCurlyPattern("⌛️ ➜ A dump has been written to{}") }
//                any { matchesCurlyPattern("⌛️ ϟ Process {} terminated with exit code 0. Expected -1.") }
//            }
//        }.every(500.milliseconds).forAtMost(5.seconds) { fail { "No exception logged" } }
//    }

    @Test
    fun `should be simple`() {
        tempDir().apply {

            script { !"cat sample.html" } check { io.err.merged.ansiRemoved { contains("cat: sample.html: No such file or directory") } }

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

    @Test
    fun `should process`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        // boring ...
        CommandLine("printenv").execute { null } check { io.merged.ansiRemoved { lines().count().isGreaterThan(10) } }

        // how about that ...
        CommandLine("printenv").executeDockerized(DockerImage { official("ubuntu") }, null) check {
            io.merged.ansiRemoved { lines().count().isGreaterThan(2) }
        }

//        DockerContainer.listContainers
//        "non-existent-container".toContainerName().isRunning
//        DockerContainer.
//        Docker.remove { containers { +"non-existent-container" } }
        // DOcker remove example
        /**
         * koodies.concurrent.process.ProcessExecutionException: Process 32473 terminated with exit code 1. Expected 0.

        ➜ A dump has been written to:
        - file:///Users/bkahlert/Development/com.bkahlert/koodies/koodies.dump.LQG.log (unchanged)
        - file:///Users/bkahlert/Development/com.bkahlert/koodies/koodies.dump.LQG.no-ansi.log (ANSI escape/control sequences removed)
        ➜ The last 4 lines are:
        Executing docker rm koodies.docker.test-container
        Error: No such container: koodies.docker.test-container
        Process 32473 terminated with exit code 1. Expected 0.

         */
    }

    @Test
    fun docker() {
    }
}


private operator fun <T> T.invoke(block: Assertion.Builder<T>.() -> Unit): T =
    also { expectThat(this, block) }

private infix fun <T> T?.check(block: T.() -> Unit): T =
    checkNotNull(this).also { it.block() }
