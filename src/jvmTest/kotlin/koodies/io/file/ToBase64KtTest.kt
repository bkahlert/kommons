package koodies.io.file

import koodies.jvm.deleteOnExit
import koodies.nio.file.toBase64
import koodies.test.HtmlFile
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ToBase64KtTest {

    @Test
    fun `should encode using Base64`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val htmlFile = deleteOnExit(HtmlFile.copyToDirectory(this))

        @Suppress("SpellCheckingInspection")
        expectThat(htmlFile.toBase64())
            .isEqualTo("PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkhlbGxvIFRpdGxlITwvdGl0b" +
                "GU+CjwvaGVhZD4KPGJvZHk+CiAgICA8aDE+SGVsbG8gSGVhZGxpbmUhPC9oM" +
                "T4KICAgIDxwPkhlbGxvIFdvcmxkITwvcD4KPC9ib2R5Pgo8L2h0bWw+")
    }
}
