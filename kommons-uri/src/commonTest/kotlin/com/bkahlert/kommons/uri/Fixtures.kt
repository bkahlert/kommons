package com.bkahlert.kommons.uri

import io.ktor.http.Url

/** Example base [Uri] string as described in [RFC3986 section 5.4](https://www.rfc-editor.org/rfc/rfc3986#section-5.4) */
const val BASE_URI_STRING = "http://a/b/c/d;p?q"
const val COMPLETE_URI_STRING: String = "https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"
const val EMPTY_URI_STRING: String = ""

val COMPLETE_URI: Uri = Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
val BASE_URI = Uri(scheme = "http", authority = Authority(userInfo = null, host = "a", port = null), path = "/b/c/d;p", query = "q")
val EMPTY_URI: Uri = Uri(null, null, "", null, null)

val COMPLETE_URL: Url = Url(COMPLETE_URI_STRING)
val EMPTY_URL: Url = Url(EMPTY_URI_STRING)
