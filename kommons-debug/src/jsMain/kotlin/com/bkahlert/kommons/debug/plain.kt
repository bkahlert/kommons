package com.bkahlert.kommons.debug

internal actual val Collection<*>.isPlain: Boolean
    get() = this::class.js.name.let {
        it.endsWith("HashSet") || it.endsWith("ArrayList")
    } || keys.contains("length")

internal actual val Map<*, *>.isPlain: Boolean
    get() = this::class.js.name.endsWith("HashMap") || keys.contains("length")
