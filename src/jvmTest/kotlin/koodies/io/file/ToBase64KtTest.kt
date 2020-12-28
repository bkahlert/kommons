package koodies.io.file

import koodies.nio.file.toBase64
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
class ToBase64KtTest {

    @Test
    fun `should encode using Base64`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val htmlFile = HtmlFile.copyToDirectory(this).deleteOnExit()

        @Suppress("SpellCheckingInspection")
        expectThat(htmlFile.toBase64())
            .isEqualTo("PGh0bWw+CjxoZWFkPjx0aXRsZT5IZWxsbyBUaXRsZSE8L3RpdGxlP" +
                "go8L2hlYWQ+Cjxib2R5Pgo8aDE+SGVsbG8gSGVhZGxpbmUhPC9oMT4KPHA+S" +
                "GVsbG8gV29ybGQhPC9wPgo8L2JvZHk+CjwvaHRtbD4=")
    }
}
