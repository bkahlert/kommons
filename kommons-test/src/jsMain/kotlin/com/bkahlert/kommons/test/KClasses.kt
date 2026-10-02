package com.bkahlert.kommons.test

import kotlin.reflect.KClass

// KClass.qualifiedName is unsupported on Kotlin/JS and throws.
@PublishedApi
internal actual fun KClass<*>.bestName(): String = simpleName ?: toString()
