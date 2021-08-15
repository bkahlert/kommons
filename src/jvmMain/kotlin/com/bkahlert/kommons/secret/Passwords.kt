package com.bkahlert.kommons.secret

import com.bkahlert.kommons.text.minus

public fun password(key: String, offset: Int): String =
    System.getProperty(key) - offset
