package koodies.io.path

import koodies.io.classPath
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
 * @see Path.of
 */
fun String.toPath(): Path =
    kotlin.runCatching { URI.create(this).toPath() }
        .recover {
            if (startsWith("classpath:")) {
                val delegate by classPath(this)
                delegate
            } else Paths.get(this)
        }.getOrThrow()

/**
 * Converts the given URI to a [Path].
 *
 * @see Paths.get
 * @see Path.of
 */
fun URI.toPath(): Path =
    Paths.get(this)


private val lock = ReentrantLock()

private fun <T> URI.internalToMappedPath(transform: (Path) -> T): T =
    runCatching { transform(Paths.get(this)) }
        .recoverCatching {
            if (it !is FileSystemNotFoundException) throw it
            FileSystems.newFileSystem(this, emptyMap<String, Any>()).use { fs ->
                transform(fs.provider().getPath(this))
            }
        }.getOrThrow()

/**
 * Gets the [Path] this [URI] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] and [Path.of] this function does not
 * only check the default file system but also loads to needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
fun <T> URI.threadSafeToMappedPath(transform: (Path) -> T): T =
    lock.withLock { internalToMappedPath(transform) }

/**
 * Gets the [Path] this [URI] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] and [Path.of] this function does not
 * only check the default file system but also loads to needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
inline fun <reified T> URI.toMappedPath(noinline transform: (Path) -> T): T =
    threadSafeToMappedPath(transform)

/**
 * Gets the [Path] this [URL] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] and [Path.of] this function does not
 * only check the default file system but also loads to needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
inline fun <reified T> URL.toMappedPath(noinline transform: (Path) -> T): T =
    toURI().toMappedPath(transform)
