package com.bkahlert.kommons.debug

import com.bkahlert.kommons.EMPTY
import kotlin.js.Json
import kotlin.js.json

/**
 * Special version of [inspectJs] that inspects the structure of
 * each object, no matter if a custom [Any.toString] exists or not.
 */
@Suppress("DEPRECATION")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public inline val <T> T.inspectJs: T get(): T = inspectJs()

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging].
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.breaks.print().calls()
 * ```
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * is printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public inline fun <T> T.inspectJs(
    caption: CharSequence? = null,
    includeCallSite: Boolean = true,
    inspect: (Any?) -> Json = { it.toJson() },
    noinline out: ((Json) -> Unit)? = null,
    noinline transform: Inspector<T>? = null,
): T {
    if (transform != null) {
        json((caption?.toString() ?: String.EMPTY) to inspect(this@inspectJs), "transformed" to inspect(transform(this@inspectJs)))
    } else {
        inspect(this@inspectJs)
    }.also {
        out?.invoke(it) ?: run {
            if (includeCallSite) console.trace(it)
            else console.log(it)
        }
    }
    return this
}
