package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.deleteRecursively
import java.nio.file.FileSystemException
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.moveTo

public fun Path.moveTo(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    vararg options: LinkOption,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    requireExists(*options)
    return copyTo(target, overwrite, preserve, *options, onError = onError)
        .also { deleteRecursively(*options) }
}

public fun Path.moveToDirectory(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    vararg options: LinkOption,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    requireExists(*options)
    require(target.isDirectory(*options) || !target.exists(*options))
    return copyToDirectory(target.createDirectories(), overwrite, preserve, *options, onError = onError)
        .also { deleteRecursively(*options) }
}

public fun Path.renameTo(target: Path, vararg options: LinkOption): Path {
    requireExists(*options)
    return moveTo(resolveSibling(target.fileName), options = options)
}

public fun Path.renameTo(fileName: String, vararg options: LinkOption): Path {
    requireExists(*options)
    return moveTo(resolveSibling(fileName), options = options)
}
