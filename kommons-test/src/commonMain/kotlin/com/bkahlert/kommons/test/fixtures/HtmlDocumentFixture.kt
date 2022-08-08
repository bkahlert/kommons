package com.bkahlert.kommons.test.fixtures

/**
 * An [TextResourceFixture] consisting of HTML code that
 * renders the headline "Hello Headline!", and
 * the paragraph "Hello World!"
 * on a background of horizontal red and white stripes.
 */
public object HtmlDocumentFixture : TextResourceFixture(
    "hello-world.html",
    "text/html",
    """
        <html>
          <head><title>Hello Title!</title>
        </head>
        <body style="background: url('${GifImageFixture.dataURI}')">
            <h1>Hello Headline!</h1>
            <p>Hello World!</p>
        </body>
        </html>
    """.trimIndent()
)
