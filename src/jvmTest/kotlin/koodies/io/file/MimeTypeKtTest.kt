package koodies.io.file

import koodies.io.path.toPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@Execution(CONCURRENT)
class MimeTypeKtTest {

    @Test
    fun `should guess mime type`() {
        expectThat("path/file.pdf".toPath().guessedMimeType).isNotNull().isEqualTo("application/pdf")
    }

    @Test
    fun `should return null on no match`() {
        expectThat("path/file".toPath().guessedMimeType).isNull()
    }
}
