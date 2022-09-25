package com.bkahlert.kommons_deprecated.io.path

import java.net.URLConnection
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.pathString
import kotlin.io.path.readBytes

/**
 * Returns the likely MIME type guessed from the file name.
 */
public fun Path.guessMimeType(): String? = kotlin.runCatching { URLConnection.guessContentTypeFromName(fileName.pathString) }.getOrNull()

/**
 * Converts this path using Base64.
 */
public fun Path.toBase64(): String = Base64.getEncoder().encodeToString(readBytes())

/**
 * Converts this path to a [data URI](https://en.wikipedia.org/wiki/Data_URI_scheme) of the form
 * `data:[<media type>][;base64],<data>`, e.g. `data:image/gif;base64,…`
 */
public fun Path.toDataUri(explicitMimeType: String? = null, fallbackMimeType: String = "application/octet-stream"): String {
    val mimeType = explicitMimeType ?: guessMimeType() ?: fallbackMimeType
    return "data:$mimeType;base64,${toBase64()}"
}
