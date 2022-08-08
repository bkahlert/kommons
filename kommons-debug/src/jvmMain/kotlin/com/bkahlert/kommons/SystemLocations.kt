package com.bkahlert.kommons

import com.bkahlert.kommons.text.EMPTY
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration

/** Typical locations on this system. */
public object SystemLocations {

    /**
     * Working directory, that is, the directory in which this binary is located.
     */
    public val Work: Path by lazy { FileSystems.getDefault().getPath(String.EMPTY).toAbsolutePath() }

    /**
     * Home directory of the logged-in user.
     */
    public val Home: Path by lazy { Paths.get(System.getProperty("user.home")) }

    /**
     * Directory, in which temporary data can be stored.
     */
    public val Temp: Path by lazy { Paths.get(System.getProperty("java.io.tmpdir")) }

    /** Directory of the running Java distribution. */
    public val JavaHome: Path by lazy { Paths.get(System.getProperty("java.home")) }
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
