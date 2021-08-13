package koodies

import koodies.docker.DockerRequiring
import koodies.docker.docker
import koodies.docker.dockerized
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.RendererProviders
import koodies.exec.error
import koodies.exec.exitCode
import koodies.exec.output
import koodies.exec.successful
import koodies.io.copyTo
import koodies.io.path.Locations
import koodies.io.path.deleteRecursively
import koodies.io.path.ls
import koodies.io.path.pathString
import koodies.io.path.tempDir
import koodies.shell.ShellScript
import koodies.test.HtmlFixture
import koodies.test.Smoke
import koodies.test.SvgFixture
import koodies.test.asserting
import koodies.test.junit.UniqueId
import koodies.test.withTempDir
import koodies.text.ANSI.Colors
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.resetLines
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.TestSpanScope
import koodies.tracing.rendering.Styles.Dotted
import koodies.tracing.rendering.Styles.None
import koodies.tracing.rendering.Styles.Solid
import koodies.tracing.runSpanning
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
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
    fun `should process shell script`() {

        val shellScript = ShellScript("Say Hello") {
            echo("Hello, World!")
            echo("Hello, Back!")
        }

        val output = mutableListOf<String>()
        shellScript.exec.processing { _, callback ->
            callback { io -> output.add(io.toString()) }
        } check {
            output { containsExactly("Hello, World!", "Hello, Back!") }
        }
    }

    @Test
    fun TestSpanScope.`should log`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 0).forEach { echo(it) }
            echo("Take Off")
        }.exec.logging(
            contentFormatter = { "${"->".ansi.red} $it" },
            decorationFormatter = Colors.brightRed,
            style = Solid,
        ) check {
            expectThatRendered().matchesCurlyPattern("""
                ╭──╴file://{}.sh
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
        }.exec.logging(renderer = RendererProviders.noDetails { copy(style = None) }) check {

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
                  - {}koodies/exec/dump--{}.log (unchanged)
                  - {}koodies/exec/dump--{}.ansi-removed.log (ANSI escape/control sequences removed)
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
    @Suppress("SpellCheckingInspection")
    fun `should exec composed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        SvgFixture.copyTo(resolve("koodies.svg"))

        docker("minidocks/librsvg", "-z", 5, "--output", "koodies.png", "koodies.svg", name = "rasterize vector")
        resolve("koodies.png") asserting { exists() }

        // run a shell script
        docker("rafib/awesome-cli-binaries", name = "convert to ascii art", renderer = null) {
            """
               /opt/bin/chafa -c full -w 9 koodies.png
            """
        }.io.output.ansiKept.resetLines().let { println(it) }
    }

    @Test
    fun TestSpanScope.`should execute using existing renderer`(uniqueId: UniqueId) = withTempDir(uniqueId) {

        val executable: Executable<Exec> = CommandLine("echo", "test")

        with(executable) {
            runSpanning("existing logging context") {
                exec.logging(
                    renderer = RendererProviders.compact { copy(decorationFormatter = { it.ansi.brightBlue }, style = Solid) }
                )
            }
        }

        runSpanning("existing logging context", decorationFormatter = { it.ansi.brightMagenta }, style = Solid) {
            log("abc")
            executable.exec.logging(decorationFormatter = { it.ansi.magenta }, style = Solid)
        }
        runSpanning("existing logging context", decorationFormatter = { it.ansi.brightBlue }, style = Solid) {
            log("abc")
            executable.exec.logging(decorationFormatter = { it.ansi.blue }, style = Dotted)
        }
        runSpanning("existing logging context", decorationFormatter = { it.ansi.brightMagenta }, style = Dotted) {
            log("abc")
            executable.exec.logging(decorationFormatter = { it.ansi.magenta }, style = Solid)
        }
        runSpanning("existing logging context", decorationFormatter = { it.ansi.brightBlue }, style = Dotted) {
            log("abc")
            executable.exec.logging(decorationFormatter = { it.ansi.blue }, style = Dotted)
        }

        expectThatRendered().matchesCurlyPattern("""
            ╭──╴existing logging context
            │
            │   ╭──╴echo test
            │   │
            │   │   test
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
            ╭──╴existing logging context
            │
            │   abc
            │   ╭──╴echo test
            │   │
            │   │   test
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
            ╭──╴existing logging context
            │
            │   abc
            │   ▶ echo test
            │   · test
            │   ✔︎
            │
            ╰──╴✔︎
            ▶ existing logging context
            · abc
            · ╭──╴echo test
            · │
            · │   test
            · │
            · ╰──╴✔︎
            ✔︎
            ▶ existing logging context
            · abc
            · ▶ echo test
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
