package koodies.test

import koodies.io.InMemoryFile
import koodies.io.InMemoryTextFile

/**
 * An HTML [InMemoryFile] showing the "Hello World!".
 */
public object HtmlFixture : Fixture<String>, InMemoryTextFile(
    "example.html", """
        <html>
          <head><title>Hello Title!</title>
        </head>
        <body style="background: url('data:image/gif;base64,R0lGODdhAQADAPABAP////8AACwAAAAAAQADAAACAgxQADs=')">
            <h1>Hello Headline!</h1>
            <p>Hello World!</p>
        </body>
        </html>
    """.trimIndent()) {
    override val contents: String get() = text
}
