package koodies.runtime

import java.net.URL

fun String.asSystemResourceUrl(): URL = ClassLoader.getSystemResources(this).nextElement()
