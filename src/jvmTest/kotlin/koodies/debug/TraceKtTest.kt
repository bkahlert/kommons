package koodies.debug

import strikt.api.Assertion

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` assertions builder's subject.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
@Suppress("RedundantVisibilityModifier")
public val <T : Assertion.Builder<V>, V> T.trace: T
    get() = also {
        get {
            val subject: V = this
            @Suppress("DEPRECATION")
            subject.trace
        }
    }

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` assertion builder's subject and the subject applied to the given [transform] to [println]
 * while still returning `this`.
 */
@Suppress("RedundantVisibilityModifier")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T : Assertion.Builder<V>, V> T.trace(transform: V.() -> Any?): T =
    also {
        get {
            val subject: V = this
            @Suppress("DEPRECATION")
            subject.trace(transform)
        }
    }
