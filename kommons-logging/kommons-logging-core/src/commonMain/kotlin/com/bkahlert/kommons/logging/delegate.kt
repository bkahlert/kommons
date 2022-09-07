package com.bkahlert.kommons.logging

import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KProperty

/**
 * Returns a logger property of which the name is derived from
 * the owning class,
 * respectively the companion object's owning class, or if missing,
 * the file class.
 *
 * Uses [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)'s [KotlinLogging.logger] to get the logger.
 */
public operator fun KotlinLogging.provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<KLogger> {
    val name = thisRef.loggerName(::provideDelegate)
    return lazy { logger(name) }
}
