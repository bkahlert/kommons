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
            .isEqualTo("data:text/html;base64,PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkh" +
                "lbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHk+CiAgICA8aDE+SGV" +
                "sbG8gSGVhZGxpbmUhPC9oMT4KICAgIDxwPkhlbGxvIFdvcmxkITwvcD4KPC9" +
                "ib2R5Pgo8L2h0bWw+")
    }
}
