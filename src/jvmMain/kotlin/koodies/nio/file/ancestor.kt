package koodies.io.file

import java.nio.file.Path

/**
 * Generalized [Path.getParent] whereas a parent is considered an ancestor of [order] `1`.
 * That is, order `0` resolves to this path and `2` to the parent's parent.
 *
 * Consequently the following invariant for this path always holds true: `ancestor(n).resolve(subpath(n))`
 *
 * Returns `null` for inexistent ancestor. No matter for since how many orders.
 *
 * @see [subpath]
 */
@Suppress("SpellCheckingInspection")
public tailrec fun Path.ancestor(order: Int): Path? =
    if (order == 0) this
    else parent?.ancestor(order - 1)
