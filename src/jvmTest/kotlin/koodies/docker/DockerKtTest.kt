package koodies.docker


import koodies.docker.Docker.AwesomeCliBinaries
import koodies.docker.Docker.LibRSvg
import koodies.docker.TestImages.BusyBox
import koodies.docker.TestImages.Ubuntu
import koodies.exec.Exec
import koodies.exec.ansiKept
import koodies.exec.ansiRemoved
import koodies.exec.io
import koodies.exec.output
import koodies.io.copyTo
import koodies.io.path.asPath
import koodies.test.junit.UniqueId
import koodies.test.HtmlFixture
import koodies.test.Smoke
import koodies.test.SvgFixture
import koodies.test.asserting
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.ANSI.ansiRemoved
import koodies.text.containsAnsi
import koodies.time.poll
import koodies.time.seconds
import koodies.unit.bytes
import koodies.unit.hasSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import strikt.assertions.length
import strikt.java.exists
import strikt.java.fileName
import java.io.InputStream
import java.net.URI
import java.nio.file.Path

class DockerKtTest {

    /**
     * Creates a temporary directory with an HTML file inside.
     * @param uniqueId used to make guarantee an isolated directory
     * @param block run with the temporary directory as the receiver and the name of the HTML file as its argument
     */
    private fun withHtmlFile(uniqueId: UniqueId, block: Path.(String) -> Unit) {
        withTempDir(uniqueId) {
            HtmlFixture.copyTo(resolve("index.html"))
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
            expecting { exec(name) } that { io.output.ansiRemoved.isEqualTo(HtmlFixture.text) }
        }
    }

    @DockerRequiring([Ubuntu::class]) @TestFactory
    fun `should run command line`(uniqueId: UniqueId) = testEach<Path.(String, String) -> Exec>(
        { command, args -> docker("ubuntu", command, args) },
        { command, args -> docker({ "ubuntu" }, command, args) },
        { command, args -> docker(Ubuntu, command, args) },
    ) { exec ->
        withHtmlFile(uniqueId) { name ->
            HtmlFixture.copyTo(resolve(name))
            expecting { exec("cat", name) } that { io.output.ansiRemoved.isEqualTo(HtmlFixture.text) }
        }
    }

    @DockerRequiring([Ubuntu::class]) @TestFactory
    fun `should run shell script`(uniqueId: UniqueId) = testEach<Path.(ScriptInitWithWorkingDirectory) -> Exec>(
        { scriptInit -> docker("busybox") { wd -> scriptInit(wd) } },
        { scriptInit -> docker({ "busybox" }) { wd -> scriptInit(wd) } },
        { scriptInit -> docker(BusyBox) { wd -> scriptInit(wd) } },
    ) { exec ->
        withHtmlFile(uniqueId) { name ->
            HtmlFixture.copyTo(resolve(name))
            expecting { exec { "cat $name" } } that { io.output.ansiRemoved.isEqualTo(HtmlFixture.text) }
        }
    }

    @DockerRequiring([Ubuntu::class, BusyBox::class]) @TestFactory
    fun `should pass input stream`(uniqueId: UniqueId) = testEach<Path.(InputStream) -> Exec>(
        { fileName -> ubuntu("cat", inputStream = fileName) },
        { fileName -> ubuntu(inputStream = fileName) { "cat" } },
        { fileName -> busybox("cat", inputStream = fileName) },
        { fileName -> busybox(inputStream = fileName) { "cat" } },
        { fileName -> docker(BusyBox, "cat", inputStream = fileName) },
        { fileName -> docker(BusyBox, inputStream = fileName) { "cat" } },
    ) { exec ->
        withTempDir(uniqueId) {
            expecting { exec(HtmlFixture.data.inputStream()) } that { io.output.ansiRemoved.isEqualTo(HtmlFixture.text) }
        }
    }

    @Nested
    inner class Download {

        private val uri = "https://github.com/NicolasCARPi/example-files/raw/master/example.png"

        @DockerRequiring @TestFactory
        fun `should download`(uniqueId: UniqueId) = testEach<Path.(String) -> Path>(
            { download(it) },
            { download(URI.create(it)) },
        ) { download -> withTempDir(uniqueId) { expecting { download(uri) } that { hasSize(40959.bytes) } } }

        @DockerRequiring @TestFactory
        fun `should download use given name`(uniqueId: UniqueId) = testEach<Path.(String, String) -> Path>(
            { uri, name -> download(uri, name) },
            { uri, name -> download(URI.create(uri), name) },
        ) { download -> withTempDir(uniqueId) { expecting { download(uri, "custom.png") } that { fileName.isEqualTo("custom.png".asPath()) } } }

        @DockerRequiring @TestFactory
        fun `should download use remote name`(uniqueId: UniqueId) = testEach<Path.(String) -> Path>(
            { download(it) },
            { download(URI.create(it)) },
        ) { download -> withTempDir(uniqueId) { expecting { download(uri) } that { fileName.isEqualTo("example.png".asPath()) } } }

        @DockerRequiring @TestFactory
        fun `should download clean remote name`(uniqueId: UniqueId) = testEach<Path.(String) -> Path>(
            { download(it) },
            { download(URI.create(it)) },
        ) { download -> withTempDir(uniqueId) { expecting { download("$uri?a=b#c") } that { fileName.isEqualTo("example.png".asPath()) } } }
    }

    @Nested
    inner class Nginx {

        @DockerRequiring([Docker.Nginx::class]) @Test
        fun `should run nginx`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            HtmlFixture.copyTo(resolve("index.html"))
            val nginxProcess = nginx(888)
            val didConnect = poll { curl("-XGET", "host.docker.internal:888").output.ansiRemoved.contains("<head><title>Hello Title!</title>") }
                .every(.5.seconds)
                .forAtMost(8.seconds)
            nginxProcess.kill()
            expectThat(didConnect).isTrue()
        }

        @DockerRequiring([Docker.Nginx::class]) @Test
        fun `should run block when nginx is listening`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            HtmlFixture.copyTo(resolve("index.html"))
            val output = listeningNginx(889) { uri ->
                curl("-XGET", uri).output.ansiRemoved
            }
            expectThat(output).contains("<head><title>Hello Title!</title>")
        }
    }

    @Suppress("SpellCheckingInspection")
    @DockerRequiring([LibRSvg::class, AwesomeCliBinaries::class]) @Smoke @Test
    fun `should run multiple containers`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        SvgFixture.copyTo(resolve("koodies.svg"))

        docker(LibRSvg, "-z", 10, "--output", "koodies.png", "koodies.svg")
        resolve("koodies.png") asserting { exists() }

        docker(AwesomeCliBinaries) {
            """
               /opt/bin/chafa koodies.png 
                """
        } asserting {
            io.output.ansiKept
                .containsAnsi()
                .length.isGreaterThan(1000)
        }
    }
}
