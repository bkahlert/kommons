package koodies.io

import koodies.asString
import koodies.io.path.Defaults
import koodies.io.path.age
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.isEmpty
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.randomDirectory
import koodies.io.path.randomFile
import koodies.io.path.randomPath
import koodies.io.path.requireTempSubPath
import koodies.jvm.runOnExit
import koodies.time.hours
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.setPosixFilePermissions
import kotlin.time.Duration

/**
 * An automatically cleaned temporary directory.
 *
 * When the program exits, [path] is checked if the number of contained files
 * exceeds [maximumFileCount] (default: 100). If so, files are deleted the following way:
 * - Files are deleted starting with the oldest files, that is, the most recently created file is deleted the last.
 * - Files younger than [minAge] (default: 1h) are never deleted—no matter the [maximumFileCount].
 * - Files are deleted until either there are no more than [maximumFileCount] files
 *   or all remaining files are younger than [minAge].
 *
 * Because the cleaning up is done automatically and affects potentially a huge number
 * of files [path] is required to be located somewhere inside of [Locations.Temp].
 */
public class TempDirectory(
    /**
     * Location of this temporary directory.
     */
    public val path: Path,

    /**
     * On cleanup, files younger than [Duration] are not deleted (default: 1h).
     */
    private val minAge: Duration = 1.hours,

    /**
     * On cleanup, this value defines the maximum number of kept files (default: 100).
     */
    private val maximumFileCount: Int = 100,
) {
    init {
        path.requireTempSubPath()
        if (path.exists()) {
            require(path.isDirectory()) { "$path already exists but is no directory." }
        } else {
            path.createDirectory()
        }
        path.setPosixFilePermissions(Defaults.OWNER_ALL_PERMISSIONS)
        runOnExit { cleanUp() }
    }

    /**
     * Converts the given [path] string to a {@code Path} and resolves it against
     * this temporary directory.
     */
    public fun resolve(path: String): Path = this.path.resolve(path)

    /**
     * Creates a directory inside this temporary directory.
     *
     * The POSIX permissions are set to `700`.
     */
    public fun tempDir(base: String = "", extension: String = ""): Path =
        path.tempDir(base, extension)

    /**
     * Creates a file inside this temporary directory.
     *
     * The POSIX permissions are set to `700`.
     */
    public fun tempFile(base: String = "", extension: String = ""): Path =
        path.tempFile(base, extension)

    /**
     * Cleans up this temporary directory as described by [TempDirectory].
     */
    public fun cleanUp(): Path = path.apply {

        listDirectoryEntriesRecursively()
            .filter { it.exists() }
            .sortedBy { it.age }
            .filter { it.age >= minAge }
            .drop(maximumFileCount)
            .forEach { it.delete() }

        listDirectoryEntriesRecursively()
            .filter { it.isDirectory() }
            .filter { it.isEmpty() }
            .forEach { it.delete() }

        if (listDirectoryEntriesRecursively().isEmpty()) {
            delete()
        }
    }

    override fun toString(): String = asString {
        ::path to path
        ::minAge to minAge
        ::maximumFileCount to maximumFileCount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TempDirectory

        if (path != other.path) return false
        if (minAge != other.minAge) return false
        if (maximumFileCount != other.maximumFileCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + minAge.hashCode()
        result = 31 * result + maximumFileCount
        return result
    }
}

/**
 * Creates a temporary directory inside of [Locations.Temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempDir(base: String = "", extension: String = ""): Path {
    val randomPath = Locations.Temp.randomPath(base, extension).requireTempSubPath()
    return randomPath.createDirectories().apply {
        setPosixFilePermissions(Defaults.OWNER_ALL_PERMISSIONS)
    }
}

/**
 * Creates a temporary file inside of [Locations.Temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempFile(base: String = "", extension: String = ""): Path {
    val randomPath = Locations.Temp.randomPath(base, extension).requireTempSubPath()
    return randomPath.createFile().apply {
        setPosixFilePermissions(Defaults.OWNER_ALL_PERMISSIONS)
    }
}

/**
 * Creates a temporary directory inside `this` temporary directory.
 *
 * The POSIX permissions are set to `700`.
 */
public fun Path.tempDir(base: String = "", extension: String = ""): Path =
    requireTempSubPath().randomDirectory(base, extension)

/**
 * Creates a temporary file inside `this` temporary directory.
 *
 * The POSIX permissions are set to `700`.
 */
public fun Path.tempFile(base: String = "", extension: String = ""): Path =
    requireTempSubPath().randomFile(base, extension)


/**
 * Creates a temporary directory, runs the given [block] inside of it
 * and deletes it and all of its content right after.
 *
 * The POSIX permissions are set to `700`.
 */
public fun <T> withTempDir(base: String = "", extension: String = "", block: Path.() -> T): T =
    Locations.FilesTemp.tempDir(base, extension).run {
        val returnValue: T = block()
        deleteRecursively()
        returnValue
    }
