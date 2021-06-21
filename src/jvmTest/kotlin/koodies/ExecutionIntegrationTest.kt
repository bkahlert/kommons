package koodies

import koodies.docker.DockerRequiring
import koodies.docker.docker
import koodies.docker.dockerized
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.IO
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.error
import koodies.exec.exitCode
import koodies.exec.output
import koodies.io.Locations
import koodies.io.copyTo
import koodies.io.ls
import koodies.io.path.deleteRecursively
import koodies.io.path.pathString
import koodies.io.tempDir
import koodies.junit.UniqueId
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectThatLogged
import koodies.logging.logged
import koodies.shell.ShellScript
import koodies.test.HtmlFixture
import koodies.test.Smoke
import koodies.test.SvgFixture
import koodies.test.asserting
import koodies.test.withTempDir
import koodies.text.ANSI.Colors
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.resetLines
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.assertions.length
import strikt.java.exists
import kotlin.io.path.exists

@Smoke @Isolated
class ExecutionIntegrationTest {

    @Test
    fun `should exec command line`() {

        CommandLine("echo", "Hello, World!").exec() check {

            io.output.ansiRemoved { isEqualTo("Hello, World!") }

            io.ansiRemoved {
                matchesCurlyPattern("""
                    Executing echo Hello, World!
                    Hello, World!
                    Process {} terminated successfully at {}
                """.trimIndent())
            }

            exitCode { isEqualTo(0) }
            successful { isTrue() }
        }
    }

    @Test
    fun `should process shell script`() {

        val shellScript = ShellScript {
            echo("Hello, World!")
            echo("Hello, Back!")
        }

        // can also be executed with builder
        var counter = 0
        shellScript.exec.processing { io -> if (io is IO.Output) counter++ } check {

            counter { isEqualTo(2) }

            BACKGROUND { logged.contains("#!(echo Hello, World!;echo Hello, Back!) ✔︎") }
        }
    }

    @Test
    fun `should nicely log`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 0).forEach { echo(it) }
            echo("Take Off")
        }.exec.logging {
            block {
                name { "countdown" }
                contentFormatter { FilteringFormatter { "${"->".ansi.red} $it" } }
                decorationFormatter { Colors.brightRed }
                border = SOLID
            }
        } check {

            BACKGROUND {
                logged.toStringMatchesCurlyPattern("""
                {{}}
                ╭──╴countdown
                │
                │   -> Executing {}
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
                {{}}
            """.trimIndent())
            }
        }
    }

    @Test
    fun `should handle errors`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 7).forEach { echo(it) }
            !"1>&2 echo 'Boom!'"
            !"exit 1"
        }.exec.logging { block { border { Border.NONE } } } check {

            state { isA<Failed>() }

            io.ansiRemoved {
                matchesCurlyPattern("""
                Executing {}
                Countdown!
                10
                9
                8
                7
                Boom!
                Process $pid terminated with exit code $exitCode
                ➜ A dump has been written to:
                  - {}koodies.dump.{}.log (unchanged)
                  - {}koodies.dump.{}.ansi-removed.log (ANSI escape/control sequences removed)
                ➜ The last 8 lines are:
                  Executing {}
                  Countdown!
                  10
                  9
                  8
                  7
                  Boom!
                  Process $pid terminated with exit code $exitCode
            """.trimIndent())
            }
        }
    }

    @Test
    fun `should be simple`() {
        tempDir().apply {

            ShellScript { "cat sample.html" }.exec(this) check {
                io.error.ansiRemoved { contains("cat: sample.html: No such file or directory") }
            }

            HtmlFixture.copyTo(resolve("sample.html"))
            ShellScript { "cat sample.html" }.exec(this) check {
                io.output.ansiRemoved { length.isEqualTo(HtmlFixture.text.length) }
            }

            ls().map { it.fileName } check {
                size { isEqualTo(1) }
                this {
                    any { toStringMatchesCurlyPattern("sample.html") }
                }
            }

            deleteRecursively()
        } check {
            (exists()) { isFalse() }
        }
    }

    @DockerRequiring @Test
    fun `should exec using docker`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        CommandLine("printenv", "HOME").exec() check {
            io.output.ansiRemoved { isEqualTo(Locations.HomeDirectory.pathString) }
        }

        CommandLine("printenv", "HOME").dockerized { "ubuntu" }.exec() check {
            io.output.ansiRemoved { isEqualTo("/root") }
        }

        ShellScript { "printenv | grep HOME | perl -pe 's/.*?HOME=//'" }.dockerized { "ubuntu" }.exec() check {
            io.output.ansiRemoved { isEqualTo("/root") }
        }
    }

    @DockerRequiring @Test
    fun `should exec composed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        SvgFixture.copyTo(resolve("koodies.svg"))

        docker("minidocks/librsvg", "-z", 5, "--output", "koodies.png", "koodies.svg")
        resolve("koodies.png") asserting { exists() }

        // run a shell script
        docker("rafib/awesome-cli-binaries", logger = null) {
            """
               /opt/bin/chafa -c full -w 9 koodies.png
            """
        }.io.output.ansiKept.resetLines().let { println(it) }
    }

    @Test
    fun InMemoryLogger.`should execute using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        val executable: Executable<Exec> = CommandLine("echo", "test")

        with(executable) {
            logging("existing logging context") {
                exec.logging(this) { smart { name by "command line logging context"; decorationFormatter by { Colors.brightBlue.invoke(it) }; border = SOLID } }
            }
        }

        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightMagenta.invoke(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) {
                smart {
                    name by "command line logging context"; decorationFormatter by { Colors.magenta.invoke(it) }; border = SOLID
                }
            }
        }
        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightBlue.invoke(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) {
                smart {
                    name by "command line logging context"; decorationFormatter by { Colors.blue.invoke(it) }; border = DOTTED
                }
            }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightMagenta.invoke(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) {
                smart {
                    name by "command line logging context"; decorationFormatter by { Colors.magenta.invoke(it) }; border = SOLID
                }
            }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightBlue.invoke(it) }) {
            logLine { "abc" }
            executable.exec.logging(this) {
                smart {
                    name by "command line logging context"; decorationFormatter by { Colors.blue.invoke(it) }; border = DOTTED
                }
            }
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
