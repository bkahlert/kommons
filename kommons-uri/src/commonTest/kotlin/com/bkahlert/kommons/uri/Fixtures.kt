package com.bkahlert.kommons.uri

import io.ktor.http.Url

const val COMPLETE_URI_STRING: String = "https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"
const val EMPTY_URI_STRING: String = ""


val COMPLETE_URI: Uri = Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
val EMPTY_URI: Uri = Uri(null, null, "", null, null)

val COMPLETE_URL: Url = Url(COMPLETE_URI_STRING)
val EMPTY_URL: Url = Url(EMPTY_URI_STRING)
