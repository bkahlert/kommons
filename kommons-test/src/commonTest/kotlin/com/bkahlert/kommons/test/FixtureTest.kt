package com.bkahlert.kommons.test

import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FixtureTest {

    @Suppress("SpellCheckingInspection")
    @Test fun gif_image_fixture() = testAll {
        GifImageFixture.name shouldBe "pixels.gif"
        GifImageFixture.mimeType shouldBe "image/gif"
        GifImageFixture.dataURI shouldBe """
            data:image/gif;base64,
            R0lGODdhAQADAPABAP////8AACwAAAAAAQADAAACAgxQADs=
        """.trimIndent().lineSequence().joinToString("")
    }

    @Suppress("SpellCheckingInspection")
    @Test fun html_document_fixture() = testAll {
        HtmlDocumentFixture.name shouldBe "hello-world.html"
        HtmlDocumentFixture.mimeType shouldBe "text/html"
        HtmlDocumentFixture.dataURI shouldBe """
            data:text/html;base64,
            PGh0bWw+CiAgPGhlYWQ+PHRpdGxlPkhlbGxvIFRpdGxlITwvdGl0bGU+CjwvaGVhZD4KPGJvZHkg
            c3R5bGU9ImJhY2tncm91bmQ6IHVybCgnZGF0YTppbWFnZS9naWY7YmFzZTY0LFIwbEdPRGRoQVFB
            REFQQUJBUC8vLy84QUFDd0FBQUFBQVFBREFBQUNBZ3hRQURzPScpIj4KICAgIDxoMT5IZWxsbyBI
            ZWFkbGluZSE8L2gxPgogICAgPHA+SGVsbG8gV29ybGQhPC9wPgo8L2JvZHk+CjwvaHRtbD4=
        """.trimIndent().lineSequence().joinToString("")
    }

    @Suppress("SpellCheckingInspection")
    @Test fun svg_image_fixture() = testAll {
        SvgImageFixture.name shouldBe "kommons.svg"
        SvgImageFixture.mimeType shouldBe "image/svg+xml"
        SvgImageFixture.dataURI shouldBe """
            data:image/svg+xml;base64,
            PHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1s
            bnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIGFyaWEtbGFiZWw9IktvbW1v
            bnMiIHJvbGU9ImltZyIgdmlld0JveD0iMCAwIDM3NSA2MCIgc3R5bGU9ImN1cnNvcjogZGVmYXVs
            dDsiPgogICAgPHN0eWxlPgogICAgICAgIHBhdGggeyBmaWxsOiAjMjMxRjIwOyB9CiAgICAgICAg
            QG1lZGlhIChwcmVmZXJzLWNvbG9yLXNjaGVtZTogZGFyaykgeyB0ZXh0IHsgY29sb3I6ICNEQ0Uw
            REY7IH0gfQogICAgPC9zdHlsZT4KICAgIDxkZWZzPgogICAgICAgIDxsaW5lYXJHcmFkaWVudCBp
            ZD0idXBwZXItayIgeDE9Ii03MjAiIHkxPSI3ODAiIHgyPSIwIiB5Mj0iNjAiPgogICAgICAgICAg
            ICA8c3RvcCBvZmZzZXQ9IjAiIHN0b3AtY29sb3I9IiMyOWFiZTIiLz4KICAgICAgICAgICAgPHN0
            b3Agb2Zmc2V0PSIuMSIgc3RvcC1jb2xvcj0iI2EzNWViYiIvPgogICAgICAgICAgICA8c3RvcCBv
            ZmZzZXQ9Ii4yIiBzdG9wLWNvbG9yPSIjMDA5MGFhIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNl
            dD0iLjMiIHN0b3AtY29sb3I9IiMwMTgxOGYiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIu
            MyIgc3RvcC1jb2xvcj0iIzAxODE4ZiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii40NSIg
            c3RvcC1jb2xvcj0iIzAwOTc4YiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii42IiBzdG9w
            LWNvbG9yPSIjNjQ4YmUwIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iLjciIHN0b3AtY29s
            b3I9IiMwMDllYzYiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuOCIgc3RvcC1jb2xvcj0i
            IzI5YWJlMiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii45IiBzdG9wLWNvbG9yPSIjMDRh
            OTcxIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iIzA0YTk3MSIv
            PgogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ4MSIgZHVyPSI2MHMiIHZhbHVl
            cz0iMDsgLTcyMDsgMCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz4KICAgICAgICAgICAgPGFu
            aW1hdGUgYXR0cmlidXRlTmFtZT0ieTEiIGR1cj0iNjBzIiB2YWx1ZXM9IjYwOyA3ODA7IDYwIiBy
            ZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVO
            YW1lPSJ4MiIgZHVyPSI2MHMiIHZhbHVlcz0iNzIwOyAwOyA3MjAiIHJlcGVhdENvdW50PSJpbmRl
            ZmluaXRlIi8+CiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9InkyIiBkdXI9IjYw
            cyIgdmFsdWVzPSItNjYwOyA2MDsgLTY2MCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz4KICAg
            ICAgICA8L2xpbmVhckdyYWRpZW50PgogICAgICAgIDxsaW5lYXJHcmFkaWVudCBpZD0ic3RyaXAi
            IHgxPSIwIiB5MT0iNjAiIHgyPSIzMDAiIHkyPSItMjQwIj4KICAgICAgICAgICAgPHN0b3Agb2Zm
            c2V0PSIwIiBzdG9wLWNvbG9yPSIjQzc1N0JDIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0i
            LjAyNSIgc3RvcC1jb2xvcj0iI0QwNjA5QSIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii4w
            NSIgc3RvcC1jb2xvcj0iI0UxNzI1QyIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii4wNzUi
            IHN0b3AtY29sb3I9IiNFRTdFMkYiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuMSIgc3Rv
            cC1jb2xvcj0iI0Y1ODYxMyIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii4yIiBzdG9wLWNv
            bG9yPSIjRjg4OTA5Ii8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iLjUiIHN0b3AtY29sb3I9
            IiNmZjkyMzQiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjZWJi
            MjFkIi8+CiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9IngxIiBkdXI9IjIwcyIg
            dmFsdWVzPSIwOyAtMzAwOyAwIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogICAgICAgICAg
            ICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ5MSIgZHVyPSIyMHMiIHZhbHVlcz0iNjA7IDM2MDsg
            NjAiIHJlcGVhdENvdW50PSJpbmRlZmluaXRlIi8+CiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJp
            YnV0ZU5hbWU9IngyIiBkdXI9IjIwcyIgdmFsdWVzPSIzMDA7IDA7IDMwMCIgcmVwZWF0Q291bnQ9
            ImluZGVmaW5pdGUiLz4KICAgICAgICAgICAgPGFuaW1hdGUgYXR0cmlidXRlTmFtZT0ieTIiIGR1
            cj0iMjBzIiB2YWx1ZXM9Ii0yNDA7IDYwOyAtMjQwIiByZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIv
            PgogICAgICAgIDwvbGluZWFyR3JhZGllbnQ+CiAgICAgICAgPGxpbmVhckdyYWRpZW50IGlkPSJs
            b3dlci1rIiB4MT0iMCIgeTE9IjYwIiB4Mj0iNjAwIiB5Mj0iLTU0MCI+CiAgICAgICAgICAgIDxz
            dG9wIG9mZnNldD0iMCIgc3RvcC1jb2xvcj0iIzI5YWJlMiIvPgogICAgICAgICAgICA8c3RvcCBv
            ZmZzZXQ9Ii4xIiBzdG9wLWNvbG9yPSIjYTM1ZWJiIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNl
            dD0iLjIiIHN0b3AtY29sb3I9IiMwMDkwYWEiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIu
            MyIgc3RvcC1jb2xvcj0iIzAxODE4ZiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii40NSIg
            c3RvcC1jb2xvcj0iIzAwOTc4YiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii42IiBzdG9w
            LWNvbG9yPSIjNjQ4YmUwIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iLjciIHN0b3AtY29s
            b3I9IiMwMDllYzYiLz4KICAgICAgICAgICAgPHN0b3Agb2Zmc2V0PSIuOCIgc3RvcC1jb2xvcj0i
            IzI5YWJlMiIvPgogICAgICAgICAgICA8c3RvcCBvZmZzZXQ9Ii45IiBzdG9wLWNvbG9yPSIjMDRh
            OTcxIi8+CiAgICAgICAgICAgIDxzdG9wIG9mZnNldD0iMSIgc3RvcC1jb2xvcj0iIzA0YTk3MSIv
            PgogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVOYW1lPSJ4MSIgZHVyPSI4MHMiIHZhbHVl
            cz0iMDsgLTYwMDsgMCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz4KICAgICAgICAgICAgPGFu
            aW1hdGUgYXR0cmlidXRlTmFtZT0ieTEiIGR1cj0iODBzIiB2YWx1ZXM9IjYwOyA2NjA7IDYwIiBy
            ZXBlYXRDb3VudD0iaW5kZWZpbml0ZSIvPgogICAgICAgICAgICA8YW5pbWF0ZSBhdHRyaWJ1dGVO
            YW1lPSJ4MiIgZHVyPSI4MHMiIHZhbHVlcz0iNjAwOyAwOyA2MDAiIHJlcGVhdENvdW50PSJpbmRl
            ZmluaXRlIi8+CiAgICAgICAgICAgIDxhbmltYXRlIGF0dHJpYnV0ZU5hbWU9InkyIiBkdXI9Ijgw
            cyIgdmFsdWVzPSItNTQwOyA2MDsgLTU0MCIgcmVwZWF0Q291bnQ9ImluZGVmaW5pdGUiLz4KICAg
            ICAgICA8L2xpbmVhckdyYWRpZW50PgogICAgICAgIDxwYXRoIGlkPSJvIgogICAgICAgICAgICAg
            IGQ9Ik03My45LDYwYy0yNy42LDIuOS0yNy42LTQ5LjgsMC00N0MxMDEuNCwxMC4xLDEwMS40LDYy
            LjksNzMuOSw2MHoKICAgICAgICAgICAgICAgICBNNjUuNyw0Ny43YzMuMSw0LjgsMTMuMiw0Ljgs
            MTYuMywwIGMzLjYtNC4yLDMuNS0xOC4yLDAuMS0yMi4zYy0zLTQuOC0xMy4zLTQuOC0xNi4zLDBD
            NjIuNSwyOS42LDYyLjEsNDMuNSw2NS43LDQ3Ljd6Ii8+CiAgICAgICAgPHBhdGggaWQ9Im0iCiAg
            ICAgICAgICAgICAgZD0iTTEwNC40LDU4LjlWMTQuMWg3LjZsMC43LDUuM2M2LjEtNy45LDE5Ljgt
            OS4yLDI0LjQsMGM1LjktNy4xLDE3LjctOSwyMy42LTIuM2M2LjIsNiwyLjYsMzMuNiwzLjUsNDEu
            N2gtOS41VjMwLjcKICAgICAgICAgICAgICAgICBjMC41LTEwLjUtMTAuNi0xMC44LTE1LjYtMy41
            YzAuMiwwLjEsMC4xLDMwLjksMC4xLDMxLjhoLTkuNVYzMC44YzAuNC0xMS0xMC44LTEwLjktMTUu
            Ni0yLjh2MzAuOUgxMDQuNEwxMDQuNCw1OC45eiIvPgogICAgPC9kZWZzPgogICAgPHBvbHlnb24g
            cG9pbnRzPSIwLDAgMzAuNSwwIDAsMzAuNSIgZmlsbD0idXJsKCN1cHBlci1rKSIvPgogICAgPHBv
            bHlnb24gcG9pbnRzPSIzMC41LDAgMCwzMC41IDAsNjAgNjAsMCIgZmlsbD0idXJsKCNzdHJpcCki
            Lz4KICAgIDxwb2x5Z29uIHBvaW50cz0iMCw2MCAzMCwzMCA2MCw2MCIgZmlsbD0idXJsKCNsb3dl
            ci1rKSIvPgogICAgPHVzZSB4bGluazpocmVmPSIjbyIvPgogICAgPHVzZSB4bGluazpocmVmPSIj
            bSIvPgogICAgPHVzZSB4bGluazpocmVmPSIjbSIgeD0iNzEuNSIvPgogICAgPHVzZSB4bGluazpo
            cmVmPSIjbyIgeD0iMTkyLjMiLz4KICAgIDxwYXRoIGQ9Ik0yOTYuMiw1OC45VjE0LjFoNy42bDAu
            Nyw1LjNjMTAuNC0xMSwyOS41LTguMSwyOC4yLDEwLjZjMCwwLDAsMjguOSwwLDI4LjloLTkuNWMt
            MC42LTUuMiwxLjYtMzAuNC0xLjctMzQuNAogICAgICAgICAgICAgYy00LjItNC42LTEyLjEtMS45
            LTE1LjgsMy40VjU5TDI5Ni4yLDU4LjlMMjk2LjIsNTguOXoiLz4KICAgIDxwYXRoIGQ9Ik0zNTcu
            NCw2MGMtNi43LDAtMTItMS4yLTE1LjktMy43di05LjJjNS45LDMuOCwxNi4xLDYuMiwyMi41LDMu
            M2M2LjgtNy42LTQuNy05LjMtMTAuNS0xMC40Yy03LjgtMS44LTExLjItNC45LTExLjQtMTIuNwog
            ICAgICAgICAgICAgYy0xLjgtMTUsMjAuNC0xNi45LDMwLjQtMTEuNVYyNWMtMTctOS43LTMwLjMs
            Mi45LTE0LjcsNi40YzQuNSwxLDEyLjMsMi45LDE0LjQsNS44QzM3OS4zLDQ3LjYsMzcyLjcsNjEu
            MiwzNTcuNCw2MHoiLz4KPC9zdmc+
        """.trimIndent().lineSequence().joinToString("")
    }

    @Suppress("SpellCheckingInspection")
    @Test fun unicode_text_document_fixture() = testAll {
        UnicodeTextDocumentFixture.name shouldBe "unicode.txt"
        UnicodeTextDocumentFixture.mimeType shouldBe "text/plain"
        UnicodeTextDocumentFixture.dataURI shouldBe """
            data:text/plain;base64,
            YcKF8J2Vkw0K4piwCvCfkYsK
        """.trimIndent().lineSequence().joinToString("")
    }

    @Suppress("SpellCheckingInspection")
    @Test fun emoji_text_document_fixture() = testAll {
        EmojiTextDocumentFixture.name shouldBe "emoji.txt"
        EmojiTextDocumentFixture.mimeType shouldBe "text/plain"
        EmojiTextDocumentFixture.contents shouldBe "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦"
        EmojiTextDocumentFixture.dataURI shouldBe """
            data:text/plain;base64,
            YfCdlZPwn6ug8J+HqfCfh6rwn5Go8J+PvuKAjfCfprHwn5Gp4oCN8J+RqeKAjfCfkabigI3wn5Gm
        """.trimIndent().lineSequence().joinToString("")
    }
}
