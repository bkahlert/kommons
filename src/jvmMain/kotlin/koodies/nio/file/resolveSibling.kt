package koodies.io.file

import koodies.io.path.pathString
import java.nio.file.Path

/**
 * Resolves a sibling by applying [transform] on one of the path segments.
 * Which one is specified by [order] whereas `0` corresponds to the [Path.getFileName].
 *
 * @sample Samples.resolveSibling
 */
public fun Path.resolveSibling(order: Int = 1, transform: Path.() -> Path): Path {
    val ancestor = requireAncestor(order + 1)
    val transformed = getName(nameCount - order - 1).transform()
    val resolve = ancestor.resolve(transformed)
    return if (order > 0) resolve.resolve(subpath(order)) else resolve
}


private object Samples {

    fun resolveSibling() {
        val path = Path.of("a/b/c")
        val sibling = path.resolveSibling { resolveSibling(fileName.pathString + "-x") }
        println(sibling) // Path.of("/a/b-x/c")
    }
}
