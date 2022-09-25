package com.bkahlert.kommons_deprecated.io.path

import com.bkahlert.kommons.test.copyToDirectory
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons_deprecated.test.withTempDir
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class MimeTypesKtTest {

    @Nested
    inner class GuessMimeType {

        @Test
        fun `should guess mime type`() {
            Paths.get("path/file.pdf").guessMimeType() shouldBe "application/pdf"
        }

        @Test
        fun `should return null on no match`() {
            Paths.get("path/file").guessMimeType() shouldBe null
        }
    }

    @Nested
    inner class ToBase64 {

        @Test
        fun `should encode using Base64`(simpleId: SimpleId) = withTempDir(simpleId) {
            val htmlFile = HtmlDocumentFixture.copyToDirectory(this)

            @Suppress("SpellCheckingInspection")
            htmlFile.toBase64() shouldBe "PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkh" +
                "lbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHkgc3R5bGU9ImJhY2t" +
                "ncm91bmQ6IHVybCgnZGF0YTppbWFnZS9naWY7YmFzZTY0LFIwbEdPRGRoQVF" +
                "BREFQQUJBUC8vLy84QUFDd0FBQUFBQVFBREFBQUNBZ3hRQURzPScpIj4KICA" +
                "gIDxoMT5IZWxsbyBIZWFkbGluZSE8L2gxPgogICAgPHA+SGVsbG8gV29ybGQ" +
                "hPC9wPgo8L2JvZHk+CjwvaHRtbD4="
        }
    }

    @Nested
    inner class ToDataUri {

        @Test
        fun `should create data URI`(simpleId: SimpleId) = withTempDir(simpleId) {
            val htmlFile = HtmlDocumentFixture.copyToDirectory(this)

            @Suppress("SpellCheckingInspection")
            htmlFile.toDataUri() shouldBe "data:text/html;base64,PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkh" +
                "lbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHkgc3R5bGU9ImJhY2t" +
                "ncm91bmQ6IHVybCgnZGF0YTppbWFnZS9naWY7YmFzZTY0LFIwbEdPRGRoQVF" +
                "BREFQQUJBUC8vLy84QUFDd0FBQUFBQVFBREFBQUNBZ3hRQURzPScpIj4KICA" +
                "gIDxoMT5IZWxsbyBIZWFkbGluZSE8L2gxPgogICAgPHA+SGVsbG8gV29ybGQ" +
                "hPC9wPgo8L2JvZHk+CjwvaHRtbD4="
        }
    }
}
