package com.bkahlert.kommons.net

import com.bkahlert.kommons.collections.matchKeysByIgnoringCase
import java.net.URI
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public fun URL.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds): Map<String, List<String>> = openConnection().run {
    this.connectTimeout = connectTimeout.inWholeMilliseconds.toInt()
    this.readTimeout = readTimeout.inWholeMilliseconds.toInt()

    val headers: MutableMap<String, MutableList<String>> = mutableMapOf()
    headerFields.forEach { (key, values) ->
        headers.getOrPut(key ?: "status") { mutableListOf() }.addAll(values)
    }
    headers.matchKeysByIgnoringCase()
}

public fun URI.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds): Map<String, List<String>> =
    toURL().headers(connectTimeout, readTimeout)
