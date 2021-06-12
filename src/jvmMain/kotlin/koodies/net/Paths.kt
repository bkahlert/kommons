package koodies.net

import koodies.text.withoutPrefix
import koodies.text.withoutSuffix
import java.net.URI
import java.net.URL

@Suppress("NOTHING_TO_INLINE")
public inline operator fun URI.div(path: CharSequence): URI = URI(toString().withoutSuffix("/") + "/" + path.withoutPrefix("/"))

@Suppress("NOTHING_TO_INLINE")
public inline operator fun URL.div(path: CharSequence): URL = URL(toString().withoutSuffix("/") + "/" + path.withoutPrefix("/"))
