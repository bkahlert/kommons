package com.bkahlert.kommons.uri

import java.net.URI
import java.net.URL

val COMPLETE_JAVA_URI: URI = URI("https", "username:password@example.com:8080", "/poo/par", "qoo=qar&qaz", "foo=far&faz")
val EMPTY_JAVA_URI: URI = URI(null, null, "", null, null)

val COMPLETE_JAVA_URL: URL = URL(COMPLETE_URI_STRING)
