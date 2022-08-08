package com.bkahlert.kommons.test.fixtures

/**
 * A [BinaryResourceFixture] encompassing an image showing a red and white pixel
 * in the Graphics Interchange Format (GIF).
 */
@Suppress("SpellCheckingInspection")
public object GifImageFixture : BinaryResourceFixture(
    "pixels.gif",
    "image/gif",
    "R0lGODdhAQADAPABAP////8AACwAAAAAAQADAAACAgxQADs="
)
