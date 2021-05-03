package koodies.io.path

import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
