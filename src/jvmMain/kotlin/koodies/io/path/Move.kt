package koodies.io.path

import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.moveTo

fun Path.moveTo(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    requireExists()
    return copyTo(target, overwrite, preserve, onError)
        .also { deleteRecursively() }
}

fun Path.moveToDirectory(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    requireExists()
    require(target.isDirectory() || !target.exists())
    return copyToDirectory(target.createDirectories(), overwrite, preserve, onError)
        .also { deleteRecursively() }
}

fun Path.renameTo(target: Path): Path {
    requireExists()
    return moveTo(resolveSibling(target.fileName))
}

fun Path.renameTo(fileName: String): Path {
    requireExists()
    return moveTo(resolveSibling(fileName))
}
