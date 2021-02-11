@file:Suppress("unused")

package koodies.debug

private fun highlight(subject: Any?) = "\u001B[96m$subject\u001B[39m"
private val brackets = highlight("｛") to highlight("｝")

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` to [println] while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.calls()
 * ```
 *
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * will be printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
val <T> T.trace: T
    get() : T = also { println(highlight(it)) }


/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` and `this` applied to the given [transform] to [println]
 * while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace { prop }.calls()
 * ```
 *
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * at the property `prop` of that value are printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
fun <T> T.trace(transform: (T.() -> Any?)): T =
    also { println("$it${brackets.first} ${it.transform()} ${brackets.second}") }
