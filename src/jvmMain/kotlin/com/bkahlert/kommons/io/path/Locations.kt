package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import com.bkahlert.kommons.text.randomString
import com.bkahlert.kommons.text.takeUnlessEmpty
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable
import kotlin.io.path.setPosixFilePermissions
import kotlin.time.Duration

/**
 * A couple of well known locations.
 */
public interface Locations {

    /**
     * Working directory, that is, the directory in which this binary is located.
     */
    public val work: Path get() = Companion.work

    /**
     * Home directory of the currently logged in user.
     */
    public val home: Path get() = Companion.home

    /**
     * Directory in which temporary data can be stored.
     */
    public val temp: Path get() = Companion.temp

    public companion object {

        /**
         * Working directory, that is, the directory in which this binary is located.
         */
        public val work: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

        /**
         * Home directory of the currently logged in user.
         */
        public val home: Path = Path.of(System.getProperty("user.home"))

        /**
         * Directory in which temporary data can be stored.
         */
        public val temp: Path = Paths.get("/tmp").takeIf { it.isWritable() } ?: Path.of(System.getProperty("java.io.tmpdir"))
    }
}

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
public fun Path.createParentDirectories(): Path = apply { parent.takeUnless { it.exists() }?.createDirectories() }


/**
 * Returns this [Path] with a path segment added.
 *
 * The path segment is created based on [base] and [extension] and a random
 * string in between.
 *
 * The newly created [Path] is guaranteed to not already exist.
 */
public tailrec fun Path.randomPath(base: String = randomString(4), extension: String = ""): Path {
    val minLength = 6
    val length = base.length + extension.length
    val randomSuffix = randomString((minLength - length).coerceAtLeast(3))
    val randomPath = resolve("${base.takeUnlessEmpty()?.let { "$it--" } ?: ""}$randomSuffix$extension")
    return randomPath.takeUnless { it.exists() } ?: randomPath(base, extension)
}


/*
 * Random directories / files
 */

/**
 * Creates a random directory inside this [Path].
 *
 * Eventually missing directories are automatically created.
 */
public fun Path.randomDirectory(base: String = randomString(4), extension: String = "-tmp"): Path =
    randomPath(base, extension).createDirectories()

/**
 * Creates a random file inside this [Path].
 *
 * Eventually missing directories are automatically created.
 */
public fun Path.randomFile(base: String = randomString(4), extension: String = ".tmp"): Path =
    randomPath(base, extension).createParentDirectories().createFile()


/*
 * Temporary directories / files
 */

/**
 * Creates a temporary directory inside of [Locations.temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempDir(base: String = "", extension: String = ""): Path =
    Locations.temp.tempDir(base, extension)

/**
 * Creates a temporary file inside of [Locations.temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempFile(base: String = "", extension: String = ""): Path =
    Locations.temp.tempFile(base, extension)

/**
 * Creates a temporary directory inside `this` temporary directory.
 *
 * The POSIX permissions are set to `700`.
 *
 * Attempting to create a temporary directory outside of [Locations.temp] will
 * throw an [IllegalArgumentException].
 */
public fun Path.tempDir(base: String = "", extension: String = ""): Path =
    requireTempSubPath().randomDirectory(base, extension).apply {
        setPosixFilePermissions(OWNER_ALL_PERMISSIONS)
    }

/**
 * Creates a temporary file inside `this` temporary directory.
 *
 * The POSIX permissions are set to `700`.
 *
 * Attempting to create a temporary directory outside of [Locations.temp] will
 * throw an [IllegalArgumentException].
 */
public fun Path.tempFile(base: String = "", extension: String = ""): Path =
    requireTempSubPath().randomFile(base, extension).apply {
        setPosixFilePermissions(OWNER_ALL_PERMISSIONS)
    }

/**
 * Runs the given [block] with a temporary directory that
 * is automatically deleted on completion.
 */
public fun <T> runWithTempDir(base: String = "", extension: String = "", block: Path.() -> T): T =
    tempDir(base, extension).run {
        val returnValue: T = block()
        deleteRecursively()
        returnValue
    }


/*
 * Misc
 */
private val cleanUpLock = ReentrantLock()

/**
 * Cleans up this directory by
 * deleting files older than the specified [keepAge] and stopping when [keepCount] files
 * are left.
 *
 * Because this process affects a potentially huge number of files,
 * this directory is required to be located somewhere inside of [Locations.temp]
 * if not explicitly specified otherwise.
 */
public fun Path.cleanUp(keepAge: Duration, keepCount: Int, enforceTempContainment: Boolean = true): Path {
    if (enforceTempContainment) requireTempSubPath()

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
                            if (it.isDirectory() && it.isEmpty()) it.delete(NOFOLLOW_LINKS)
                        }
                    }

                kotlin.runCatching { if (isEmpty()) delete(NOFOLLOW_LINKS) }
            }
        } catch (e: UncheckedIOException) {
            if (e.cause !is NoSuchFileException) throw e
        }
    }

    return this
}
