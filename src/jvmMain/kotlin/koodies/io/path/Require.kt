package koodies.io.path

import koodies.io.directoryNotEmpty
import koodies.io.fileAlreadyExists
import koodies.io.noSuchFile
import koodies.unit.size
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

/**
 * Requires this to be a directory and throws an [IllegalArgumentException] otherwise.
 */
fun Path.requireDirectory() {
    if (!isDirectory()) throw NotDirectoryException("$this")
}

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is not empty, that is, has size greater zero
 * - this directory is not empty, that is, has entries
 */
fun Path.requireEmpty() {
    if (isNotEmpty) {
        if (isDirectory()) throw directoryNotEmpty()
        throw fileAlreadyExists(this, "Must be empty but has $size.")
    }
}

/**
 * Throws if this [Path] does not exist.
 */
fun Path.requireExists() {
    if (notExists()) throw noSuchFile()
}

/**
 * Throws if this [Path] does exist.
 */
fun Path.requireExistsNot() {
    if (exists()) throw FileAlreadyExistsException(asString())
}

/**
 * Requires this to be a file and throws an [IllegalArgumentException] otherwise.
 */
fun Path.requireFile() {
    require(isRegularFile()) { "$this is no file." }
}

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is empty, that is, has zero size
 * - this directory is empty, that is, has no entries
 */
fun Path.requireNotEmpty() {
    if (isEmpty) {
        if (isDirectory()) throw noSuchFile(this, "Directory must not be empty but has no entries.")
        throw noSuchFile(this, "File must not be empty but has zero size.")
    }
}

/**
 * Checks if this path is inside of one of the System's temporary directories.
 *
 * @throws IllegalArgumentException this path is not inside [Locations.Temp]
 */
fun Path.requireTempSubPath(): Path =
    apply { require(!isDefaultFileSystem() || isSubPathOf(Locations.Temp)) { "${this.normalize().toAbsolutePath()} is not inside ${Locations.Temp}." } }
