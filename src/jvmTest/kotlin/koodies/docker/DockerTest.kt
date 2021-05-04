package koodies.docker

import koodies.docker.Docker.run
import koodies.docker.TestImages.BusyBox
import koodies.docker.TestImages.Ubuntu
import koodies.exec.Exec
import koodies.exec.ansiKept
import koodies.exec.ansiRemoved
import koodies.exec.io
import koodies.exec.output
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.HtmlFile
import koodies.test.IdeaWorkaroundTest
import koodies.test.Smoke
import koodies.test.SvgFile
import koodies.test.SystemIOExclusive
import koodies.test.UniqueId
import koodies.test.asserting
import koodies.test.copyTo
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.containsEscapeSequences
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isNull
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.length
import strikt.java.exists
import java.nio.file.Path

class DockerTest {

    @Nested
    inner class InfoProperty {

        @DockerRequiring @Test
        fun `should return info for existing key`() {
            expectThat(Docker.info["server.server-version"]).toStringMatchesCurlyPattern("{}.{}.{}")
        }

        @DockerRequiring @Test
        fun `should return null for unknown key`() {
            expectThat(Docker.info["unknown.key"]).isNull()
        }
    }

    @Nested
    inner class ImageProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.images { "hello-world" }).isEqualTo(TestImages.HelloWorld)
        }

        @Test
        fun `should provide commands`() {
            expectCatching { Docker.images.list() }.isSuccess()
        }
    }

    @Nested
    inner class ContainerProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.containers { "docker-container".sanitized }).isEqualTo(DockerContainer("docker-container"))
        }

        @DockerRequiring @Test
        fun `should provide commands`() {
            expectCatching { Docker.containers.list() }.isSuccess()
        }
    }

    @DockerRequiring @Test
    fun `should return true if engine is running`() {
        expectThat(Docker.engineRunning).isTrue()
    }

    @Nested
    inner class ContainerRunning {

        @ContainersTest @IdeaWorkaroundTest
        fun `should return true if container is running`(testContainers: TestContainers) {
            val container = testContainers.newRunningTestContainer()
            expectThat(Docker(container.name).isRunning).isTrue()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container exited`(testContainers: TestContainers) {
            val container = testContainers.newExitedTestContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container does not exist`(testContainers: TestContainers) {
            val container = testContainers.newNotExistentContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }
    }

    @Disabled
    @DockerRequiring([BusyBox::class])
    @Nested
    inner class RunCommand {

        @SystemIOExclusive
        @Test
        fun `should run`() {
            val process = Docker.run {
                image { "busybox" }
                commandLine {
                    command { "echo" }
                    arguments { +"test" }
                }
            }
            expectThat(process.io.output.ansiRemoved).isEqualTo("test")
        }

        @Test
        fun InMemoryLogger.`should use existing logger`() {
            run {
                image { "busybox" }
                commandLine {
                    command { "echo" }
                    arguments { +"test" }
                }
            }
            expectThatLogged().contains("test")
        }
    }

    @DockerRequiring([BusyBox::class])
    @Nested
    inner class StopCommand {

//        @SystemIoExclusive
//        @Test
//        fun `should stop`() {
//            val process = Docker.stop {
//                image { official("busybox") }
//                commandLine {
//                    command { "echo" }
//                    arguments { +"test" }
//                }
//            }
//            expectThat(process.output()).isEqualTo("test")
//        }
//
//        @Test
//        fun InMemoryLogger.`should use existing logger`() {
//            stop {
//                image { official("busybox") }
//                commandLine {
//                    command { "echo" }
//                    arguments { +"test" }
//                }
//            }
//            expectThatLogged().contains("test")
//        }
    }


    @Suppress("SpellCheckingInspection")
    object LibRSvg : DockerImage("minidocks", listOf("librsvg"))

    @Suppress("SpellCheckingInspection")
    object Chafa : DockerImage("rafib", listOf("awesome-cli-binaries"))

    @Nested
    inner class Shortcuts {

        /**
         * Creates a temporary directory with an HTML file inside.
         * @param uniqueId used to make guarantee an isolated directory
         * @param block run with the temporary directory as the receiver and the name of the HTML file as its argument
         */
        private fun withHtmlFile(uniqueId: UniqueId, block: Path.(String) -> Unit) {
            withTempDir(uniqueId) {
                HtmlFile.copyTo(resolve("index.html"))
                block("index.html")
            }
        }

        @DockerRequiring([Ubuntu::class, BusyBox::class]) @TestFactory
        fun `should run using well-known images`(uniqueId: UniqueId) = testEach<Path.(String) -> Exec>(
            { fileName -> ubuntu("cat", fileName) },
            { fileName -> ubuntu { "cat $fileName" } },
            { fileName -> busybox("cat", fileName) },
            { fileName -> busybox { "cat $fileName" } },
        ) { exec ->
            withHtmlFile(uniqueId) { name ->
                expecting { exec(name) } that { io.output.ansiRemoved.isEqualTo(HtmlFile.text) }
            }
        }

        @DockerRequiring([Ubuntu::class]) @TestFactory
        fun `should run command line`(uniqueId: UniqueId) = testEach<Path.(String, String) -> Exec>(
            { command, args -> docker("ubuntu", command, args) },
            { command, args -> docker({ "ubuntu" }, command, args) },
            { command, args -> docker(Ubuntu, command, args) },
        ) { exec ->
            withHtmlFile(uniqueId) { name ->
                HtmlFile.copyTo(resolve(name))
                expecting { exec("cat", name) } that { io.output.ansiRemoved.isEqualTo(HtmlFile.text) }
            }
        }

        @DockerRequiring([Ubuntu::class]) @TestFactory
        fun `should run shell script`(uniqueId: UniqueId) = testEach<Path.(ScriptInitWithWorkingDirectory) -> Exec>(
            { scriptInit -> docker("busybox") { wd -> scriptInit(wd) } },
            { scriptInit -> docker({ "busybox" }) { wd -> scriptInit(wd) } },
            { scriptInit -> docker(BusyBox) { wd -> scriptInit(wd) } },
        ) { exec ->
            withHtmlFile(uniqueId) { name ->
                HtmlFile.copyTo(resolve(name))
                expecting { exec { "cat $name" } } that { io.output.ansiRemoved.isEqualTo(HtmlFile.text) }
            }
        }

        @Suppress("SpellCheckingInspection")
        @DockerRequiring([LibRSvg::class, Chafa::class]) @Smoke @Test
        fun InMemoryLogger.`should run multiple containers`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            SvgFile.copyTo(resolve("koodies.svg"))

            docker(LibRSvg, "-z", 10, "--output", "koodies.png", "koodies.svg")
            resolve("koodies.png") asserting { exists() }

            docker(Chafa, logger = this@`should run multiple containers`) {
                """
               /opt/bin/chafa koodies.png 
                """
            } asserting {
                io.output.ansiKept
                    .containsEscapeSequences()
                    .length.isGreaterThan(1000)
            }
        }
    }
}
