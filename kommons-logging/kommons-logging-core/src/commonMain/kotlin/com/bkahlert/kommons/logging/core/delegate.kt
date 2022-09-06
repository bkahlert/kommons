package com.bkahlert.kommons.logging.core

import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

internal expect fun loggerNameOf(thisRef: Any?, fn: KFunction<*>): String

/**
 * Returns a logger property of which the name is derived from
 * the owning class,
 * respectively the companion object's owning class, or if missing,
 * the file class.
 *
 * Uses [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)'s [KotlinLogging.logger] to get the logger.
 */
public operator fun KotlinLogging.provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<KLogger> {
    val name = loggerNameOf(thisRef, ::provideDelegate)
    return lazy { logger(name) }
}
