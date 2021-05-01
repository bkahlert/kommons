package koodies

import koodies.collections.size
import koodies.concurrent.process.IO
import koodies.concurrent.process.error
import koodies.concurrent.process.output
import koodies.docker.DockerImage
import koodies.exec.CommandLine
import koodies.exec.Executable
import koodies.exec.Process.ExitState
import koodies.exec.exitCode
import koodies.io.path.Locations
import koodies.io.path.Locations.ls
import koodies.io.path.deleteRecursively
import koodies.io.path.tempDir
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectLogged
import koodies.logging.expectThatLogged
import koodies.shell.ShellScript
import koodies.test.HtmlFile
import koodies.test.SystemIOExclusive
import koodies.test.UniqueId
import koodies.test.copyTo
import koodies.test.withTempDir
import koodies.text.ANSI
import koodies.text.ANSI.Colors
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Formatter
import koodies.text.ansiRemoved
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import strikt.assertions.length
import kotlin.io.path.exists

// TODO update readme.md
@SystemIOExclusive
class ExecutionIntegrationTest {

    @Test
    fun `should process`() {
        // simply create a command line
        val commandLine = CommandLine("echo", "Hello, World!") check {
            workingDirectory { isEqualTo(Locations.WorkingDirectory) }
            commandLine { isEqualTo("echo \"Hello, World!\"") }
        }

        // and execute it
        commandLine.exec() check {

            // just OUT
            io.output.ansiRemoved { isEqualTo("Hello, World!") }

            // or all IO
            io.ansiRemoved {
                matchesCurlyPattern("""
                    Executing echo "Hello, World!"
                    Hello, World!
                    Process {} terminated successfully at {}
                """.trimIndent())
            }

            // further information
            exitCode { isEqualTo(0) }
            successful { isTrue() }
        }
    }

    @Test
    fun `should script`() {
        // simply create a shell script
        val shellScript = ShellScript {
            !"echo 'Hello, World!'"
            !"echo 'Hello, Back!'"
        } check {
            // can be run like a process
            exec().io.output.ansiRemoved { contains("Hello, World!").contains("Hello, Back!") }
        }

        // can also be executed with builder
        var counter = 0
        shellScript.exec.processing { io ->
            if (io is IO.Output) counter++
        } check {
            successful { isTrue() }
        }

        counter { isEqualTo(2) }
        BACKGROUND check { logged.contains("Script(name=null;content=echo 'Hello, World!';echo 'Hello, Back!'}) ✔︎") }
    }

    @Test
    fun `should output`() {

        // if output is too boring, you can customize it
        ShellScript {
            !"echo 'Countdown!'"
            (10 downTo 0).forEach { !"echo '$it'" }
            !"echo 'Take Off'"
        }.exec.logging {
            block {
                val arrow = "->".red()
                caption { "countdown" }
                contentFormatter { Formatter { "$arrow $it" } }
                decorationFormatter { ANSI.Colors.brightRed }
                border = SOLID
            }
        }

        BACKGROUND check {
            expectLogged.toStringMatchesCurlyPattern("""
                {{}}
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
    fun `should handle errors`() {

        // and if something goes wrong, a failed exit state is returned
        ShellScript {
            !"echo 'Countdown!'"
            (10 downTo 7).forEach { !"echo '$it'" }
            !"1>&2 echo 'Boom!'"
            !"exit -1"
        }.exec.logging {
            block { border { Border.NONE } }
        } check {
            exitState { isA<ExitState.Failure>() }
            io.ansiRemoved {
                matchesCurlyPattern("""
                Executing {}
                📄 file://{}
                Countdown!
                10
                9
                8
                7
                Boom!
                Process $pid terminated with exit code $exitCode
                📄 file://{}
                ➜ A dump has been written to:
                  - {}koodies.dump.{}.log (unchanged)
                  - {}koodies.dump.{}.no-ansi.log (ANSI escape/control sequences removed)
                ➜ The last 10 lines are:
                  Executing {}
                  📄 file://{}
                  Countdown!
                  10
                  9
                  8
                  7
                  Boom!
                  Process $pid terminated with exit code $exitCode
                {{}}
            """.trimIndent())
            }
        }
    }

    @Test
    fun `should be simple`() {
        tempDir().apply {

            ShellScript { !"cat sample.html" }.exec(this) check {
                io.error.ansiRemoved { contains("cat: sample.html: No such file or directory") }
            }

            HtmlFile.copyTo(resolve("sample.html"))
            ShellScript { !"cat sample.html" }.exec(this) check {
                io.output.ansiRemoved { length.isEqualTo(HtmlFile.text.length) }
            }

            this.ls().map { it.fileName } check {
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

        // let's define a simple command line
        val commandLine = CommandLine("printenv")

        // and run it
        commandLine.exec() check {
            io.output { size.isGreaterThan(10) }
        }

        // How about running it in a container?
        with(DockerImage { "ubuntu" }) {
            commandLine.exec.dockerized() check {
                (io.output.toList()) { any { ansiRemoved.isEqualTo("HOME=/root") } }
            }
        }
    }

    @Test
    fun docker() {
    }


    @Test
    fun InMemoryLogger.`should execute using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        val executable: Executable = CommandLine(this, "echo", "test")

        with(executable) {
            logging("existing logging context") {
                exec.logging(this) { smart { caption by "command line logging context"; decorationFormatter by { Colors.brightBlue(it) }; border = SOLID } }
            }
        }

        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border = SOLID } }
        }
        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border = DOTTED } }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border = SOLID } }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border = DOTTED } }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   ╭──╴existing logging context
            │   │   
            │   │   ╭──╴command line logging context
            │   │   │   
            │   │   │   Executing echo test
            │   │   │   test
            │   │   │   Process {} terminated successfully at {}.
            │   │   │
            │   │   ╰──╴✔︎
            │   │
            │   ╰──╴✔︎
            │   ╭──╴existing logging context
            │   │   
            │   │   abc
            │   │   ╭──╴command line logging context
            │   │   │   
            │   │   │   Executing echo test
            │   │   │   test
            │   │   │   Process {} terminated successfully at {}.
            │   │   │
            │   │   ╰──╴✔︎
            │   │
            │   ╰──╴✔︎
            │   ╭──╴existing logging context
            │   │   
            │   │   abc
            │   │   ▶ command line logging context
            │   │   · Executing echo test
            │   │   · test
            │   │   · Process {} terminated successfully at {}.
            │   │   ✔︎
            │   │
            │   ╰──╴✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ╭──╴command line logging context
            │   · │   
            │   · │   Executing echo test
            │   · │   test
            │   · │   Process {} terminated successfully at {}.
            │   · │
            │   · ╰──╴✔︎
            │   ✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ▶ command line logging context
            │   · · Executing echo test
            │   · · test
            │   · · Process {} terminated successfully at {}.
            │   · ✔︎
            │   ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}


private operator fun <T> T.invoke(block: Assertion.Builder<T>.() -> Unit): T =
    also { expectThat(this, block) }

private infix fun <T> T?.check(block: T.() -> Unit): T =
    checkNotNull(this).also { it.block() }
