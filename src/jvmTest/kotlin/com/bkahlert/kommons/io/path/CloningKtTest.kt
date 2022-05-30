package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.randomFile
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException

class CloningKtTest {

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should clone file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).exists().hasContent("cloneFile test")
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should return target`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).isEqualTo(clone).not { isEqualTo(file) }
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should fail on existing target`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile().writeText("cloneFile test")
        val clone = file.resolveSibling("cloned".withRandomSuffix()).writeText("already there")
        expect {
            catching { file.cloneTo(clone) }.isFailure().isA<FileAlreadyExistsException>()
            that(clone).exists().hasContent("already there")
        }
    }
}
