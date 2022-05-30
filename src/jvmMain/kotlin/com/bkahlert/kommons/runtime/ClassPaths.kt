package com.bkahlert.kommons.io

import com.bkahlert.kommons.io.path.WrappedPath
import com.bkahlert.kommons.io.path.asReadOnly
import com.bkahlert.kommons.io.path.resolveBetweenFileSystems
import com.bkahlert.kommons.io.path.toMappedPath
import com.bkahlert.kommons.runtime.contextClassLoader
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.properties.ReadOnlyProperty

/**
 * Attempts to load the [Class] with the given [name] using this [ClassLoader].
 *
 * Returns `null` if the class can't be loaded.
 */
public fun ClassLoader.loadClassOrNull(name: String): Class<*>? = kotlin.runCatching { loadClass(name) }.getOrNull()

/**
 * Gets the class path resources, the specified [path] points to and applies [transform] to each.
 *
 * Also this function does its best to avoid write access by wrapping the
 * actual [FileSystem] with a write protection layer. **Write protection
 * also covers paths generated from the one provided during the [transform] call.**
 *
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
public inline fun <reified T> useClassPaths(path: String, crossinline transform: (Path) -> T): List<T> {
    val normalizedPath = path.removePrefix("classpath:").removePrefix("/")
    return contextClassLoader.getResources(normalizedPath).asSequence().map { url ->
        url.toMappedPath { classPath -> transform(classPath.asReadOnly()) }
    }.toList()
}

/**
 * Gets the class path resource, the specified [path] points to and applies [transform] to it.
 *
 * Also this function does its best to avoid write access by wrapping the
 * actual [FileSystem] with a write protection layer. **Write protection
 * also covers paths generated from the one provided during the [transform] call.**
 *
 * **This function only returns one match out of possibly many. Use [useClassPaths] to get all.**
 *
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 * @see useClassPaths
 */
public inline fun <reified T> useClassPath(path: String, crossinline transform: (Path) -> T): T? {
    val normalizedPath = path.removePrefix("classpath:").removePrefix("/")
    return contextClassLoader.getResource(normalizedPath)?.toMappedPath { transform(it.asReadOnly()) }
}

/**
 * Gets the class path resource, the specified [path] points to and applies [transform] to it.
 *
 * In contrast to [useClassPath] this function throws if no resource could be found.
 *
 * @see useClassPath
 */
public inline fun <reified T> useRequiredClassPath(path: String, crossinline transform: (Path) -> T): T =
    useClassPath(path, transform) ?: throw noSuchFile(path)

/**
 * Reads the class path resource, the specified [path] points to and returns it.
 *
 * Returns `null` if no resource could be found.
 *
 * @see requireClassPathText
 */
public fun readClassPathText(path: String): String? =
    useClassPath(path) { it.readText() }

/**
 * Reads the class path resource, the specified [path] points to and returns it.
 *
 * Throws an exception throws if no resource could be found.
 *
 * @see readClassPathText
 */
public fun requireClassPathText(path: String): String =
    useRequiredClassPath(path) { it.readText() }

/**
 * Reads the class path resource, the specified [path] points to and returns it.
 *
 * Returns `null` if no resource could be found.
 *
 * @see requireClassPathBytes
 */
public fun readClassPathBytes(path: String): ByteArray? =
    useClassPath(path) { it.readBytes() }

/**
 * Reads the class path resource, the specified [path] points to and returns it.
 *
 * Throws an exception throws if no resource could be found.
 *
 * @see readClassPathBytes
 */
public fun requireClassPathBytes(path: String): ByteArray =
    useRequiredClassPath(path) { it.readBytes() }

/**
 * Gets a proxied class path resource that get only accesses the moment
 * an operation is executed. Should a [FileSystem] need to be loaded this
 * will be done transparently as it will be closed afterwards.
 */
public fun classPath(path: String): ReadOnlyProperty<Any?, Path> = ReadOnlyProperty<Any?, Path> { _, _ -> DelegatingPath(path) }

@JvmInline
private value class DelegatingPath(inline val path: String) : WrappedPath, Path {

    inline fun <reified T> op(crossinline transform: Path.() -> T): T =
        useClassPath(path, transform) ?: throw NoSuchFileException(path, null, "classpath:$path could not be found")

    override val wrappedPath: Path get() = op { this }

    override fun compareTo(other: Path?): Int = op { this.compareTo(other) }
    override fun register(watcher: WatchService?, events: Array<out WatchEvent.Kind<*>>?, vararg modifiers: WatchEvent.Modifier?): WatchKey =
        throw UnsupportedOperationException("No watch support.")

    override fun getFileSystem(): FileSystem = op { fileSystem }
    override fun isAbsolute(): Boolean = op { isAbsolute }
    override fun getRoot(): Path = op { root }
    override fun getFileName(): Path = op { fileName }
    override fun getParent(): Path = op { parent }
    override fun getNameCount(): Int = op { nameCount }
    override fun getName(index: Int): Path = op { getName(index) }
    override fun subpath(beginIndex: Int, endIndex: Int): Path = op { subpath(beginIndex, endIndex) }
    override fun startsWith(other: Path): Boolean = op { startsWith(other) }
    override fun endsWith(other: Path): Boolean = op { endsWith(other) }
    override fun normalize(): Path = op { normalize() }
    override fun resolve(other: Path): Path = op { resolveBetweenFileSystems(other) }
    override fun relativize(other: Path): Path = op { relativize(other) }
    override fun toUri(): URI = op { toUri() }
    override fun toAbsolutePath(): Path = op { toAbsolutePath() }
    override fun toRealPath(vararg options: LinkOption?): Path = op { toRealPath() }
}
