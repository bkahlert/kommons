package koodies.net

import koodies.collections.matchKeysByIgnoringCase
import koodies.time.seconds
import koodies.time.toIntMilliseconds
import java.net.URI
import java.net.URL
import kotlin.time.Duration

public fun URL.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds): Map<String, List<String>> = openConnection().run {
    this.connectTimeout = connectTimeout.toIntMilliseconds()
    this.readTimeout = readTimeout.toIntMilliseconds()

    val headers: MutableMap<String, MutableList<String>> = mutableMapOf()
    headerFields.forEach { (key, values) ->
        headers.getOrPut(key) { mutableListOf() }.addAll(values)
    }
    headers.putIfAbsent("status", headerFields[null] ?: mutableListOf())
    headers.matchKeysByIgnoringCase()
}

public fun URI.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds): Map<String, List<String>> =
    toURL().headers(connectTimeout, readTimeout)
