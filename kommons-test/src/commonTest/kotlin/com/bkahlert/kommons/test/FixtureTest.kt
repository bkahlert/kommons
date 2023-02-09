package com.bkahlert.kommons.test

import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType.Image
import io.ktor.http.ContentType.Text
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets.UTF_8
import kotlin.test.Test

class FixtureTest {

    @Test fun gif_image_fixture() = testAll {
        GifImageFixture.name shouldBe "pixels.gif"
        GifImageFixture.contentType shouldBe Image.GIF
        GifImageFixture.contents.size shouldBe 35
    }

    @Test fun html_document_fixture() = testAll {
        kotlin.runCatching {
            HtmlDocumentFixture.name shouldBe "hello-world.html"
            HtmlDocumentFixture.contentType shouldBe Text.Html.withCharset(UTF_8)
        }.recover { if (it::class.simpleName?.contains("UnsupportedClassVersion") != true) throw it }
    }

    @Test fun svg_image_fixture() = testAll {
        SvgImageFixture.name shouldBe "kommons.svg"
        SvgImageFixture.contentType shouldBe Image.SVG
        SvgImageFixture.contents.length shouldBe 4638
    }

    @Test fun unicode_text_document_fixture() = testAll {
        UnicodeTextDocumentFixture.name shouldBe "unicode.txt"
        UnicodeTextDocumentFixture.contentType shouldBe Text.Plain.withCharset(UTF_8)
        UnicodeTextDocumentFixture.contents.length shouldBe 11
    }

    @Test fun emoji_text_document_fixture() = testAll {
        EmojiTextDocumentFixture.name shouldBe "emoji.txt"
        EmojiTextDocumentFixture.contentType shouldBe Text.Plain.withCharset(UTF_8)
        EmojiTextDocumentFixture.contents shouldBe "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦"
    }
}
