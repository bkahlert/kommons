package com.bkahlert.kommons.debug

/**
 * The built-in `Object` object.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object">Object</a>
 */
public external object Object {
    /**
     * Returns the own enumerable property names of the specified [obj] as an array,
     * iterated in the same order that a usual loop would.
     */
    public fun keys(obj: Any): Array<String>

    /**
     * Returns the own enumerable string-keyed property [key, value] pairs of the specified [obj] as an array.
     */
    public fun entries(obj: Any): Array<Array<Any?>>

    /**
     * Returns all properties, including non-enumerable properties except for those which use Symbol, found directly in
     * the specified [obj] as an array.
     */
    public fun getOwnPropertyNames(obj: Any): Array<String>
}

/**
 * Returns the own enumerable property names of this object as an array,
 * iterated in the same order that a usual loop would.
 */
public val Any.keys: Array<String> get() = Object.keys(this)

/**
 * Returns the own enumerable string-keyed property [key, value] pairs of this object as an array.
 */
public val Any.entries: Array<Array<Any?>> get() = Object.entries(this)
