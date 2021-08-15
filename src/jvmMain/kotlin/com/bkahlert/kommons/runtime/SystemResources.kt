package com.bkahlert.kommons.runtime

import java.net.URL

public fun String.asSystemResourceUrl(): URL = ClassLoader.getSystemResources(this).nextElement()
