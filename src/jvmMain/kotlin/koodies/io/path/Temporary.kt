package koodies.io.path

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.setPosixFilePermissions

/**
 * Creates a temporary directory inside of [Locations.Temp].
 *
 * The permissions POSIX are set to `700`.
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
 * The permissions POSIX are set to `700`.
 */
public fun tempFile(base: String = "", extension: String = ""): Path {
    val randomPath = Locations.Temp.randomPath(base, extension).requireTempSubPath()
    return randomPath.createFile().apply {
        setPosixFilePermissions(Defaults.OWNER_ALL_PERMISSIONS)
    }
}
