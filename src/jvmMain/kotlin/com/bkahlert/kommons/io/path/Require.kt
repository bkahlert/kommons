package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.directoryNotEmpty
import com.bkahlert.kommons.io.fileAlreadyExists
import com.bkahlert.kommons.io.noSuchFile
import java.nio.file.FileAlreadyExistsException
import java.nio.file.LinkOption
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

/**
 * Requires this to be a directory and throws an [IllegalArgumentException] otherwise.
 */
public fun Path.requireDirectory(vararg options: LinkOption) {
    if (!isDirectory(*options)) throw NotDirectoryException("$this")
}

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is not empty, that is, has size greater zero
 * - this directory is not empty, that is, has entries
 */
public fun Path.requireEmpty(vararg options: LinkOption) {
    if (isNotEmpty()) {
        if (isDirectory(*options)) throw directoryNotEmpty()
        throw fileAlreadyExists(this, "Must be empty but has ${getSize()}.")
    }
}

/**
 * Throws if this [Path] does not exist.
 */
public fun Path.requireExists(vararg options: LinkOption) {
    if (notExists(*options)) throw noSuchFile()
}

/**
 * Throws if this [Path] does exist.
 */
public fun Path.requireExistsNot(vararg options: LinkOption) {
    if (exists(*options)) throw FileAlreadyExistsException(pathString)
}

/**
 * Requires this to be a file and throws an [IllegalArgumentException] otherwise.
 */
public fun Path.requireFile(vararg options: LinkOption) {
    require(isRegularFile(*options)) { "$this is no file." }
}

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is empty, that is, has zero size
 * - this directory is empty, that is, has no entries
 */
public fun Path.requireNotEmpty(vararg options: LinkOption) {
    if (isEmpty(*options)) {
        if (isDirectory(*options)) throw noSuchFile(this, "Directory must not be empty but has no entries.")
        throw noSuchFile(this, "File must not be empty but has zero size.")
    }
}

/**
 * Checks if this path is inside of one of the System's temporary directories.
 *
 * @throws IllegalArgumentException this path is not inside [Locations.Temp]
 */
public fun Path.requireTempSubPath(): Path =
    apply {
        require(!isDefaultFileSystem() || isSubPathOf(Locations.Default.Temp)) {
            "${normalize().toAbsolutePath()} is not inside ${Locations.Default.Temp}."
        }
    }
