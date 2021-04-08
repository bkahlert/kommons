package koodies.io

import koodies.io.path.withDirectoriesCreated
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.java.exists
import strikt.java.parent

@Execution(CONCURRENT)
class PathKtTest {

    @Nested
    inner class WithMissingDirectoriesCreated {

        @Test
        fun `should create missing directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("some/dir/some/file")
            expectThat(file.withDirectoriesCreated()).parent.isNotNull().exists()
        }
    }
}
