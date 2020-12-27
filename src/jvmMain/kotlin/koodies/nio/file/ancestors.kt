package koodies.io.file

import java.nio.file.Path

/**
 * Returns a list of all ancestors starting with [order].
 * By default the list starts with the parent which is considered an ancestor of [order] `1`.
 * Order `0` starts with this path and `2` with the parent's parent.
 */
fun Path.ancestors(order: Int = 1): List<Path> =
    ancestorSequence(order).toList()
