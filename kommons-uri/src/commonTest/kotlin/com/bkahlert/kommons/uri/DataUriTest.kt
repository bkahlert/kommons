package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.ContentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.name
import kotlin.test.Test

class DataUriTest {

    @Test
    fun instantiation() = testAll {
        DataUri(ContentType.Text.Plain, "foo".encodeToByteArray()) should {
            it.mediaType shouldBe ContentType.Text.Plain
            it.data shouldBe "foo".encodeToByteArray()
        }
    }

    @Test
    fun equality() = testAll {
        DataUri(ContentType.Text.Plain, "foo".encodeToByteArray()) should {
            it shouldBe DataUri(ContentType.Text.Plain, "foo".encodeToByteArray())
            it shouldNotBe DataUri(ContentType.Text.CSS, "foo".encodeToByteArray())
            it shouldNotBe DataUri(ContentType.Text.Plain, "bar".encodeToByteArray())
        }
    }

    @Test
    fun media_type_string() = testAll {
        DataUri(ContentType.Text.Plain.withCharset(DataUri.DEFAULT_CHARSET), asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET)) should {
            it.mediaTypeString shouldBe "text/plain;charset=${DataUri.DEFAULT_CHARSET.name}"
        }
    }

    @Test
    fun to_string() = testAll {
        DataUri(null, asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET)) should {
            it.toString() shouldBe "data:;base64,$asciiBase64Text"
        }
        DataUri(ContentType.Text.Plain, asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET)) should {
            it.toString() shouldBe "data:text/plain;base64,$asciiBase64Text"
        }
        DataUri(ContentType.Text.Plain.withCharset(DataUri.DEFAULT_CHARSET), asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET)) should {
            it.toString() shouldBe "data:text/plain;charset=${DataUri.DEFAULT_CHARSET.name};base64,$asciiBase64Text"
        }
    }

    @Test
    fun regex() = testAll {
        DataUri.REGEX.shouldNotBeNull()
    }

    @Test
    fun parse_ascii_text() = testAll {
        DataUri.parse("data:,$asciiEncodedText") should {
            it shouldBe DataUri(null, asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET))
        }
        DataUri.parse("data:text/plain,$asciiEncodedText") should {
            it shouldBe DataUri(ContentType.Text.Plain, asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET))
        }

        DataUri.parse("data:text/plain;charset=${DataUri.DEFAULT_CHARSET.name},$asciiEncodedText") should {
            it shouldBe DataUri(ContentType.Text.Plain.withCharset(DataUri.DEFAULT_CHARSET), asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET))
        }
    }

    @Test
    fun parse_unicode_text() = testAll {
        val charset = Charset.forName("utf-8")
        DataUri.parse("data:text/plain;charset=${charset.name},${unicodeText.encodeURLPathPart()}") should {
            it shouldBe DataUri(ContentType.Text.Plain.withCharset(charset), unicodeText.encodeToByteArray(charset))
        }
        DataUri.parse("data:text/plain;charset=${charset.name},${emojiText.encodeURLPathPart()}") should {
            it shouldBe DataUri(ContentType.Text.Plain.withCharset(charset), emojiText.encodeToByteArray(charset))
        }
    }

    @Test
    fun parse_base64_unicode_text() = testAll {
        val charset = Charset.forName("utf-8")
        DataUri.parse("data:;base64,$asciiBase64Text") should {
            it shouldBe DataUri(null, asciiText.encodeToByteArray(DataUri.DEFAULT_CHARSET))
        }
        DataUri.parse("data:text/plain;charset=UTF-8;base64,YcKF8J2Vkw0K4piwCvCfkYsK") should {
            it shouldBe DataUri(ContentType.Text.Plain.withCharset(charset), unicodeText.encodeToByteArray(charset))
        }
        DataUri.parse("data:text/plain;charset=UTF-8;base64,YfCdlZPwn6ug8J%2BHqfCfh6rwn5Go8J%2BPvuKAjfCfprHwn5Gp4oCN8J%2BRqeKAjfCfkabigI3wn5Gm") should {
            it shouldBe DataUri(ContentType.Text.Plain.withCharset(charset), emojiText.encodeToByteArray(charset))
        }
    }

    @Test
    fun parse_base64_image() = testAll {
        DataUri.parse(svgImageBase64Encoded.replace(";charset=UTF-8", "")) should {
            it shouldBe DataUri(ContentType.Image.SVG, svgImage.encodeToByteArray(Charset.forName("utf-8")))
        }
        DataUri.parse("data:image/gif;base64,R0lGODdhAQADAPABAP%2F%2F%2F%2F8AACwAAAAAAQADAAACAgxQADs") should {
            it shouldBe DataUri(ContentType.Image.GIF, gifImage)
        }
    }

    @Test
    fun parse_base64_unicode_image() = testAll {
        val charset = Charset.forName("utf-8")
        DataUri.parse(svgImageBase64Encoded) should {
            it shouldBe DataUri(ContentType.Image.SVG.withCharset(charset), svgImage.encodeToByteArray(charset))
        }
    }

    @Test
    fun parse_invalid() = testAll {
        shouldThrow<IllegalArgumentException> { DataUri.parse("data:") }.message shouldBe "data: is no valid data URI"
        DataUri.parseOrNull("data:").shouldBeNull()
    }

    companion object {
        const val asciiText = " !0@Az{|}~"
        const val asciiEncodedText = "%20%210%40Az%7B%7C%7D~"
        const val asciiBase64Text = "ICEwQEF6e3x9fg"
        const val asciiDataUri = "data:;base64,$asciiBase64Text"

        val unicodeText = UnicodeTextDocumentFixture.contents
        val emojiText = EmojiTextDocumentFixture.contents
        val svgImage = SvgImageFixture.contents.replace("</svg>", "<text>$emojiText</text></svg>")
        val svgImageBase64Encoded = "data:image/svg+xml" +
            ";charset=UTF-8" +
            ";base64," +
            "PHN2ZyB2ZXJzaW9uPSIxLjEi" +
            "IHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxp" +
            "bms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIGFyaWEtbGFiZWw9" +
            "IktvbW1vbnMiIHJvbGU9ImltZyIgdmlld0JveD0iMCAwIDM3NSA2MCIgc3R5" +
            "bGU9ImN1cnNvcjogZGVmYXVsdDsiPgogICAgPHN0eWxlPgogICAgICAgIHBh" +
            "dGggeyBmaWxsOiAjMjMxRjIwOyB9CiAgICAgICAgQG1lZGlhIChwcmVmZXJz" +
            "LWNvbG9yLXNjaGVtZTogZGFyaykgeyB0ZXh0IHsgY29sb3I6ICNEQ0UwREY7" +
            "IH0gfQogICAgPC9zdHlsZT4KICAgIDxkZWZzPgogICAgICAgIDxsaW5lYXJH" +
            "cmFkaWVudCBpZD0idXBwZXItayIgeDE9Ii03MjAiIHkxPSI3ODAiIHgyPSIw" +
            "IiB5Mj0iNjAiPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9IjAiIHN0b3At" +
            "Y29sb3I9IiMyOWFiZTIiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIu" +
            "MSIgc3RvcC1jb2xvcj0iI2EzNWViYiIvPgogICAgICAgICAgICA8c3RvcCBv" +
            "ZmZzZXQ9Ii4yIiBzdG9wLWNvbG9yPSIjMDA5MGFhIi8%2BCiAgICAgICAgIC" +
            "AgIDxzdG9wIG9mZnNldD0iLjMiIHN0b3AtY29sb3I9IiMwMTgxOGYiLz4KIC" +
            "AgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuMyIgc3RvcC1jb2xvcj0iIzAxOD" +
            "E4ZiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii40NSIgc3RvcC1jb2" +
            "xvcj0iIzAwOTc4YiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii42Ii" +
            "BzdG9wLWNvbG9yPSIjNjQ4YmUwIi8%2BCiAgICAgICAgICAgIDxzdG9wIG9m" +
            "ZnNldD0iLjciIHN0b3AtY29sb3I9IiMwMDllYzYiLz4KICAgICAgICAgICAg" +
            "PHN0b3Agb2Zmc2V0PSIuOCIgc3RvcC1jb2xvcj0iIzI5YWJlMiIvPgogICAg" +
            "ICAgICAgICA8c3RvcCBvZmZzZXQ9Ii45IiBzdG9wLWNvbG9yPSIjMDRhOTcx" +
            "Ii8%2BCiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj" +
            "0iIzA0YTk3MSIvPgogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW" +
            "1lPSJ4MSIgZHVyPSI2MHMiIHZhbHVlcz0iMDsgLTcyMDsgMCIgcmVwZWF0Q2" +
            "91bnQ9ImluZGVmaW5pdGUiLz4KICAgICAgICAgICAgPGFuaW1hdGUgYXR0cm" +
            "lidXRlTmFtZT0ieTEiIGR1cj0iNjBzIiB2YWx1ZXM9IjYwOyA3ODA7IDYwIi" +
            "ByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogICAgICAgICAgICA8YW5pbW" +
            "F0ZSBhdHRyaWJ1dGVOYW1lPSJ4MiIgZHVyPSI2MHMiIHZhbHVlcz0iNzIwOy" +
            "AwOyA3MjAiIHJlcGVhdENvdW50PSJpbmRlZmluaXRlIi8%2BCiAgICAgICAg" +
            "ICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9InkyIiBkdXI9IjYwcyIgdmFs" +
            "dWVzPSItNjYwOyA2MDsgLTY2MCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUi" +
            "Lz4KICAgICAgICA8L2xpbmVhckdyYWRpZW50PgogICAgICAgIDxsaW5lYXJH" +
            "cmFkaWVudCBpZD0ic3RyaXAiIHgxPSIwIiB5MT0iNjAiIHgyPSIzMDAiIHky" +
            "PSItMjQwIj4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNv" +
            "bG9yPSIjQzc1N0JDIi8%2BCiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iLj" +
            "AyNSIgc3RvcC1jb2xvcj0iI0QwNjA5QSIvPgogICAgICAgICAgICA8c3RvcC" +
            "BvZmZzZXQ9Ii4wNSIgc3RvcC1jb2xvcj0iI0UxNzI1QyIvPgogICAgICAgIC" +
            "AgICA8c3RvcCBvZmZzZXQ9Ii4wNzUiIHN0b3AtY29sb3I9IiNFRTdFMkYiLz" +
            "4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuMSIgc3RvcC1jb2xvcj0iI0" +
            "Y1ODYxMyIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii4yIiBzdG9wLW" +
            "NvbG9yPSIjRjg4OTA5Ii8%2BCiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0i" +
            "LjUiIHN0b3AtY29sb3I9IiNmZjkyMzQiLz4KICAgICAgICAgICAgPHN0b3Ag" +
            "b2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjZWJiMjFkIi8%2BCiAgICAgICAgIC" +
            "AgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9IngxIiBkdXI9IjIwcyIgdmFsdW" +
            "VzPSIwOyAtMzAwOyAwIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogIC" +
            "AgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ5MSIgZHVyPSIyMH" +
            "MiIHZhbHVlcz0iNjA7IDM2MDsgNjAiIHJlcGVhdENvdW50PSJpbmRlZmluaX" +
            "RlIi8%2BCiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9Ingy" +
            "IiBkdXI9IjIwcyIgdmFsdWVzPSIzMDA7IDA7IDMwMCIgcmVwZWF0Q291bnQ9" +
            "ImluZGVmaW5pdGUiLz4KICAgICAgICAgICAgPGFuaW1hdGUgYXR0cmlidXRl" +
            "TmFtZT0ieTIiIGR1cj0iMjBzIiB2YWx1ZXM9Ii0yNDA7IDYwOyAtMjQwIiBy" +
            "ZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogICAgICAgIDwvbGluZWFyR3Jh" +
            "ZGllbnQ%2BCiAgICAgICAgPGxpbmVhckdyYWRpZW50IGlkPSJsb3dlci1rIi" +
            "B4MT0iMCIgeTE9IjYwIiB4Mj0iNjAwIiB5Mj0iLTU0MCI%2BCiAgICAgICAg" +
            "ICAgIDxzdG9wIG9mZnNldD0iMCIgc3RvcC1jb2xvcj0iIzI5YWJlMiIvPgog" +
            "ICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii4xIiBzdG9wLWNvbG9yPSIjYTM1" +
            "ZWJiIi8%2BCiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iLjIiIHN0b3AtY2" +
            "9sb3I9IiMwMDkwYWEiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuMy" +
            "Igc3RvcC1jb2xvcj0iIzAxODE4ZiIvPgogICAgICAgICAgICA8c3RvcCBvZm" +
            "ZzZXQ9Ii40NSIgc3RvcC1jb2xvcj0iIzAwOTc4YiIvPgogICAgICAgICAgIC" +
            "A8c3RvcCBvZmZzZXQ9Ii42IiBzdG9wLWNvbG9yPSIjNjQ4YmUwIi8%2BCiAg" +
            "ICAgICAgICAgIDxzdG9wIG9mZnNldD0iLjciIHN0b3AtY29sb3I9IiMwMDll" +
            "YzYiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuOCIgc3RvcC1jb2xv" +
            "cj0iIzI5YWJlMiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii45IiBz" +
            "dG9wLWNvbG9yPSIjMDRhOTcxIi8%2BCiAgICAgICAgICAgIDxzdG9wIG9mZn" +
            "NldD0iMSIgc3RvcC1jb2xvcj0iIzA0YTk3MSIvPgogICAgICAgICAgICA8YW" +
            "5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ4MSIgZHVyPSI4MHMiIHZhbHVlcz0iMD" +
            "sgLTYwMDsgMCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz4KICAgICAgIC" +
            "AgICAgPGFuaW1hdGUgYXR0cmlidXRlTmFtZT0ieTEiIGR1cj0iODBzIiB2YW" +
            "x1ZXM9IjYwOyA2NjA7IDYwIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPg" +
            "ogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ4MiIgZHVyPS" +
            "I4MHMiIHZhbHVlcz0iNjAwOyAwOyA2MDAiIHJlcGVhdENvdW50PSJpbmRlZm" +
            "luaXRlIi8%2BCiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9" +
            "InkyIiBkdXI9IjgwcyIgdmFsdWVzPSItNTQwOyA2MDsgLTU0MCIgcmVwZWF0" +
            "Q291bnQ9ImluZGVmaW5pdGUiLz4KICAgICAgICA8L2xpbmVhckdyYWRpZW50" +
            "PgogICAgICAgIDxwYXRoIGlkPSJvIgogICAgICAgICAgICAgIGQ9Ik03My45" +
            "LDYwYy0yNy42LDIuOS0yNy42LTQ5LjgsMC00N0MxMDEuNCwxMC4xLDEwMS40" +
            "LDYyLjksNzMuOSw2MHoKICAgICAgICAgICAgICAgICBNNjUuNyw0Ny43YzMu" +
            "MSw0LjgsMTMuMiw0LjgsMTYuMywwIGMzLjYtNC4yLDMuNS0xOC4yLDAuMS0y" +
            "Mi4zYy0zLTQuOC0xMy4zLTQuOC0xNi4zLDBDNjIuNSwyOS42LDYyLjEsNDMu" +
            "NSw2NS43LDQ3Ljd6Ii8%2BCiAgICAgICAgPHBhdGggaWQ9Im0iCiAgICAgIC" +
            "AgICAgICAgZD0iTTEwNC40LDU4LjlWMTQuMWg3LjZsMC43LDUuM2M2LjEtNy" +
            "45LDE5LjgtOS4yLDI0LjQsMGM1LjktNy4xLDE3LjctOSwyMy42LTIuM2M2Lj" +
            "IsNiwyLjYsMzMuNiwzLjUsNDEuN2gtOS41VjMwLjcKICAgICAgICAgICAgIC" +
            "AgICBjMC41LTEwLjUtMTAuNi0xMC44LTE1LjYtMy41YzAuMiwwLjEsMC4xLD" +
            "MwLjksMC4xLDMxLjhoLTkuNVYzMC44YzAuNC0xMS0xMC44LTEwLjktMTUuNi" +
            "0yLjh2MzAuOUgxMDQuNEwxMDQuNCw1OC45eiIvPgogICAgPC9kZWZzPgogIC" +
            "AgPHBvbHlnb24gcG9pbnRzPSIwLDAgMzAuNSwwIDAsMzAuNSIgZmlsbD0idX" +
            "JsKCN1cHBlci1rKSIvPgogICAgPHBvbHlnb24gcG9pbnRzPSIzMC41LDAgMC" +
            "wzMC41IDAsNjAgNjAsMCIgZmlsbD0idXJsKCNzdHJpcCkiLz4KICAgIDxwb2" +
            "x5Z29uIHBvaW50cz0iMCw2MCAzMCwzMCA2MCw2MCIgZmlsbD0idXJsKCNsb3" +
            "dlci1rKSIvPgogICAgPHVzZSB4bGluazpocmVmPSIjbyIvPgogICAgPHVzZS" +
            "B4bGluazpocmVmPSIjbSIvPgogICAgPHVzZSB4bGluazpocmVmPSIjbSIgeD" +
            "0iNzEuNSIvPgogICAgPHVzZSB4bGluazpocmVmPSIjbyIgeD0iMTkyLjMiLz" +
            "4KICAgIDxwYXRoIGQ9Ik0yOTYuMiw1OC45VjE0LjFoNy42bDAuNyw1LjNjMT" +
            "AuNC0xMSwyOS41LTguMSwyOC4yLDEwLjZjMCwwLDAsMjguOSwwLDI4LjloLT" +
            "kuNWMtMC42LTUuMiwxLjYtMzAuNC0xLjctMzQuNAogICAgICAgICAgICAgYy" +
            "00LjItNC42LTEyLjEtMS45LTE1LjgsMy40VjU5TDI5Ni4yLDU4LjlMMjk2Lj" +
            "IsNTguOXoiLz4KICAgIDxwYXRoIGQ9Ik0zNTcuNCw2MGMtNi43LDAtMTItMS" +
            "4yLTE1LjktMy43di05LjJjNS45LDMuOCwxNi4xLDYuMiwyMi41LDMuM2M2Lj" +
            "gtNy42LTQuNy05LjMtMTAuNS0xMC40Yy03LjgtMS44LTExLjItNC45LTExLj" +
            "QtMTIuNwogICAgICAgICAgICAgYy0xLjgtMTUsMjAuNC0xNi45LDMwLjQtMT" +
            "EuNVYyNWMtMTctOS43LTMwLjMsMi45LTE0LjcsNi40YzQuNSwxLDEyLjMsMi" +
            "45LDE0LjQsNS44QzM3OS4zLDQ3LjYsMzcyLjcsNjEuMiwzNTcuNCw2MHoiLz" +
            "4KPHRleHQ%2BYfCdlZPwn6ug8J%2BHqfCfh6rwn5Go8J%2BPvuKAjfCfprHw" +
            "n5Gp4oCN8J%2BRqeKAjfCfkabigI3wn5GmPC90ZXh0Pjwvc3ZnPg"
        val gifImage = GifImageFixture.bytes
    }
}
