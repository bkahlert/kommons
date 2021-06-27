package koodies.io

import koodies.io.file.pathString
import koodies.io.path.deleteOnExit
import koodies.io.path.hasContent
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.SvgFixture
import koodies.test.expecting
import koodies.test.withTempDir
import koodies.text.containsAnsi
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.length
import strikt.assertions.startsWith
import strikt.java.fileName
import strikt.java.parent
import kotlin.io.path.createDirectory

class InMemoryFileExtensionsKtTest {

    @Test
    fun `should copy to`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { SvgFixture.copyTo(resolve("image.svg")) } that {
            fileName.pathString.isEqualTo("image.svg")
            hasContent(SvgFixture.contents.decodeToString())
            parent.isEqualTo(this@withTempDir)
        }
    }

    @Test
    fun `should copy to directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { SvgFixture.copyToDirectory(this) } that {
            fileName.pathString.isEqualTo(SvgFixture.name)
            hasContent(SvgFixture.contents.decodeToString())
            parent.isEqualTo(this@withTempDir)
        }
    }

    @Test
    fun `should copy to temp`() {
        expecting { SvgFixture.copyToTemp().deleteOnExit() } that {
            fileName.pathString.startsWith("koodies").endsWith(".svg")
            hasContent(SvgFixture.contents.decodeToString())
            parent.isEqualTo(Koodies.FilesTemp)
        }
    }

    @Test
    fun `should load graphic`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val svgFile = SvgFixture.copyToDirectory(this)
        expecting { svgFile.asImageOrNull() } that { isNotNull() }
    }

    @Test
    fun `should return null on unknown extension`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val svgFile = SvgFixture.copyTo(resolve("image.unknown"))
        expecting { svgFile.asImageOrNull() } that { isNull() }
    }

    @Test
    fun `should return null on directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = resolve("image.svg").createDirectory()
        expecting { dir.asImageOrNull() } that { isNull() }
    }

    @Slow @Test
    fun TestSpan.`should create ASCII art`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val asciiArt = SvgFixture.toAsciiArt()
        expecting { asciiArt } that {
            containsAnsi()
            length.isGreaterThan(1000)
        }
    }
}

val <T : InMemoryFile> Assertion.Builder<T>.name get() = get("name %s") { name }
val <T : InMemoryTextFile> Assertion.Builder<T>.text get() = get("text %s") { text }
val <T : InMemoryFile> Assertion.Builder<T>.data get() = get("data %s") { data }
