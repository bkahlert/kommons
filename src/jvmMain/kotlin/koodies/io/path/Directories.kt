package koodies.io.path

import java.nio.file.Path
import kotlin.io.path.createDirectories


/**
 * Alias for [isSubPathOf].
 */
public fun Path.isInside(path: Path): Boolean = isSubPathOf(path)

/**
 * Returns whether this path is a sub path of [path].
 */
public fun Path.isSubPathOf(path: Path): Boolean =
    normalize().toAbsolutePath().startsWith(path.normalize().toAbsolutePath())

/**
 * Returns this [Path] with all parent directories created.
 *
 * Example: If directory `/some/where` existed and this method was called on `/some/where/resides/a/file`,
 * the missing directories `/some/where/resides` and `/some/where/resides/a` would be created.
 */
public fun Path.withDirectoriesCreated(): Path = also { parent?.createDirectories() }
