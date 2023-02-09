package com.bkahlert.kommons.uri

import io.ktor.http.Parameters

/**
 * Returns the [User information subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.1).
 */
public val Uri.userInfo: String? get() = authority?.userInfo

/**
 * Returns the [Host subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.2).
 */
public val Uri.host: String? get() = authority?.host

/**
 * Returns the [Port subcomponent](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.3).
 */
public val Uri.port: Int? get() = authority?.port


/**
 * Returns the segments of the [Path component](https://www.rfc-editor.org/rfc/rfc3986#section-3.3).
 *
 * ***Note:** In case of an absolute path, the first segment is an empty string.*
 */
public val Uri.pathSegments: List<String> get() = path.split(Uri.PATH_SEGMENTS_SEPARATOR)

/**
 * Returns the [Parameters] encoded in the [Query component](https://www.rfc-editor.org/rfc/rfc3986#section-3.4), or
 * [Parameters.Empty], if no [Uri.query] is present.
 */
public val Uri.queryParameters: Parameters get() = query?.let { io.ktor.http.parseQueryString(it) } ?: Parameters.Empty

/**
 * Returns the [Parameters] encoded in the [Fragment component](https://www.rfc-editor.org/rfc/rfc3986#section-3.5), or
 * [Parameters.Empty], if no [Uri.fragment] is present.
 */
public val Uri.fragmentParameters: Parameters get() = fragment?.let { io.ktor.http.parseQueryString(it) } ?: Parameters.Empty


/**
 * Returns a copy of this [Uri] with the specified [pathSegment]
 * added to the [pathSegments].
 */
public infix operator fun Uri.div(pathSegment: String): Uri =
    Uri(scheme, authority, (pathSegments + pathSegment).joinToString(Uri.PATH_SEGMENTS_SEPARATOR), query, fragment)
