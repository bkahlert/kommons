package koodies.test

/**
 * A HTML file [Fixture] showing the "Hello World!".
 */
object HtmlFile : Fixture by StringFixture(
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
