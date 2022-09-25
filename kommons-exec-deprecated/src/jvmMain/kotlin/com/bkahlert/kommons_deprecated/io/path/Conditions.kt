package com.bkahlert.kommons_deprecated.io.path

import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

/**
 * Depending on the file type returns if
 * - this file is empty, that is, has zero length
 * - this directory is empty, that is, has no entries
 *
 * @throws IllegalArgumentException if this is neither a file nor a directory
 */
public fun Path.isEmpty(vararg options: LinkOption): Boolean {
    requireExists(*options)
    return when {
        isRegularFile(*options) -> fileSize() == 0L
        isDirectory(*options) -> listDirectoryEntries().none()
        else -> throw IllegalArgumentException("$this must either be a file or a directory.")
    }
}

/**
 * Depending on the file type returns if
 * - this file is not empty, that is, has positive length
 * - this directory is not empty, that is, has entries
 *
 * @throws IllegalArgumentException if this is neither a file nor a directory
 */
public fun Path.isNotEmpty(vararg options: LinkOption): Boolean {
    requireExists(*options)
    return when {
        isRegularFile(*options) -> fileSize() != 0L
        isDirectory(*options) -> listDirectoryEntries().any()
        else -> throw IllegalArgumentException("$this must either be a file or a directory.")
    }
}
