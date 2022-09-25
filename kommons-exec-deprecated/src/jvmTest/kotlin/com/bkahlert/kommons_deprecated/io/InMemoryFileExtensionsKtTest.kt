package com.bkahlert.kommons_deprecated.io

import com.bkahlert.kommons.io.deleteOnExit
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons_deprecated.docker.DockerRequiring
import com.bkahlert.kommons_deprecated.io.path.hasContent
import com.bkahlert.kommons_deprecated.io.path.pathString
import com.bkahlert.kommons_deprecated.test.expecting
import com.bkahlert.kommons_deprecated.test.withTempDir
import com.bkahlert.kommons_deprecated.text.containsAnsi
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

// TODO delete
class InMemoryFileExtensionsKtTest {

    val file get() = InMemoryImage(SvgImageFixture.name, SvgImageFixture.bytes)

    @Test
    fun `should copy to`(simpleId: SimpleId) = withTempDir(simpleId) {
        expecting { file.copyTo(resolve("image.svg")) } that {
            fileName.pathString.isEqualTo("image.svg")
            hasContent(file.data)
            parent.isEqualTo(this@withTempDir)
        }
    }

    @Test
    fun `should copy to directory`(simpleId: SimpleId) = withTempDir(simpleId) {
        expecting { file.copyToDirectory(this) } that {
            fileName.pathString.isEqualTo(file.name)
            hasContent(file.data)
            parent.isEqualTo(this@withTempDir)
        }
    }

    @Test
    fun `should copy to temp`() {
        expecting { file.copyToTemp().deleteOnExit() } that {
            fileName.pathString.startsWith("kommons").endsWith(".svg")
            hasContent(file.data)
            parent.isEqualTo(com.bkahlert.kommons_deprecated.Kommons.FilesTemp)
        }
    }

    @Test
    fun `should load graphic`(simpleId: SimpleId) = withTempDir(simpleId) {
        val svgFile = file.copyToDirectory(this)
        expecting { svgFile.asImageOrNull() } that { isNotNull() }
    }

    @Test
    fun `should return null on unknown extension`(simpleId: SimpleId) = withTempDir(simpleId) {
        val svgFile = file.copyTo(resolve("image.unknown"))
        expecting { svgFile.asImageOrNull() } that { isNull() }
    }

    @Test
    fun `should return null on directory`(simpleId: SimpleId) = withTempDir(simpleId) {
        val dir = resolve("image.svg").createDirectory()
        expecting { dir.asImageOrNull() } that { isNull() }
    }

    @Slow @DockerRequiring @Test
    fun `should create ASCII art`(simpleId: SimpleId) = withTempDir(simpleId) {
        val asciiArt = file.toAsciiArt()
        expecting { asciiArt } that {
            containsAnsi()
            length.isGreaterThan(1000)
        }
    }
}

val <T : InMemoryFile> Assertion.Builder<T>.name get() = get("name %s") { name }
val <T : InMemoryTextFile> Assertion.Builder<T>.text get() = get("text %s") { text }
val <T : InMemoryFile> Assertion.Builder<T>.data get() = get("data %s") { data }
