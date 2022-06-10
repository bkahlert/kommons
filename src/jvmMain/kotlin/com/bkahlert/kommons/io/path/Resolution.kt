package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.ClassPath
import java.net.URI
import java.net.URL
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Attempts to parse [this@toPath] as an [URI] and convert it to a [Path].
 *
 * If parsing fails, converts this string to a [Path].
 *
 * @see Paths.get
 */
public fun String.asPath(): Path =
    kotlin.runCatching { URI.create(this).asPath() }
        .recover {
            if (startsWith("classpath:")) ClassPath(this)
            else Paths.get(this)
        }.getOrThrow()

/**
 * Converts the given URI to a [Path].
 *
 * @see Paths.get
 */
public fun URI.asPath(): Path =
    Paths.get(this)

// TODO delete
private val lock = ReentrantLock()

// TODO delete
private fun <T> URI.internalToMappedPath(transform: (Path) -> T): T =
    runCatching { transform(Paths.get(this)) }
        .recoverCatching {
            if (it !is FileSystemNotFoundException) throw it
            FileSystems.newFileSystem(this, emptyMap<String, Any>()).use { fs ->
                transform(fs.provider().getPath(this))
            }
        }.getOrThrow()
// TODO delete
/**
 * Gets the [Path] this [URI] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] this function does not
 * only check the default file system but also loads the needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
public fun <T> URI.threadSafeToMappedPath(transform: (Path) -> T): T =
    lock.withLock { internalToMappedPath(transform) }

/**
 * Gets the [Path] this [URI] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] this function does not
 * only check the default file system but also loads the needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
@Deprecated("replace with usePath", replaceWith = ReplaceWith("usePath(transform)", "com.bkahlert.kommons.usePath"))
public inline fun <reified T> URI.toMappedPath(noinline transform: (Path) -> T): T =
    threadSafeToMappedPath(transform)

/**
 * Gets the [Path] this [URL] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] this function does not
 * only check the default file system but also loads the needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
@Deprecated("replace with usePath", replaceWith = ReplaceWith("usePath(transform)", "com.bkahlert.kommons.usePath"))
public inline fun <reified T> URL.toMappedPath(noinline transform: (Path) -> T): T =
    toURI().toMappedPath(transform)

/**
 * Resolves the specified [path] against this path
 * whereas [path] may be of a different [FileSystem] than
 * the one of this path.
 *
 * If the [FileSystem] is the same, [Path.resolve] is used.
 * Otherwise, this [FileSystem] is used—unless [path] is [absolute][Path.isAbsolute].
 *
 * In other words: [path] is resolved against this path's file system.
 * So the resolved path will reside in this path's file system, too.
 * The only exception is if [path] is absolute. Since
 * an absolute path is already "fully-qualified" it is
 * the resolved result *(and its file system the resulting file system)*.
 */
@Deprecated("replace with kommons-debug", replaceWith = ReplaceWith("resolveBetweenFileSystems(path)", "com.bkahlert.kommons.resolveBetweenFileSystems"))
public fun Path.resolveBetweenFileSystems(path: Path): Path =
    when {
        fileSystem == path.fileSystem -> resolve(path)
        path.isAbsolute -> path
        else -> path.fold(this) { acc, segment -> acc.resolve("$segment") }
    }

/**
 * Resolves a sibling by applying [transform] on one of the path segments.
 * Which one is specified by [order] whereas `0` corresponds to the [Path.getFileName].
 */
public fun Path.resolveSibling(order: Int = 1, transform: Path.() -> Path): Path {
    val ancestor = requireAncestor(order + 1)
    val transformed = getName(nameCount - order - 1).transform()
    val resolve = ancestor.resolve(transformed)
    return if (order > 0) resolve.resolve(subpath(order)) else resolve
}
