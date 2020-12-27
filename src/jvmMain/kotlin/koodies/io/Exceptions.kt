package koodies.io

import koodies.io.path.asString
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path


/**
 * Constructs an instance of [NotDirectoryException]
 * with this path.
 */
fun Path.noDirectory(): NotDirectoryException =
    NotDirectoryException(asString())

/**
 * Constructs an instance of [DirectoryNotEmptyException]
 * with this path.
 */
@JvmName("receiverDirectoryNotEmpty")
fun Path.directoryNotEmpty(): DirectoryNotEmptyException =
    DirectoryNotEmptyException(this.asString())

/**
 * Constructs an instance of [DirectoryNotEmptyException]
 * with [path] as the not empty directory.
 */
fun directoryNotEmpty(path: Path): DirectoryNotEmptyException =
    DirectoryNotEmptyException(path.asString())

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with an unknown path and an optional [reason].
 */
fun fileAlreadyExists(reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(null, null, reason)

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with [path] as the already existing file and an optional [reason].
 */
fun fileAlreadyExists(path: Path, reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(path.asString(), null, reason)

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with [source] and [target]  path and an optional [reason].
 */
fun fileAlreadyExists(source: Path, target: Path, reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(source.asString(), target.asString(), reason)

/**
 * Constructs an instance of [FileSystemException]
 * with this path.
 */
fun Path.fileSystemException(): FileSystemException =
    FileSystemException(this.asString())

/**
 * Constructs an instance of [FileSystemException]
 * with an unknown path and an optional [reason].
 */
fun fileSystemException(reason: String? = null): FileSystemException =
    FileSystemException(null, null, reason)

/**
 * Constructs an instance of [FileSystemException]
 * with [path] as the affected path and an optional [reason].
 */
fun fileSystemException(path: Path, reason: String? = null): FileSystemException =
    FileSystemException(path.asString(), null, reason)

/**
 * Constructs an instance of [FileSystemException]
 * with [source] and [target] as the affected paths and an optional [reason].
 */
fun fileSystemException(source: Path, target: Path, reason: String? = null): FileSystemException =
    FileSystemException(source.asString(), target.asString(), reason)

/**
 * Constructs an instance of [NoSuchFileException]
 * with this path.
 */
fun Path.noSuchFile(): NoSuchFileException =
    NoSuchFileException(this.asString())

/**
 * Constructs an instance of [NoSuchFileException]
 * with an unknown path and an optional [reason].
 */
fun noSuchFile(reason: String? = null): NoSuchFileException =
    NoSuchFileException(null, null, reason)

/**
 * Constructs an instance of [NoSuchFileException]
 * with [path] as the missing path and an optional [reason].
 */
fun noSuchFile(path: Path, reason: String? = null): NoSuchFileException =
    NoSuchFileException(path.asString(), null, reason)

/**
 * Constructs an instance of [NoSuchFileException]
 * with [source] and [target] as the missing paths and an optional [reason].
 */
fun noSuchFile(source: Path, target: Path, reason: String? = null): NoSuchFileException =
    NoSuchFileException(source.asString(), target.asString(), reason)
