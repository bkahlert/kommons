package com.bkahlert.kommons.debug

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

/**
 * Renders the [simplified] type of this object.
 *
 * Example:
 * ```kotlin
 * "string".renderType() // "kotlin.String"
 * "string".renderType(simplified=false) // "String"
 * ```
 */
public fun Any.renderType(simplified: Boolean = true): String = buildString { this@renderType.renderTypeTo(this, simplified) }

/**
 * Renders the [simplified] type of this object to the specified [out].
 * @see renderType
 */
public fun Any.renderTypeTo(out: StringBuilder, simplified: Boolean = true) {
    if (simplified) {
        when (this) {
            // specific types
            is UByteArray -> out.append("UByteArray")
            is UShortArray -> out.append("UShortArray")
            is UIntArray -> out.append("UIntArray")
            is ULongArray -> out.append("ULongArray")

            is Map<*, *> -> if (isPlain) out.append("Map") else out.append(this::class.simplifiedName)
            is Set<*> -> if (isPlain) out.append("Set") else out.append(this::class.simplifiedName)
            is List<*> -> if (isPlain) out.append("List") else out.append(this::class.simplifiedName)
            is Collection<*> -> if (isPlain) out.append("Collection") else out.append(this::class.simplifiedName)
            is Iterable<*> -> out.append("Iterable")

            // KCallable
            is KProperty<*> -> out.append(this::class.simplifiedName)
            is KFunction<*> -> renderFunctionTypeTo(out, simplified)
            is Function<*> -> renderFunctionTypeTo(out, simplified)

            else -> this::class.renderKClassifierTo(out, simplified)
        }
    } else this::class.renderKClassifierTo(out, simplified)
}

private val objectRegex = "\\$\\d+$".toRegex()
internal val KClass<*>.simplifiedName get() = simpleName?.takeUnless { objectRegex.containsMatchIn(it) }?.removeSuffix("Impl") ?: "<object>"

/**
 * Renders this [KClassifier].
 */
internal fun KClassifier.renderKClassifier(simplified: Boolean = true): String = buildString { this@renderKClassifier.renderKClassifierTo(this, simplified) }

/**
 * Renders this [KClassifier] to the specified [out].
 * @see renderKClassifier
 */
internal expect fun KClassifier.renderKClassifierTo(out: StringBuilder, simplified: Boolean = true)

/**
 * Renders the type of this function.
 */
internal fun Function<*>.renderFunctionType(simplified: Boolean = true): String = buildString { this@renderFunctionType.renderFunctionTypeTo(this, simplified) }

/**
 * Renders the type of this function to the specified [out].
 * @see renderFunctionType
 */
internal expect fun Function<*>.renderFunctionTypeTo(out: StringBuilder, simplified: Boolean = true)
