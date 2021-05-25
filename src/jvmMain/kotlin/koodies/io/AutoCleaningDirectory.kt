package koodies.io

import koodies.asString
import koodies.io.path.Defaults
import koodies.io.path.requireTempSubPath
import koodies.jvm.onExit
import koodies.time.hours
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.setPosixFilePermissions
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * A directory that is cleaned when the program exits
 * by deleting files older than the specified [keepAge] and stopping when [keepCount] files
 * are left.
 *
 * Because this process affects a potentially huge number of files,
 * this directory is required to be located somewhere inside of [Locations.Temp]
 * if not explicitly specified otherwise.
 */
public data class AutoCleaningDirectory(

    /**
     * Location of this temporary directory.
     */
    public val path: Path,

    /**
     * On cleanup, files younger than [Duration] are not deleted.
     */
    public val keepAge: Duration = 1.hours,

    /**
     * On cleanup, this value defines the maximum number of kept files.
     */
    public val keepCount: Int = 100,

    /**
     * Whether to check if [path] is located inside of [Locations.Temp].
     */
    public val enforceTempContainment: Boolean = true,
) {
    init {
        path.requireTempSubPath()
        if (path.exists()) {
            require(path.isDirectory()) { "$path already exists but is no directory." }
        } else {
            path.createDirectory()
        }
        path.setPosixFilePermissions(Defaults.OWNER_ALL_PERMISSIONS)
        onExit { cleanUp() }
    }

    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun getValue(thisRef: Any?, property: KProperty<*>): Path = path

    /**
     * Triggers [Path.cleanUp] now.
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun cleanUp(): Path = path.cleanUp(keepAge, keepCount, enforceTempContainment)

    override fun toString(): String = asString {
        ::path to path
        ::keepAge to keepAge
        ::keepCount to keepCount
    }
}

/**
 * Creates a sub-directory [directoryName] in `this` directory that is cleaned when the program exits
 * by deleting files older than the specified [keepAge] and stopping when [keepCount] files
 * are left.
 *
 * Because this process affects a potentially huge number of files,
 * this directory is required to be located somewhere inside of [Locations.Temp]
 * if not explicitly specified otherwise.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.autoCleaning(
    directoryName: String,
    keepAge: Duration = 1.hours,
    keepCount: Int = 100,
    enforceTempContainment: Boolean = true,
): AutoCleaningDirectory =
    AutoCleaningDirectory(resolve(directoryName), keepAge, keepCount, enforceTempContainment)
