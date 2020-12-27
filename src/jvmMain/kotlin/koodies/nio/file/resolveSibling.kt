package koodies.io.file

import koodies.functional.letIf
import koodies.io.path.asString
import java.nio.file.Path

/**
 * Resolves a sibling by applying [transform] on one of the path segments.
 * Which one is specified by [order] whereas `0` corresponds to the [Path.getFileName].
 *
 * @sample Samples.resolveSibling
 */
fun Path.resolveSibling(order: Int = 1, transform: Path.() -> Path): Path {
    val ancestor = requireAncestor(order + 1)
    val transformed = getName(nameCount - order - 1).transform()
    return ancestor.resolve(transformed).letIf(order > 0) { it.resolve(subpath(order)) }
}


private object Samples {

    fun resolveSibling() {
        val path = Path.of("a/b/c")
        val sibling = path.resolveSibling { resolveSibling(fileName.asString() + "-x") }
        println(sibling) // Path.of("/a/b-x/c")
    }
}
