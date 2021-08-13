package koodies.io.file

import java.nio.file.Path

/**
 * Requires an [ancestor] of the specified [order] and returns it.
 *
 * Otherwise an [IllegalArgumentException] is thrown.
 *
 * @see [ancestor]
 */
public fun Path.requireAncestor(order: Int): Path {
    require(order >= 0) { "The ancestor order $order must not be negative." }
    return ancestor(order) ?: throw IllegalArgumentException("No ancestor of order $order")
}
