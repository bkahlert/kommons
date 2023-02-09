package com.bkahlert.kommons.test.fixtures

import io.ktor.http.ContentType

/**
 * A [BinaryResourceFixture] encompassing an image showing a red and white pixel
 * in the Graphics Interchange Format (GIF).
 */
@Suppress("SpellCheckingInspection")
public val GifImageFixture: BinaryResourceFixture = BinaryResourceFixture(
    "pixels.gif",
    ContentType.Image.GIF,
    "R0lGODdhAQADAPABAP////8AACwAAAAAAQADAAACAgxQADs="
)
