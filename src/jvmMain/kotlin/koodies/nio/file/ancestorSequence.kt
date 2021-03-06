package koodies.io.file

import java.nio.file.Path

/**
 * Returns a [Sequence] of all ancestors starting with [order].
 * By default the sequence starts with the parent which is considered an ancestor of [order] `1`.
 * Order `0` starts with this path and `2` with the parent's parent.
 */
public fun Path.ancestorSequence(order: Int = 1): Sequence<Path> =
    ancestor(order)?.let { start ->
        var ancestor: Path? = start
        generateSequence { ancestor?.also { ancestor = ancestor?.parent } }
    } ?: emptySequence()
