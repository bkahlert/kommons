package koodies

import koodies.docker.DockerRequiring
import koodies.docker.docker
import koodies.docker.dockerized
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.IO
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.RendererProviders
import koodies.exec.error
import koodies.exec.exitCode
import koodies.exec.output
import koodies.exec.successful
import koodies.io.Locations
import koodies.io.copyTo
import koodies.io.ls
import koodies.io.path.deleteRecursively
import koodies.io.path.pathString
import koodies.io.tempDir
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.test.HtmlFixture
import koodies.test.Smoke
import koodies.test.SvgFixture
import koodies.test.asserting
import koodies.test.withTempDir
import koodies.text.ANSI.Colors
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.resetLines
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.TestSpan
import koodies.tracing.rendering.BlockStyles
import koodies.tracing.rendering.BlockStyles.Dotted
import koodies.tracing.rendering.BlockStyles.None
import koodies.tracing.rendering.BlockStyles.Solid
import koodies.tracing.spanning
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

            exitCode { isEqualTo(0) }
            successful { isTrue() }
        }
    }

    @Test
    fun TestSpan.`should process shell script`() {

        val shellScript = ShellScript {
            echo("Hello, World!")
            echo("Hello, Back!")
        }

        // can also be executed with builder
        var counter = 0
        shellScript.exec.processing { io -> if (io is IO.Output) counter++ } check {

            counter { isEqualTo(2) }

            expectThatRendered().matchesCurlyPattern("""
                ╭──╴#!(echo Hello, World!;echo Hello, Back!)
                │
                │   Hello, World!                                                              
                │   Hello, Back!                                                               
                │
                ╰──╴✔︎
            """.trimIndent())
        }
    }

    @Test
    fun TestSpan.`should nicely log`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 0).forEach { echo(it) }
            echo("Take Off")
        }.exec.logging(name = "countdown",
            renderer = RendererProviders.block {
                copy(
                    contentFormatter = { "${"->".ansi.red} $it" },
                    decorationFormatter = Colors.brightRed,
                    blockStyle = BlockStyles.Solid,
                )
            }) check {

            expectThatRendered().matchesCurlyPattern("""
                {{}}
                ╭──╴countdown
                │
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
                │
                ╰──╴✔︎
                {{}}
            """.trimIndent())
        }
    }

    @Test
    fun `should handle errors`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 7).forEach { echo(it) }
            !"1>&2 echo 'Boom!'"
            !"exit 1"
        }.exec.logging(renderer = RendererProviders.noDetails { copy(blockStyle = None) }) check {

            state { isA<Failed>() }

            io.ansiRemoved {
                matchesCurlyPattern("""
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
                ➜ The last 7 lines are:
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
        docker("rafib/awesome-cli-binaries", renderer = null) {
            """
               /opt/bin/chafa -c full -w 9 koodies.png
            """
        }.io.output.ansiKept.resetLines().let { println(it) }
    }

    @Test
    fun TestSpan.`should execute using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        val executable: Executable<Exec> = CommandLine("echo", "test")

        with(executable) {
            spanning("existing logging context") {
                exec.logging(
                    name = "command line logging context",
                    renderer = RendererProviders.compact { copy(decorationFormatter = { Colors.brightBlue.invoke(it) }, blockStyle = Solid) }
                )
            }
        }

        spanning("existing logging context", { it(copy(blockStyle = Solid, decorationFormatter = { Colors.brightMagenta.invoke(it) })) }) {
            log("abc")
            executable.exec.logging(
                name = "command line logging context",
                renderer = RendererProviders.compact { copy(decorationFormatter = { Colors.magenta.invoke(it) }, blockStyle = Solid) }
            )
        }
        spanning("existing logging context", { it(copy(blockStyle = Solid, decorationFormatter = { Colors.brightBlue.invoke(it) })) }) {
            log("abc")
            executable.exec.logging(
                name = "command line logging context",
                renderer = RendererProviders.compact { copy(decorationFormatter = { Colors.blue.invoke(it) }, blockStyle = Dotted) }
            )
        }
        spanning("existing logging context", { it(copy(blockStyle = Dotted, decorationFormatter = { Colors.brightMagenta.invoke(it) })) }) {
            log("abc")
            executable.exec.logging(
                name = "command line logging context",
                renderer = RendererProviders.compact { copy(decorationFormatter = { Colors.magenta.invoke(it) }, blockStyle = Solid) }
            )
        }
        spanning("existing logging context", { it(copy(blockStyle = Dotted, decorationFormatter = { Colors.brightBlue.invoke(it) })) }) {
            log("abc")
            executable.exec.logging(
                name = "command line logging context",
                renderer = RendererProviders.compact { copy(decorationFormatter = { Colors.blue.invoke(it) }, blockStyle = Dotted) }
            )
        }

        expectThatRendered().matchesCurlyPattern("""
            ╭──╴existing logging context
            │
            │   ╭──╴command line logging context
            │   │
            │   │   test                                                                            
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
            ╭──╴existing logging context
            │
            │   abc                                                                             
            │   ╭──╴command line logging context
            │   │
            │   │   test                                                                            
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
            ╭──╴existing logging context
            │
            │   abc                                                                             
            │   ▶ command line logging context
            │   · test                                                                            
            │   ✔︎
            │
            ╰──╴✔︎
            ▶ existing logging context
            · abc                                                                             
            · ╭──╴command line logging context
            · │
            · │   test                                                                            
            · │
            · ╰──╴✔︎
            ✔︎
            ▶ existing logging context
            · abc                                                                             
            · ▶ command line logging context
            · · test                                                                            
            · ✔︎
            ✔︎
        """.trimIndent())
    }
}


private operator fun <T> T.invoke(block: Assertion.Builder<T>.() -> Unit): T =
    also { expectThat(this, block) }

private infix fun <T> T?.check(block: T.() -> Unit): T =
    checkNotNull(this).also { it.block() }
