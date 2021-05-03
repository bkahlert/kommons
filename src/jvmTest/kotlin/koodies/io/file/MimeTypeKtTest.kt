package koodies.io.file

import koodies.io.path.asPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull


class MimeTypeKtTest {

    @Test
    fun `should guess mime type`() {
        expectThat("path/file.pdf".asPath().guessedMimeType).isNotNull().isEqualTo("application/pdf")
    }

    @Test
    fun `should return null on no match`() {
        expectThat("path/file".asPath().guessedMimeType).isNull()
    }
}
