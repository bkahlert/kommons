package koodies.io.path

import koodies.io.copyToDirectory
import koodies.test.HtmlFixture
import koodies.test.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class MimeTypesKtTest {

    @Nested
    inner class GuessMimeType {

        @Test
        fun `should guess mime type`() {
            expectThat("path/file.pdf".asPath().guessMimeType()).isNotNull().isEqualTo("application/pdf")
        }

        @Test
        fun `should return null on no match`() {
            expectThat("path/file".asPath().guessMimeType()).isNull()
        }
    }

    @Nested
    inner class ToBase64 {

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

    @Nested
    inner class ToDataUri {

        @Test
        fun `should create data URI`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val htmlFile = HtmlFixture.copyToDirectory(this)

            @Suppress("SpellCheckingInspection")
            expectThat(htmlFile.toDataUri())
                .isEqualTo("data:text/html;base64,PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkh" +
                    "lbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHkgc3R5bGU9ImJhY2t" +
                    "ncm91bmQ6IHVybCgnZGF0YTppbWFnZS9naWY7YmFzZTY0LFIwbEdPRGRoQVF" +
                    "BREFQQUJBUC8vLy84QUFDd0FBQUFBQVFBREFBQUNBZ3hRQURzPScpIj4KICA" +
                    "gIDxoMT5IZWxsbyBIZWFkbGluZSE8L2gxPgogICAgPHA+SGVsbG8gV29ybGQ" +
                    "hPC9wPgo8L2JvZHk+CjwvaHRtbD4=")
        }
    }
}
