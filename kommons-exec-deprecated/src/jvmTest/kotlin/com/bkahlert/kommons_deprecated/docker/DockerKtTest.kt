package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons.ansiContained
import com.bkahlert.kommons.test.copyTo
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.inputStream
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons_deprecated.docker.Docker.AwesomeCliBinaries
import com.bkahlert.kommons_deprecated.docker.Docker.LibRSvg
import com.bkahlert.kommons_deprecated.docker.TestImages.BusyBox
import com.bkahlert.kommons_deprecated.docker.TestImages.Ubuntu
import com.bkahlert.kommons_deprecated.exec.Exec
import com.bkahlert.kommons_deprecated.exec.output
import com.bkahlert.kommons_deprecated.test.Smoke
import com.bkahlert.kommons_deprecated.test.withTempDir
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.pathString

class DockerKtTest {

    /**
     * Creates a temporary directory with an HTML file inside.
     * @param simpleId used to make guarantee an isolated directory
     * @param block run with the temporary directory as the receiver and the name of the HTML file as its argument
     */
    private fun withHtmlFile(simpleId: SimpleId, block: Path.(String) -> Unit) {
        withTempDir(simpleId) {
            HtmlDocumentFixture.copyTo(resolve("index.html"), overwrite = true)
            block("index.html")
        }
    }

    @DockerRequiring([Ubuntu::class, BusyBox::class]) @TestFactory
    fun `should run using well-known images`(simpleId: SimpleId) = testEach(
        { fileName -> ubuntu("cat", fileName) },
        { fileName -> ubuntu { "cat $fileName" } },
        { fileName -> busybox("cat", fileName) },
        { fileName -> busybox { "cat $fileName" } },
    ) { exec: Path.(String) -> Exec ->
        withHtmlFile(simpleId) { name ->
            exec(name).io.output.ansiRemoved shouldBe HtmlDocumentFixture.contents
        }
    }

    @DockerRequiring([Ubuntu::class]) @TestFactory
    fun `should run command line`(simpleId: SimpleId) = testEach(
        { command, args -> docker("ubuntu", command, args) },
        { command, args -> docker({ "ubuntu" }, command, args) },
        { command, args -> docker(Ubuntu, command, args) },
    ) { exec: Path.(String, String) -> Exec ->
        withHtmlFile(simpleId) { name ->
            HtmlDocumentFixture.copyTo(resolve(name), overwrite = true)
            exec("cat", name).io.output.ansiRemoved shouldBe HtmlDocumentFixture.contents
        }
    }

    @DockerRequiring([Ubuntu::class]) @TestFactory
    fun `should run shell script`(simpleId: SimpleId) = testEach(
        { scriptInit -> docker("busybox") { wd -> scriptInit(wd) } },
        { scriptInit -> docker({ "busybox" }) { wd -> scriptInit(wd) } },
        { scriptInit -> docker(BusyBox) { wd -> scriptInit(wd) } },
    ) { exec: Path.(ScriptInitWithWorkingDirectory) -> Exec ->
        withHtmlFile(simpleId) { name ->
            HtmlDocumentFixture.copyTo(resolve(name), overwrite = true)
            exec { "cat $name" }.io.output.ansiRemoved shouldBe HtmlDocumentFixture.contents
        }
    }

    @DockerRequiring([Ubuntu::class, BusyBox::class]) @TestFactory
    fun `should pass input stream`(simpleId: SimpleId) = testEach(
        { fileName -> ubuntu("cat", inputStream = fileName) },
        { fileName -> ubuntu(inputStream = fileName) { "cat" } },
        { fileName -> busybox("cat", inputStream = fileName) },
        { fileName -> busybox(inputStream = fileName) { "cat" } },
        { fileName -> docker(BusyBox, "cat", inputStream = fileName) },
        { fileName -> docker(BusyBox, inputStream = fileName) { "cat" } },
    ) { exec: Path.(InputStream) -> Exec ->
        withTempDir(simpleId) {
            exec(HtmlDocumentFixture.inputStream()).io.output.ansiRemoved shouldBe HtmlDocumentFixture.contents
        }
    }

    @Nested
    inner class Download {

        private val uri = "https://github.com/NicolasCARPi/example-files/raw/master/example.png"

        @DockerRequiring @TestFactory
        fun `should download`(simpleId: SimpleId) = testEach(
            { download(it) },
            { download(URI.create(it)) },
        ) { download: Path.(String) -> Path ->
            withTempDir(simpleId) { download(uri).fileSize() shouldBe 40959L }
        }

        @DockerRequiring @TestFactory
        fun `should download use given name`(simpleId: SimpleId) = testEach(
            { uri, name -> download(uri, name) },
            { uri, name -> download(URI.create(uri), name) },
        ) { download: Path.(String, String) -> Path ->
            withTempDir(simpleId) { download(uri, "custom.png").fileName.pathString shouldBe "custom.png" }
        }

        @DockerRequiring @TestFactory
        fun `should download use remote name`(simpleId: SimpleId) = testEach(
            { download(it) },
            { download(URI.create(it)) },
        ) { download: Path.(String) -> Path ->
            withTempDir(simpleId) { download(uri).fileName.pathString shouldBe "example.png" }
        }

        @DockerRequiring @TestFactory
        fun `should download clean remote name`(simpleId: SimpleId) = testEach(
            { download(it) },
            { download(URI.create(it)) },
        ) { download: Path.(String) -> Path ->
            withTempDir(simpleId) { download("$uri?a=b#c").fileName.pathString shouldBe "example.png" }
        }
    }

    @Suppress("SpellCheckingInspection")
    @DockerRequiring([LibRSvg::class, AwesomeCliBinaries::class]) @Smoke @Test
    fun `should run multiple containers`(simpleId: SimpleId) = withTempDir(simpleId) {
        SvgImageFixture.copyTo(resolve("kommons.svg"))

        docker(LibRSvg, "-z", 10, "--output", "kommons.png", "kommons.svg")
        resolve("kommons.png").shouldExist()

        docker(AwesomeCliBinaries) {
            """
               /opt/bin/chafa kommons.png 
                """
        }.io.output.ansiKept should {
            it.ansiContained shouldBe true
            it.length shouldBeGreaterThan 1000
        }
    }
}
