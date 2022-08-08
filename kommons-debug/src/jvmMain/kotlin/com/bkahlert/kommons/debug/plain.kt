package com.bkahlert.kommons.debug

internal actual val Collection<*>.isPlain: Boolean
    get() = isNative

internal actual val Map<*, *>.isPlain: Boolean
    get() = isNative

private val Any.isNative: Boolean
    get() = javaClass.`package`.name.let {
        it.startsWith("java.") || it.startsWith("kotlin.")
    }
