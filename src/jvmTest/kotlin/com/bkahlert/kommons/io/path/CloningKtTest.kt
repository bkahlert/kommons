package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.text.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path

class CloningKtTest {

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should clone file`(@TempDir tempDir: Path) {
        val file = tempDir.createTempFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).exists().hasContent("cloneFile test")
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should return target`(@TempDir tempDir: Path) {
        val file = tempDir.createTempFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).isEqualTo(clone).not { isEqualTo(file) }
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should fail on existing target`(@TempDir tempDir: Path) {
        val file = tempDir.createTempFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix()).writeText("already there")
        expect {
            catching { file.cloneTo(clone) }.isFailure().isA<FileAlreadyExistsException>()
            that(clone).exists().hasContent("already there")
        }
    }
}
