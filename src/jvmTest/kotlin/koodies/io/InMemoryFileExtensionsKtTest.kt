package koodies.io

import koodies.io.file.pathString
import koodies.io.path.deleteOnExit
import koodies.io.path.hasContent
import koodies.logging.InMemoryLogger
import koodies.test.SvgFixture
import koodies.test.UniqueId
import koodies.test.expecting
import koodies.test.withTempDir
import koodies.text.containsEscapeSequences
import org.junit.jupiter.api.Test
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
    fun `should copy to temp`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { SvgFixture.copyToTemp().deleteOnExit() } that {
            fileName.pathString.startsWith("koodies").endsWith(".svg")
            hasContent(SvgFixture.contents.decodeToString())
            parent.isEqualTo(InternalLocations.FilesTemp)
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

    @Test
    fun `should create ASCII art`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val asciiArt = SvgFixture.toAsciiArt(logger)
        expecting { asciiArt } that {
            containsEscapeSequences()
            length.isGreaterThan(1000)
        }
    }
}
