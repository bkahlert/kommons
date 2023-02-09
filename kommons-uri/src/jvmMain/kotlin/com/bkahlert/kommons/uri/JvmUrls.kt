package com.bkahlert.kommons.uri

import java.net.URL

/**
 * Returns the [URL] converted to a [Uri].
 */
public fun URL.toUri(): Uri = Uri.parse(toString())

/**
 * Returns the [Uri] converted to a [URL].
 */
public fun Uri.toJavaUrl(): URL = URL(toString())

/**
 * Returns a copy of this [URL] with the specified [pathSegment]
 * added to the [URL.path].
 */
public infix operator fun URL.div(pathSegment: String): URL =
    toURI().div(pathSegment).toURL()
