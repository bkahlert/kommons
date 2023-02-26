package com.bkahlert.kommons.uri

import io.ktor.http.URLBuilder
import io.ktor.http.Url

/**
 * Returns the [Url] converted to a [Uri].
 */
public fun Url.toUri(): Uri = Uri.parse(toString())

/**
 * Returns the [Uri] converted to a [Url].
 */
public fun Uri.toUrl(): Url = Url(toString())

/** Constructs a [Url] instance from the specified [urlString]. */
@Deprecated(
    message = "This function is a synonym for Url(...) for better discoverability. Use Url factory instead.",
    replaceWith = ReplaceWith("Url(urlString)", "io.ktor.http.Url"),
)
@Suppress("NOTHING_TO_INLINE")
public inline fun Url.Companion.parse(urlString: String): Url = Url(urlString)

/**
 * Builds a [Url] instance with the given [builder] function, and
 * the [URLBuilder] initialized with the given [url].
 */
public inline fun Url.Companion.build(url: Url? = null, builder: URLBuilder.() -> Unit): Url = when (url) {
    null -> URLBuilder()
    else -> URLBuilder(url)
}.apply(builder).build()

/**
 * Returns a copy of this [Url] with the specified [pathSegment]
 * added to the [pathSegments].
 */
public operator fun Url.div(pathSegment: String): Url =
    Url.build(this) { pathSegments = pathSegments + pathSegment }
