package com.bkahlert.kommons

import com.bkahlert.kommons.docker.DockerRequiring
import com.bkahlert.kommons.docker.docker
import com.bkahlert.kommons.docker.dockerized
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Executable
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.exec.RendererProviders
import com.bkahlert.kommons.exec.error
import com.bkahlert.kommons.exec.exitCode
import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.exec.successful
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.copyTo
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.ANSI.Colors
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons.tracing.rendering.Styles.None
import com.bkahlert.kommons.tracing.rendering.Styles.Solid
import com.bkahlert.kommons.tracing.runSpanning
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

@Smoke @Isolated
class ExecutionIntegrationTest {

    @Test
    fun `should exec command line`() {

        CommandLine("echo", "Hello World!").exec() should {

            it.io.output.ansiRemoved shouldBe "Hello World!"

            it.exitCode shouldBe 0
            it.successful shouldBe true
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
        } should {
            it.output.lines().shouldContainExactly("Hello, World!", "Hello, Back!")
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
        ) should {
            rendered() shouldMatchGlob """
                ╭──╴file://*.sh
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
            """.trimIndent()
        }
    }

    @Test
    fun `should handle errors`() {

        ShellScript {
            echo("Countdown!")
            (10 downTo 7).forEach { echo(it) }
            !"1>&2 echo 'Boom!'"
            !"exit 1"
        }.exec.logging(renderer = RendererProviders.noDetails { copy(style = None) }) should {

            it.state.shouldBeInstanceOf<Failed>()

            it.io.ansiRemoved shouldMatchGlob """
                Countdown!
                10
                9
                8
                7
                Boom!
                Process ${it.pid} terminated with exit code ${it.exitCode}
                ➜ A dump has been written to:
                  - *kommons/exec/dump--*.log (unchanged)
                  - *kommons/exec/dump--*.ansi-removed.log (ANSI escape/control sequences removed)
                ➜ The last 7 lines are:
                  Countdown!
                  10
                  9
                  8
                  7
                  Boom!
                  Process ${it.pid} terminated with exit code ${it.exitCode}

            """.trimIndent()
        }
    }

    @Test
    fun `should be simple`() {
        withTempDirectory {

            ShellScript { "cat sample.html" }.exec(this) should {
                it.io.error.ansiRemoved shouldContain "cat: sample.html: No such file or directory"
            }

            HtmlDocumentFixture.copyTo(resolve("sample.html"))
            ShellScript { "cat sample.html" }.exec(this) should {
                it.io.output.ansiRemoved shouldHaveLength HtmlDocumentFixture.contents.length
            }

            listDirectoryEntries().map { it.fileName } should {
                it shouldHaveSize 1
                it.forAny { it.shouldNotBeNull().pathString shouldBe "sample.html" }
            }

            deleteRecursively()
        } should {
            it.shouldNotExist()
        }
    }

    @DockerRequiring @Test
    fun `should exec using docker`(simpleId: SimpleId) = withTempDir(simpleId) {

        CommandLine("printenv", "HOME").exec() should {
            it.io.output.ansiRemoved shouldBe SystemLocations.Home.pathString
        }

        CommandLine("printenv", "HOME").dockerized { "ubuntu" }.exec() should {
            it.io.output.ansiRemoved shouldBe "/root"
        }

        ShellScript { "printenv | grep HOME | perl -pe 's/.*?HOME=//'" }.dockerized { "ubuntu" }.exec() should {
            it.io.output.ansiRemoved shouldBe "/root"
        }
    }

    @DockerRequiring @Test
    @Suppress("SpellCheckingInspection")
    fun `should exec composed`(simpleId: SimpleId) = withTempDir(simpleId) {
        SvgImageFixture.copyTo(resolve("kommons.svg"))

        docker("minidocks/librsvg", "-z", 5, "--output", "kommons.png", "kommons.svg", name = "rasterize vector")
        resolve("kommons.png").shouldExist()

        // run a shell script
        docker("rafib/awesome-cli-binaries", name = "convert to ascii art", renderer = null) {
            """
               /opt/bin/chafa -c full -w 9 kommons.png
            """
        }.io.output.ansiKept.also { println(it) }
    }

    @Test
    fun TestSpanScope.`should execute using existing renderer`(simpleId: SimpleId) = withTempDir(simpleId) {

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

        rendered() shouldMatchGlob """
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
        """.trimIndent()
    }
}
