package koodies.io.path

import java.nio.file.Path
import kotlin.io.path.createDirectories


/**
 * Returns whether this path is inside [path].
 */
fun Path.isInside(path: Path): Boolean =
    normalize().toAbsolutePath().startsWith(path.normalize().toAbsolutePath())

/**
 * Returns this [Path] with all parent directories created.
 *
 * Example: If directory `/some/where` existed and this method was called on `/some/where/resides/a/file`,
 * the missing directories `/some/where/resides` and `/some/where/resides/a` would be created.
 */
fun Path.withDirectoriesCreated() = also { parent?.createDirectories() }
