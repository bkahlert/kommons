package koodies.secret

import koodies.text.minus

fun password(key: String, offset: Int): String =
    System.getProperty(key) - offset
