package koodies.io

import koodies.exec.Process.State.Exited.Failed
import koodies.exec.parse
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
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.or
import koodies.shell.ShellScript
import koodies.text.Semantics.formattedAs
import koodies.time.days
import koodies.time.hours
import koodies.time.minutes
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.setPosixFilePermissions
import kotlin.time.Duration

/**
 * A couple of well known locations.
 */
public interface Locations {

    /**
     * Working directory, that is, the directory in which this binary can be found.
     */
    public val WorkingDirectory: Path get() = Companion.WorkingDirectory

    /**
     * Home directory of the currently logged in user.
     */
    public val HomeDirectory: Path get() = Companion.HomeDirectory

    /**
     * Directory in which temporary data can be stored.
     */
    public val Temp: Path get() = Companion.Temp

    public companion object {

        /**
         * Working directory, that is, the directory in which this binary can be found.
         */
        public val WorkingDirectory: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

        /**
         * Home directory of the currently logged in user.
         */
        public val HomeDirectory: Path = Path.of(System.getProperty("user.home"))

        /**
         * Directory in which temporary data can be stored.
         */
        public val Temp: Path = Path.of(System.getProperty("java.io.tmpdir"))
    }
}

internal object InternalLocations : Locations {

    /**
     * Directory in which Koodies-specific data can be stored.
     */
    internal val InternalTemp: Path by Locations.Temp.autoCleaning("com.bkahlert.koodies", 30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.autoCleaning("exec", 1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.autoCleaning("files", 10.minutes, 20)
}

/**
 * Creates a temporary directory inside of [Locations.autoCleaning].
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
 * Creates a temporary file inside of [Locations.autoCleaning].
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
    Locations.Temp.tempDir(base, extension).run {
        val returnValue: T = block()
        deleteRecursively()
        returnValue
    }

/**
 * Resolves [glob] using the system's `ls` command line tool.
 */
public fun Path.ls(glob: String = ""): List<Path> =
    ShellScript { !"ls $glob" }.exec.logging(BACKGROUND, this) {
        errorsOnly("${this@ls.formattedAs.input} $ ls ${glob.formattedAs.input}")
    }.parse.columns<Path, Failed>(1) {
        resolve(it[0])
    } or { emptyList() }

/**
 * Cleans up this directory by
 * deleting files older than the specified [keepAge] and stopping when [keepCount] files
 * are left.
 *
 * Because this process affects a potentially huge number of files,
 * this directory is required to be located somewhere inside of [Locations.Temp]
 * if not explicitly specified otherwise.
 */
public fun Path.cleanUp(keepAge: Duration, keepCount: Int, enforceTempContainment: Boolean = true): Path {
    if (enforceTempContainment) requireTempSubPath()

    if (exists()) {
        listDirectoryEntriesRecursively()
            .filter { it.exists() }
            .sortedBy { it.age }
            .filter { it.age >= keepAge }
            .drop(keepCount)
            .forEach { it.delete() }

        listDirectoryEntriesRecursively()
            .filter { it.isDirectory() }
            .filter { it.isEmpty() }
            .forEach { it.delete() }

        if (listDirectoryEntriesRecursively().isEmpty()) {
            delete()
        }
    }

    return this
}
