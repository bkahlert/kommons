package com.bkahlert.kommons.io

import com.bkahlert.kommons.EMPTY
import com.bkahlert.kommons.Program
import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.Timestamp
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.text.takeUnlessEmpty
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UncheckedIOException
import java.io.Writer
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.CopyOption
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption.FOLLOW_LINKS
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Stream
import kotlin.concurrent.withLock
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.toPath
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.io.path.writer
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Returns this URL as a [Path] or `null` otherwise. */
public fun URL.toPathOrNull(): Path? = kotlin.runCatching { toPath() }.getOrNull()

/** Returns this URI as a [Path] or `null` otherwise. */
public fun URI.toPathOrNull(): Path? = kotlin.runCatching { toPath() }.getOrNull()

/** Returns this file as a [Path] or `null` otherwise. */
public fun File.toPathOrNull(): Path? = kotlin.runCatching { toPath() }.getOrNull()

/** Returns this URL as a [Path] or throws an exception otherwise. */
public fun URL.toPath(): Path = toURI().toPath()


/** Returns this URL as a [File] or `null` otherwise. */
public fun URL.toFileOrNull(): File? = kotlin.runCatching { toFile() }.getOrNull()

/** Returns this URI as a [File] or `null` otherwise. */
public fun URI.toFileOrNull(): File? = kotlin.runCatching { toFile() }.getOrNull()

/** Returns this path as a [File] or `null` otherwise. */
public fun Path.toFileOrNull(): File? = kotlin.runCatching { toFile() }.getOrNull()

/** Returns this URL as a [File] or throws an exception otherwise. */
public fun URL.toFile(): File = toPath().toFile()

/** Returns this URI as a [File] or throws an exception otherwise. */
public fun URI.toFile(): File = toPath().toFile()


/**
 * Creates an empty file in the directory specified by this path, using
 * the given [prefix] and [suffix] to generate its name.
 *
 * @see createTempFile
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createTempFile(prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path =
    createTempFile(this, prefix, suffix, *attributes)

/**
 * Creates a new directory in the directory specified by this path, using
 * the given [prefix] to generate its name.
 *
 * @see createTempDirectory
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createTempDirectory(prefix: String? = null, vararg attributes: FileAttribute<*>): Path =
    createTempDirectory(this, prefix, *attributes)

/**
 * Creates a new file in the default temp directory, using
 * the given [prefix] and [suffix] to generate its name,
 * the specified [attributes] and [text] encoded using UTF-8 or the specified [charset].
 *
 * @see createTempFile
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun createTempTextFile(
    text: CharSequence,
    charset: Charset = Charsets.UTF_8,
    prefix: String? = null,
    suffix: String? = null,
    vararg attributes: FileAttribute<*>
): Path = createTempFile(prefix, suffix, *attributes).apply { writeText(text, charset) }

/**
 * Creates a new file in the default temp directory, using
 * the given [prefix] and [suffix] to generate its name,
 * the specified [attributes] and [bytes].
 *
 * @see createTempFile
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun createTempBinaryFile(
    bytes: ByteArray,
    prefix: String? = null,
    suffix: String? = null,
    vararg attributes: FileAttribute<*>
): Path = createTempFile(prefix, suffix, *attributes).apply { writeBytes(bytes) }

/**
 * Creates a new file in the directory specified by this path, using
 * the given [prefix] and [suffix] to generate its name,
 * the specified [attributes] and [text] encoded using UTF-8 or the specified [charset].
 *
 * @see createTempFile
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createTempTextFile(
    text: CharSequence,
    charset: Charset = Charsets.UTF_8,
    prefix: String? = null,
    suffix: String? = null,
    vararg attributes: FileAttribute<*>
): Path = createTempFile(prefix, suffix, *attributes).apply { writeText(text, charset) }

/**
 * Creates a new file in the directory specified by this path, using
 * the given [prefix] and [suffix] to generate its name,
 * the specified [attributes] and [bytes].
 *
 * @see createTempFile
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createTempBinaryFile(
    bytes: ByteArray,
    prefix: String? = null,
    suffix: String? = null,
    vararg attributes: FileAttribute<*>
): Path = createTempFile(prefix, suffix, *attributes).apply { writeBytes(bytes) }

/**
 * Creates a new file specified by this path, using
 * the specified [attributes] and [text] encoded using UTF-8 or the specified [charset],
 * failing if the file already exists.
 *
 * @see createFile
 * @see writeText
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createTextFile(
    text: CharSequence,
    charset: Charset = Charsets.UTF_8,
    vararg attributes: FileAttribute<*>
): Path = createFile(*attributes).apply { writeText(text, charset) }

/**
 * Creates a new file specified by this path, using
 * the specified [attributes] and [bytes],
 * failing if the file already exists.
 *
 * @see createFile
 * @see writeBytes
 */
@Suppress("NOTHING_TO_INLINE")
@Throws(IOException::class)
public inline fun Path.createBinaryFile(
    bytes: ByteArray,
    vararg attributes: FileAttribute<*>
): Path = createFile(*attributes).apply { writeBytes(bytes) }


/**
 * Checks if the file located by the **normalized** path is a directory.
 *
 * By default, symbolic links in the path are followed.
 *
 * @param options options to control how symbolic links are handled.
 *
 * @see Files.isDirectory
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.isNormalizedDirectory(vararg options: LinkOption): Boolean =
    Files.isDirectory(normalize(), *options)

/**
 * Checks if the [file] located by the **normalized** path is a directory,
 * or throws an [IllegalArgumentException] otherwise.
 */
public fun requireNormalizedDirectory(file: Path, vararg options: LinkOption): Path =
    file.apply { require(isNormalizedDirectory(*options)) { "${normalize()} is no directory" } }

/**
 * Checks if the [file] located by the **normalized** path is a directory,
 * or throws an [IllegalStateException] otherwise.
 */
public fun checkNormalizedDirectory(file: Path, vararg options: LinkOption): Path =
    file.apply { check(isNormalizedDirectory(*options)) { "${normalize()} is no directory" } }

/**
 * Checks if the [file] located by the **normalized** path is **no** directory,
 * or throws an [IllegalArgumentException] otherwise.
 */
public fun requireNoDirectoryNormalized(file: Path, vararg options: LinkOption): Path =
    file.apply { require(!isNormalizedDirectory(*options)) { "${normalize()} is a directory" } }

/**
 * Checks if the [file] located by the **normalized** path is **no** directory,
 * or throws an [IllegalStateException] otherwise.
 */
public fun checkNoDirectoryNormalized(file: Path, vararg options: LinkOption): Path =
    file.apply { check(!isNormalizedDirectory(*options)) { "${normalize()} is a directory" } }


/** Alias for [isSubPathOf]. */
public fun Path.isInside(path: Path): Boolean = isSubPathOf(path)

/** Returns whether this path is a sub path of [path]. */
public fun Path.isSubPathOf(path: Path): Boolean =
    normalize().toAbsolutePath().startsWith(path.normalize().toAbsolutePath())


/**
 * Returns this [Path] with all parent directories created.
 *
 * Example: if directory `/some/where` existed and this method was called on `/some/where/resides/a/file`,
 * the missing directories `/some/where/resides` and `/some/where/resides/a` would be created.
 */
public fun Path.createParentDirectories(): Path = apply { parent?.takeUnless { it.exists() }?.createDirectories() }

/**
 * The duration passed since when this file was last modified.
 */
public var Path.age: Duration
    get() :Duration = (Timestamp - getLastModifiedTime().toMillis()).milliseconds
    set(value) {
        setLastModifiedTime(FileTime.from(Instant.now().minusMillis(value.inWholeMilliseconds)))
    }

/**
 * This path's creation time.
 */
public var Path.created: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().creationTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:creationTime", fileTime)
    }

/**
 * This path's last accessed time.
 */
public var Path.lastAccessed: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().lastAccessTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:lastAccessTime", fileTime)
    }

/**
 * This path's last modified time.
 */
public var Path.lastModified: FileTime
    get() = Files.getLastModifiedTime(this)
    set(fileTime) {
        Files.setLastModifiedTime(this, fileTime)
    }


/**
 * Resolves the specified [path] against this path
 * whereas [path] may be of a different [FileSystem] than
 * the one of this path.
 *
 * If the [FileSystem] is the same, [Path.resolve] is used.
 * Otherwise, this [FileSystem] is used—unless [path] is [absolute][Path.isAbsolute].
 *
 * In other words: [path] is resolved against this path's file system.
 * So the resolved path resides in this path's file system, too.
 * The only exception is if [path] is absolute. Since
 * an absolute path is already "fully qualified" it's
 * returned as is.
 */
public fun Path.resolveBetweenFileSystems(path: Path): Path =
    when {
        fileSystem == path.fileSystem -> resolve(path)
        path.isAbsolute -> path
        else -> path.fold(this) { acc, segment -> acc.resolve("$segment") }
    }


/**
 * Returns this [Path] with a path segment added.
 *
 * The path segment is created based on [prefix] and [suffix],
 * joined with a random string.
 *
 * The newly created [Path] is guaranteed to not already exist.
 */
public tailrec fun Path.resolveRandom(prefix: String = randomString(4), suffix: String = String.EMPTY): Path {
    val minLength = 6
    val length = prefix.length + suffix.length
    val randomSuffix = randomString((minLength - length).coerceAtLeast(3))
    val randomPath = resolve("${prefix.takeUnlessEmpty()?.let { "$it--" } ?: String.EMPTY}$randomSuffix$suffix")
    return randomPath.takeUnless { it.exists() } ?: resolveRandom(prefix, suffix)
}


/**
 * Returns a path based on the following rules:
 * - If this path **is no directory** it's returned.
 * - If this path **is a directory** the filename returned by the specified [computeFileName] relative to this directory is returned.
 *
 * Use [options] to control how symbolic links are handled.
 */
public fun Path.resolveFile(vararg options: LinkOption, computeFileName: () -> Path): Path =
    if (isNormalizedDirectory(*options)) checkNoDirectoryNormalized(resolve(computeFileName()), *options) else this

/**
 * Returns a path based on the following rules:
 * - If this path **is no directory** it's returned.
 * - If this path **is a directory** the specified [fileName] relative to this directory is returned.
 *
 * Use [options] to control how symbolic links are handled.
 */
public fun Path.resolveFile(fileName: Path, vararg options: LinkOption): Path =
    if (isNormalizedDirectory(*options)) checkNoDirectoryNormalized(resolve(fileName), *options) else this

/**
 * Returns a path based on the following rules:
 * - If this path **is no directory** it's returned.
 * - If this path **is a directory** the specified [fileName] relative to this directory is returned.
 *
 * Use [options] to control how symbolic links are handled.
 */
public fun Path.resolveFile(fileName: String, vararg options: LinkOption): Path =
    if (isNormalizedDirectory(*options)) checkNoDirectoryNormalized(resolve(fileName), *options) else this


private fun Path.getPathMatcher(glob: String): PathMatcher? {
    // avoid creating a matcher if all entries are required.
    if (glob == "*" || glob == "**" || glob == "**/*") return null

    // create a matcher and return a filter that uses it.
    return fileSystem.getPathMatcher("glob:$glob")
}

private fun Path.streamContentsRecursively(glob: String = "*", vararg options: LinkOption): Stream<Path> {
    if (!isDirectory(*options)) throw NotDirectoryException(pathString)
    val fileVisitOptions = options.let { if (it.contains(LinkOption.NOFOLLOW_LINKS)) emptyArray() else arrayOf(FOLLOW_LINKS) }
    val walk = Files.walk(this, *fileVisitOptions).filter { it != this }
    return getPathMatcher(glob)
        ?.let { matcher -> walk.filter { path -> matcher.matches(path) } }
        ?: walk
}

/**
 * Returns a list of the entries in this directory and its subdirectories
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path doesn't refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.walk
 */
public fun Path.listDirectoryEntriesRecursively(glob: String = "*", vararg options: LinkOption): List<Path> =
    streamContentsRecursively(glob, *options).asSequence().toList()

/**
 * Calls the [block] callback with a sequence of all entries in this directory
 * and its subdirectories optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path doesn't refer to a directory.
 * @throws IOException If an I/O error occurs.
 * @return the value returned by [block].
 *
 * @see Files.walk
 */
public fun <T> Path.useDirectoryEntriesRecursively(glob: String = "*", vararg options: LinkOption, block: (Sequence<Path>) -> T): T =
    streamContentsRecursively(glob, *options).use { block(it.asSequence()) }

/**
 * Performs the given [action] on each entry in this directory and its subdirectories
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path doesn't refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.walk
 */
public fun Path.forEachDirectoryEntryRecursively(glob: String = "*", vararg options: LinkOption, action: (Path) -> Unit): Unit =
    streamContentsRecursively(glob, *options).use { it.forEach(action) }


/**
 * Copies a file or directory located by this path to the given [target]
 * directory to a path with the same [Path.getFileName] as this one.
 *
 * @see Path.copyTo
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.copyToDirectory(
    target: Path,
    overwrite: Boolean = false,
    createDirectories: Boolean = false
): Path {
    if (createDirectories) target.createDirectories()
    return copyTo(target.resolve(fileName.pathString), overwrite)
}

/**
 * Copies a file or directory located by this path to the given [target]
 * directory to a path with the same [Path.getFileName] as this one.
 *
 * @see Path.copyTo
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.copyToDirectory(
    target: Path,
    vararg options: CopyOption,
    createDirectories: Boolean = false
): Path {
    if (createDirectories) target.createDirectories()
    return copyTo(target.resolve(fileName.pathString), *options)
}


/**
 * Deletes this file or empty directory.
 *
 * Returns the deleted path.
 */
public fun Path.delete(vararg options: LinkOption): Path =
    apply { if (exists(*options)) Files.delete(this) }

/**
 * Deletes this file or directory recursively.
 *
 * Symbolic links aren't followed but deleted themselves.
 *
 * Returns the deleted path.
 */
public fun Path.deleteRecursively(vararg options: LinkOption, predicate: (Path) -> Boolean = { true }): Path =
    apply {
        if (exists(*options, LinkOption.NOFOLLOW_LINKS)) {
            if (isDirectory(*options, LinkOption.NOFOLLOW_LINKS)) {
                forEachDirectoryEntry { it.deleteRecursively(*options, LinkOption.NOFOLLOW_LINKS, predicate = predicate) }
            }

            if (predicate(this)) {
                var maxAttempts = 3
                var ex: Throwable? = kotlin.runCatching { delete(*options, LinkOption.NOFOLLOW_LINKS) }.exceptionOrNull()
                while (ex != null && maxAttempts > 0) {
                    maxAttempts--
                    if (ex is DirectoryNotEmptyException) {
                        val files = listDirectoryEntriesRecursively(options = options)
                        files.forEach { it.deleteRecursively(*options, predicate = predicate) }
                    }
                    Thread.sleep(100)
                    ex = kotlin.runCatching { delete(*options, LinkOption.NOFOLLOW_LINKS) }.exceptionOrNull()
                }
                if (ex != null) throw ex
            }
        }
    }

/**
 * Deletes the contents of this directory.
 *
 * Throws if this is no directory.
 */
public fun Path.deleteDirectoryEntriesRecursively(predicate: (Path) -> Boolean = { true }): Path =
    apply { listDirectoryEntriesRecursively().forEach { it.deleteRecursively(predicate = predicate) } }

/** Deletes this file when the virtual machine shuts down. */
public fun Path.deleteOnExit(recursively: Boolean = true): Path {
    val file = toFile()
    Program.onExit {
        if (recursively) file.deleteRecursively()
        else file.delete()
    }
    return this
}


private val cleanUpLock = ReentrantLock()

/**
 * Cleans up this directory by
 * deleting files older than the specified [keepAge] and stopping when [keepCount] files
 * are left.
 *
 * Because this process might affect a huge number of files,
 * this directory needs to be somewhere inside of [SystemLocations.Temp]
 * if not explicitly specified otherwise.
 */
// TODO test
public fun Path.cleanUp(keepAge: Duration, keepCount: Int, enforceTempContainment: Boolean = true): Path {
    if (enforceTempContainment) requireTempSubPath(this)

    cleanUpLock.withLock {
        try {
            if (exists()) {
                listDirectoryEntriesRecursively()
                    .mapNotNull { kotlin.runCatching { if (!it.isDirectory()) it to it.age else null }.getOrNull() }
                    .sortedBy { (_, age) -> age }
                    .filter { (_, age) -> age >= keepAge }
                    .drop(keepCount)
                    .forEach { (file, _) -> file.runCatching { delete(NOFOLLOW_LINKS) } }

                listDirectoryEntriesRecursively()
                    .forEach {
                        kotlin.runCatching {
                            if (it.isDirectory() && it.listDirectoryEntries().isEmpty()) it.delete(NOFOLLOW_LINKS)
                        }
                    }

                kotlin.runCatching { if (listDirectoryEntries().isEmpty()) delete(NOFOLLOW_LINKS) }
            }
        } catch (e: UncheckedIOException) {
            if (e.cause !is NoSuchFileException) throw e
        }
    }

    return this
}


/**
 * Runs the given [block] with a temporary directory that
 * is automatically deleted on completion.
 */
public fun <T> withTempDirectory(prefix: String = String.EMPTY, block: Path.() -> T): T =
    createTempDirectory(prefix).run {
        val result = runCatching(block)
        deleteRecursively()
        result.getOrThrow()
    }

/**
 * Checks if this path is inside of one of the System's temporary directories,
 * or throws an [IllegalArgumentException] otherwise.
 */
public fun requireTempSubPath(path: Path): Path =
    path.apply {
        require(fileSystem != FileSystems.getDefault() || isSubPathOf(SystemLocations.Temp)) {
            "${normalize().toAbsolutePath()} isn't inside ${SystemLocations.Temp}."
        }
    }


/**
 * Calls the specified [block] callback
 * giving it a new [InputStream] of this file
 * and returns the result.
 *
 * The [InputStream] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [READ] options.
 */
public inline fun <R> Path.useInputStream(vararg options: OpenOption, block: (InputStream) -> R): R =
    inputStream(*options).use(block)

/**
 * Calls the specified [block] callback
 * giving it a new [BufferedInputStream] of this file
 * and returns the result.
 *
 * The [BufferedInputStream] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [READ]
 * options.
 */
public inline fun <R> Path.useBufferedInputStream(
    vararg options: OpenOption,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    block: (BufferedInputStream) -> R
): R = inputStream(*options).buffered(bufferSize).use(block)

/**
 * Calls the specified [block] callback
 * giving it a new [InputStreamReader] of this file
 * and returns the result.
 *
 * The [InputStreamReader] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [READ]
 * options.
 */
public inline fun <R> Path.useReader(
    vararg options: OpenOption,
    charset: Charset = Charsets.UTF_8,
    block: (InputStreamReader) -> R
): R = reader(charset, *options).use(block)

/**
 * Calls the specified [block] callback
 * giving it a new [BufferedReader] of this file
 * and returns the result.
 *
 * The [BufferedReader] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [READ]
 * options.
 */
public inline fun <R> Path.useBufferedReader(
    vararg options: OpenOption,
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    block: (BufferedReader) -> R
): R = bufferedReader(charset, bufferSize, *options).use(block)

/**
 * Calls the specified [block] callback
 * giving it a new [OutputStream] of this file
 * and returns the result.
 *
 * The [OutputStream] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [CREATE], [TRUNCATE_EXISTING], and [WRITE]
 * options.
 */
public inline fun Path.useOutputStream(
    vararg options: OpenOption,
    block: (OutputStream) -> Unit
): Path = apply { outputStream(*options).use(block) }

/**
 * Calls the specified [block] callback
 * giving it a new [BufferedOutputStream] of this file
 * and returns the result.
 *
 * The [BufferedOutputStream] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [CREATE], [TRUNCATE_EXISTING], and [WRITE]
 * options.
 */
public inline fun Path.useBufferedOutputStream(
    vararg options: OpenOption,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    block: (BufferedOutputStream) -> Unit
): Path = apply { outputStream(*options).buffered(bufferSize).use(block) }

/**
 * Calls the specified [block] callback
 * giving it a new [Writer] of this file
 * and returns the result.
 *
 * The [Writer] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [CREATE], [TRUNCATE_EXISTING], and [WRITE]
 * options.
 */
public inline fun Path.useWriter(
    vararg options: OpenOption,
    charset: Charset = Charsets.UTF_8,
    block: (Writer) -> Unit
): Path = apply { writer(charset, *options).use(block) }

/**
 * Calls the specified [block] callback
 * giving it a new [BufferedWriter] of this file
 * and returns the result.
 *
 * The [BufferedWriter] is closed correctly whether an exception is thrown or not.
 *
 * If no [options] are present then it's equal to open the file with
 * the [CREATE], [TRUNCATE_EXISTING], and [WRITE]
 * options.
 */
public inline fun Path.useBufferedWriter(
    vararg options: OpenOption,
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    block: (BufferedWriter) -> Unit
): Path = apply { bufferedWriter(charset, bufferSize, *options).use(block) }
