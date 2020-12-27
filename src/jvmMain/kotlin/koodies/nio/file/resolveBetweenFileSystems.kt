package koodies.io.file

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Resolves the specified [path] against this path
 * whereas [path] may be of a different [FileSystem] than
 * the one of this path.
 *
 * If the [FileSystem] is the same, [Path.resolve] is used.
 * Otherwise this [FileSystem] is used—unless [path] is [absolute][Path.isAbsolute].
 *
 * In other words: [path] is resolved against this path's file system.
 * So the resolved path will reside in this path's file system, too.
 * The only exception is if [path] is absolute. Since
 * an absolute path is already "fully-qualified" it is
 * the resolved result *(and its file system the resulting file system)*.
 */
fun Path.resolveBetweenFileSystems(path: Path): Path =
    when {
        fileSystem == path.fileSystem -> resolve(path)
        path.isAbsolute -> path
        else -> path.fold(this) { acc, segment -> acc.resolve("$segment") }
    }

