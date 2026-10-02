package com.bkahlert.kommons.test

import kotlin.reflect.KClass

@PublishedApi
internal actual fun KClass<*>.bestName(): String = qualifiedName ?: simpleName ?: toString()
