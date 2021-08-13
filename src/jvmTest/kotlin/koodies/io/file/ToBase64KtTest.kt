package koodies.io.file

import koodies.io.copyToDirectory
import koodies.test.junit.UniqueId
import koodies.nio.file.toBase64
import koodies.test.HtmlFixture
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ToBase64KtTest {

    @Test
    fun `should encode using Base64`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val htmlFile = HtmlFixture.copyToDirectory(this)

        @Suppress("SpellCheckingInspection")
        expectThat(htmlFile.toBase64())
            .isEqualTo("PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkh" +
                "lbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHkgc3R5bGU9ImJhY2t" +
                "ncm91bmQ6IHVybCgnZGF0YTppbWFnZS9naWY7YmFzZTY0LFIwbEdPRGRoQVF" +
                "BREFQQUJBUC8vLy84QUFDd0FBQUFBQVFBREFBQUNBZ3hRQURzPScpIj4KICA" +
                "gIDxoMT5IZWxsbyBIZWFkbGluZSE8L2gxPgogICAgPHA+SGVsbG8gV29ybGQ" +
                "hPC9wPgo8L2JvZHk+CjwvaHRtbD4=")
    }
}
