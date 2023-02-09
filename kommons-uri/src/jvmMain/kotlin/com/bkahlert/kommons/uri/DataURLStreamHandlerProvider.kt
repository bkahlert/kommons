package com.bkahlert.kommons.uri

import java.net.URL
import java.net.URLStreamHandler
import java.net.spi.URLStreamHandlerProvider

/**
 * A [URLStreamHandlerProvider] as described in [URL]
 * providing support for [DataUri] as specified in [RFC2397](https://www.rfc-editor.org/rfc/rfc2397).
 */
public class DataURLStreamHandlerProvider : URLStreamHandlerProvider() {
    override fun createURLStreamHandler(protocol: String?): URLStreamHandler? = when (protocol) {
        DataUri.SCHEMA -> DataURLStreamHandler
        else -> null
    }
}
