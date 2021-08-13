package koodies.secret

import koodies.text.minus

public fun password(key: String, offset: Int): String =
    System.getProperty(key) - offset
