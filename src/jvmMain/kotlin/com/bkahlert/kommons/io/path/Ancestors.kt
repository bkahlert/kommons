package com.bkahlert.kommons.io.path

import java.nio.file.Path

/**
 * Generalized [Path.getParent] whereas a parent is considered an ancestor of [order] `1`.
 * That is, order `0` resolves to this path and `2` to the parent's parent.
 *
 * Consequently, the following invariant for this path always holds true: `ancestor(n).resolve(subpath(n))`
 *
 * Returns `null` for inexistent ancestor. No matter for since how many orders.
 *
 * @see [subpath]
 */
@Suppress("SpellCheckingInspection")
public tailrec fun Path.ancestor(order: Int): Path? =
    if (order == 0) this
    else parent?.ancestor(order - 1)

/**
 * Returns a list of all ancestors starting with [order].
 * By default, the list starts with the parent which is considered an ancestor of [order] `1`.
 * Order `0` starts with this path and `2` with the parent's parent.
 */
public fun Path.ancestors(order: Int = 1): List<Path> =
    ancestorSequence(order).toList()

/**
 * Returns a [Sequence] of all ancestors starting with [order].
 * By default, the sequence starts with the parent which is considered an ancestor of [order] `1`.
 * Order `0` starts with this path and `2` with the parent's parent.
 */
public fun Path.ancestorSequence(order: Int = 1): Sequence<Path> =
    ancestor(order)?.let { start ->
        var ancestor: Path? = start
        generateSequence { ancestor?.also { ancestor = ancestor?.parent } }
    } ?: emptySequence()

/**
 * Requires an [ancestor] of the specified [order] and returns it.
 *
 * Otherwise, an [IllegalArgumentException] is thrown.
 *
 * @see [ancestor]
 */
public fun Path.requireAncestor(order: Int): Path {
    require(order >= 0) { "The ancestor order $order must not be negative." }
    return ancestor(order) ?: throw IllegalArgumentException("No ancestor of order $order")
}

/**
 * Returns the sub path of this path starting at the [order] segments towards the root.
 * That is, `ancestor(n).resolve(subpath(n))` always holds true.
 *
 * @see [ancestor]
 */
@Suppress("SpellCheckingInspection")
public fun Path.subpath(order: Int): Path = subpath(nameCount - order, nameCount)
