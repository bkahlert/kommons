package com.bkahlert.kommons.uri

import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 * A [URLStreamHandler]
 * providing support for [DataUri] as specified in [RFC2397](https://www.rfc-editor.org/rfc/rfc2397).
 */
public object DataURLStreamHandler : URLStreamHandler() {

    override fun openConnection(url: URL): URLConnection {
        val dataUri = DataUri.parse(url.toString())
        val contentType: String? = dataUri.mediaTypeString
        val bytes = dataUri.data
        return object : URLConnection(url) {
            override fun connect() {}
            override fun getContentType(): String? = contentType
            override fun getContentLengthLong(): Long = bytes.size.toLong()
            override fun getInputStream(): InputStream = bytes.inputStream()
        }
    }
}
