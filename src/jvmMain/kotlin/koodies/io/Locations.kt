package koodies.io

import koodies.exec.Process.State.Exited.Failed
import koodies.exec.RendererProviders
import koodies.exec.parse
import koodies.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import koodies.io.path.age
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.isEmpty
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.requireTempSubPath
import koodies.or
import koodies.shell.ShellScript
import koodies.text.Semantics.formattedAs
import koodies.text.randomString
import koodies.text.takeUnlessEmpty
import java.nio.file.FileSystems
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.Paths
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
         * Working directory, that is, the directory in which this binary is located.
         */
        public val WorkingDirectory: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

        /**
         * Home directory of the currently logged in user.
         */
        public val HomeDirectory: Path = Path.of(System.getProperty("user.home"))

        /**
         * Directory in which temporary data can be stored.
         */
        public val Temp: Path = Paths.get("/tmp").takeIf { it.isWritable() } ?: Path.of(System.getProperty("java.io.tmpdir"))
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
 * Creates a temporary directory inside of [Locations.Temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempDir(base: String = "", extension: String = ""): Path =
    Locations.Temp.tempDir(base, extension)

/**
 * Creates a temporary file inside of [Locations.Temp].
 *
 * The POSIX permissions are set to `700`.
 */
public fun tempFile(base: String = "", extension: String = ""): Path =
    Locations.Temp.tempFile(base, extension)

/**
 * Creates a temporary directory inside `this` temporary directory.
 *
 * The POSIX permissions are set to `700`.
 *
 * Attempting to create a temporary directory outside of [Locations.Temp] will
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
 * Attempting to create a temporary directory outside of [Locations.Temp] will
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

/**
 * Resolves [glob] using the system's `ls` command line tool.
 */
public fun Path.ls(glob: String = ""): List<Path> =
    ShellScript("${this.formattedAs.input} $ ls ${glob.formattedAs.input}") { !"ls $glob" }
        .exec.logging(this, renderer = RendererProviders.errorsOnly())
        .parse.columns<Path, Failed>(1) {
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
            .filter { it.exists() && !it.isDirectory() }
            .sortedBy { it.age }
            .filter { it.age >= keepAge }
            .drop(keepCount)
            .forEach { it.delete(NOFOLLOW_LINKS) }

        listDirectoryEntriesRecursively()
            .filter { it.isDirectory() }
            .filter { it.isEmpty() }
            .forEach { it.delete(NOFOLLOW_LINKS) }

        if (listDirectoryEntriesRecursively().isEmpty()) {
            delete(NOFOLLOW_LINKS)
        }
    }

    return this
}
