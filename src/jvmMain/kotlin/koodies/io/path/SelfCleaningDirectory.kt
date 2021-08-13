package koodies.io.path

import koodies.asString
import koodies.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import koodies.io.path.SelfCleaningDirectory.CleanUpMode
import koodies.runtime.onExit
import koodies.time.hours
import java.nio.file.Path
import kotlin.io.path.createDirectories
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
public data class SelfCleaningDirectory(

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

    /**
     * When cleanup should be done.
     */
    public val cleanUpMode: CleanUpMode = CleanUpMode.OnShutdown,
) {

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

    /** When cleanup should be done. */
    public enum class CleanUpMode(
        /** Whether cleanup should be done on startup. */
        public val onStart: Boolean,
        /** Whether cleanup should be done on shutdown. */
        public val onShutdown: Boolean,
    ) {
        /** Cleanup is done on startup. */
        OnStart(onStart = true, onShutdown = false),

        /** Cleanup is done on shutdown. */
        OnShutdown(onStart = false, onShutdown = true),

        /** Cleanup is done on startup and shutdown. */
        OnStartAndShutdown(onStart = true, onShutdown = true),
    }

    init {
        path.requireTempSubPath()
        if (cleanUpMode.onStart) cleanUp()

        if (path.exists()) {
            require(path.isDirectory()) { "$path already exists but is no directory." }
        } else {
            path.createDirectories()
        }
        path.setPosixFilePermissions(OWNER_ALL_PERMISSIONS)

        if (cleanUpMode.onShutdown) onExit { cleanUp() }
    }
}

/**
 * Cleans `this` directory when the program exits by deleting files older than
 * the specified [keepAge] and stopping when [keepCount] files are left.
 *
 * Because this process affects a potentially huge number of files,
 * this directory is required to be located somewhere inside of [Locations.Temp]
 * if not explicitly specified otherwise.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.selfCleaning(
    keepAge: Duration = 1.hours,
    keepCount: Int = 100,
    enforceTempContainment: Boolean = true,
    cleanUpMode: CleanUpMode = CleanUpMode.OnShutdown,
): SelfCleaningDirectory = SelfCleaningDirectory(this, keepAge, keepCount, enforceTempContainment, cleanUpMode)
