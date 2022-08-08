package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.deleteRecursively
import com.bkahlert.kommons.io.EOF
import com.bkahlert.kommons.io.directoryNotEmpty
import com.bkahlert.kommons.io.fileAlreadyExists
import com.bkahlert.kommons.io.fileSystemException
import com.bkahlert.kommons.io.noSuchFile
import com.bkahlert.kommons.resolveBetweenFileSystems
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.CopyOption
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

/**
 * Copies this path to the [target] recursively exactly as [File.copyRecursively] would do it.
 * (In short: A file `/src/file` or a directory `/src/dir` copied to `/dest/target` would then be located at `/dest/target`)
 *
 * Yet, there are two differences:
 * 1. [Java's non-blocking I/O](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) is used for this purpose which
 *    extends the use case to all implementations of [FileSystem]
 * 2. File attributes can be copied as well using the [preserve] flag (default: off).
 *    (Since files are copied top-down, the [lastModified] attribute might be—although preserved—again be updated.
 *
 * Should `target` already exist and be a directory, an exception is thrown to avoid serious data loss.
 *
 * @see copyToDirectory
 */
public fun Path.copyTo(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    vararg options: LinkOption,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {

    if (notExists(*options)) {
        onError(this, noSuchFile(this, target, "Source file doesn't exist.")) != OnErrorAction.TERMINATE
        return target
    }

    try {
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (!src.exists(*options)) {
                if (onError(src, noSuchFile(src, "Source file doesn't exist.")) == OnErrorAction.TERMINATE) return target
            } else {
                val dstFile = target.resolveBetweenFileSystems(relativize(src))
                if (dstFile.exists(*options) && (!src.isDirectory(*options) || !dstFile.isDirectory(*options))) {

                    if (src.isRegularFile(*options) &&
                        dstFile.isRegularFile(*options) &&
                        src.fileSize() == dstFile.fileSize() &&
                        src.inputStream(*options).contentEquals(dstFile.inputStream(*options))
                    ) continue

                    val stillExists: Boolean = if (overwrite) {
                        dstFile.deleteRecursively(*options).exists(*options)
                    } else {
                        true
                    }

                    if (stillExists) {
                        if (onError(dstFile, fileAlreadyExists(src, dstFile, "The destination file already exists.")) == OnErrorAction.TERMINATE) {
                            return target
                        }
                        continue
                    }
                }

                if (!dstFile.parent.exists(*options)) dstFile.parent.createDirectories()

                if (src.isDirectory(*options)) {
                    if (!dstFile.exists(*options)) {

                        val createdDir =
                            Files.copy(
                                src, dstFile, *copyOptions(
                                    replaceExisting = overwrite,
                                    copyAttributes = preserve,
                                    copyOptions = options
                                )
                            )
                        val isEmpty = createdDir.run { isDirectory(*options) && isEmpty(*options) }
                        if (!isEmpty && onError(src, dstFile.directoryNotEmpty()) == OnErrorAction.TERMINATE) {
                            return target
                        }
                    }
                } else {
                    val copiedFile = Files.copy(
                        src, dstFile, *copyOptions(
                            replaceExisting = overwrite,
                            copyAttributes = preserve,
                            copyOptions = options,
                        )
                    )
                    val copiedFileSize = copiedFile.fileSize()
                    val srcFileSize = src.fileSize()
                    if (copiedFileSize != srcFileSize &&
                        onError(src, fileSystemException(src, dstFile, "Only $copiedFileSize out of $srcFileSize were copied.")) == OnErrorAction.TERMINATE
                    ) {
                        return target
                    }
                }
            }
        }
        return target
    } catch (e: TerminateException) {
        return target
    }
}

private fun copyOptions(
    replaceExisting: Boolean = false,
    copyAttributes: Boolean = false,
    atomicMove: Boolean = false,
    vararg copyOptions: CopyOption,
): Array<CopyOption> {
    return listOf(
        replaceExisting to StandardCopyOption.REPLACE_EXISTING,
        copyAttributes to StandardCopyOption.COPY_ATTRIBUTES,
        atomicMove to StandardCopyOption.ATOMIC_MOVE,
    ).mapNotNull { (active, option) -> if (active) option else null }
        .run { plus(copyOptions) }
        .toTypedArray()
}

private fun InputStream.contentEquals(otherStream: InputStream): Boolean {
    if (this === otherStream) return true
    val thisBufferStream: BufferedInputStream = buffered()
    val otherBufferedStream: BufferedInputStream = otherStream.buffered()
    var ch = thisBufferStream.read()
    while (EOF != ch) {
        val ch2 = otherBufferedStream.read()
        if (ch != ch2) return false
        ch = thisBufferStream.read()
    }
    return otherBufferedStream.read() == EOF
}


/**
 * Copies this path to the [targetDirectory] recursively exactly as [File.copyRecursively] would do it,
 * if the call happened on the target's parent (directory).
 * (In short: A file `/src/file` or a directory `/src/dir` copied to `/dest/target`
 *  would then be located at `/dest/target/file` respectively `/dest/target/dir`)
 *
 * Yet, there are two differences:
 * 1. [Java's non-blocking I/O](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) is used for this purpose which
 *    extends the use case too all implementations of [FileSystem]
 * 2. File attributes can be copied as well using the [preserve] flag *(default: off)*.
 *    (Since files are copied top-down, the [lastModified] attribute might be—although preserved—again be updated.
 *
 * Should `targetDirectory` already exist and contain a directory with the same name as this path,
 * an exception is thrown to avoid data loss.
 *
 * @see copyTo
 */
public fun Path.copyToDirectory(
    targetDirectory: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    vararg options: LinkOption,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    if (notExists(*options)) {
        onError(this, NoSuchFileException(this.toString(), "$targetDirectory", "The source file doesn't exist.")) != OnErrorAction.TERMINATE
        return targetDirectory
    }

    if (!targetDirectory.exists(*options)) targetDirectory.createDirectories()
    if (!targetDirectory.isDirectory(*options)) {
        onError(
            this,
            FileAlreadyExistsException(this.toString(), "$targetDirectory", "The destination must not exist or be a directory.")
        ) != OnErrorAction.TERMINATE
        return targetDirectory
    }

    return copyTo(targetDirectory.resolveBetweenFileSystems(fileName), overwrite = overwrite, preserve = preserve, onError = onError, options = options)
}

/**
 * Copies this path to a temporary directory—the name based on the
 * optional [base] and [extension].
 */
public fun Path.copyToTemp(
    base: String = nameWithoutExtension,
    extension: String = extensionOrNull?.let { ".$it" } ?: "",
): Path = copyTo(Kommons.FilesTemp.createTempFile(base, extension), overwrite = true)


public class TerminateException(path: Path) : FileSystemException(path.toString())
