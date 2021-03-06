package koodies.io.file

import java.nio.file.Path

/**
 * Returns the sub path of this path starting at the [order] segments towards the root.
 * That is, `ancestor(n).resolve(subpath(n))` always holds true.
 *
 * @see [ancestor]
 */
@Suppress("SpellCheckingInspection")
public fun Path.subpath(order: Int): Path = subpath(nameCount - order, nameCount)
