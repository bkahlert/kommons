package koodies.net

import java.net.URI
import java.net.URL

@Suppress("NOTHING_TO_INLINE")
public inline operator fun URI.div(path: CharSequence): URI = URI(toString().removeSuffix("/") + "/" + path.removePrefix("/"))

@Suppress("NOTHING_TO_INLINE")
public inline operator fun URL.div(path: CharSequence): URL = URL(toString().removeSuffix("/") + "/" + path.removePrefix("/"))
