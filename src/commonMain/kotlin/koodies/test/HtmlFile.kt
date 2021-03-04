package koodies.test

/**
 * A HTML file [Fixture] showing the "Hello World!".
 */
public object HtmlFile : Fixture by TextFixture(
    "example.html", """
        <html>
        <head><title>Hello Title!</title>
        </head>
        <body>
        <h1>Hello Headline!</h1>
        <p>Hello World!</p>
        </body>
        </html>
    """.trimIndent())
