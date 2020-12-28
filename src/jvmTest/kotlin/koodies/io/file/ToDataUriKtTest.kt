package koodies.io.file

import koodies.runtime.deleteOnExit
import koodies.test.Fixtures.copyToDirectory
import koodies.test.HtmlFile
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToDataUriKtTest {

    @Test
    fun `should create data URI`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val htmlFile = HtmlFile.copyToDirectory(this).deleteOnExit()

        @Suppress("SpellCheckingInspection")
        expectThat(htmlFile.toDataUri())
            .isEqualTo("data:text/html;base64,PGh0bWw+CjxoZWFkPjx0aXRsZT5IZWx" +
                "sbyBUaXRsZSE8L3RpdGxlPgo8L2hlYWQ+Cjxib2R5Pgo8aDE+SGVsbG8gSGV" +
                "hZGxpbmUhPC9oMT4KPHA+SGVsbG8gV29ybGQhPC9wPgo8L2JvZHk+CjwvaHR" +
                "tbD4=")
    }
}
