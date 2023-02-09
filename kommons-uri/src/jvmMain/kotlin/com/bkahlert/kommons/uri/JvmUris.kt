package com.bkahlert.kommons.uri

import java.net.URI

/**
 * Returns the [URI] converted to a [Uri].
 */
public fun URI.toUri(): Uri = Uri.parse(toString())

/**
 * Returns the [Uri] converted to a [URI].
 */
public fun Uri.toJavaUri(): URI = URI(toString())

/**
 * Returns a copy of this [URI] with the specified [pathSegment]
 * added to the [URI.path].
 */
public infix operator fun URI.div(pathSegment: String): URI =
    URI(scheme, authority, "$path/$pathSegment", query, fragment)
