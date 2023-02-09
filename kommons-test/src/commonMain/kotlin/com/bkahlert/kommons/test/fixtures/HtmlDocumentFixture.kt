package com.bkahlert.kommons.test.fixtures

import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets

/**
 * An [TextResourceFixture] consisting of HTML code that
 * renders the headline "Hello Headline!", and
 * the paragraph "Hello World!"
 * on a background of horizontal red and white stripes.
 */
public val HtmlDocumentFixture: TextResourceFixture = TextResourceFixture(
    "hello-world.html",
    ContentType.Text.Html.withCharset(Charsets.UTF_8),
    """
        <html>
          <head><title>Hello Title!</title>
        </head>
        <body style="background: url('${GifImageFixture.dataUri}')">
          <h1>Hello World!</h1>
          <p>${EmojiTextDocumentFixture.contents}</p>
          <iframe src="${UnicodeTextDocumentFixture.dataUri}"></iframe>
          <img src="${SvgImageFixture.dataUri}">
        </body>
        </html>
    """.trimIndent()
)
