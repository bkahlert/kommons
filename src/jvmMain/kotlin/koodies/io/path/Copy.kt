package koodies.io.path

import koodies.io.directoryNotEmpty
import koodies.io.file.CopyOptions
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.file.resolveSibling
import koodies.io.file.walkTopDown
import koodies.io.fileAlreadyExists
import koodies.io.fileSystemException
import koodies.io.noSuchFile
import koodies.text.withRandomSuffix
import org.apache.commons.io.IOUtils
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists


/**
 * Copies this path to the [target] recursively exactly as [File.copyRecursively] would do it.
 * (In short: A file `/src/file` or a directory `/src/dir` copied to `/dest/target` would then be located at `/dest/target`)
 *
 * Yet, there are two differences:
 * 1. [Java's non-blocking I/O](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) is used for this purpose which
 *    extends the use case too all implementations of [FileSystem]
 * 2. File attributes can be copied as well using the [preserve] flag *(default: off)*.
 *    (Since files are copied top-down, the [lastModified] attribute might be—although preserved—again be updated.
 *
 * Should `target` already exist and be a directory, an exception is thrown to avoid serious data loss.
 *
 * @see copyToDirectory
 */
fun Path.copyTo(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {

    if (notExists()) {
        onError(this, noSuchFile(this, target, "Source file doesn't exist.")) != OnErrorAction.TERMINATE
        return target
    }

    try {
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (!src.exists()) {
                if (onError(src, noSuchFile(src, "Source file doesn't exist.")) == OnErrorAction.TERMINATE) return target
            } else {
                val dstFile = target.resolveBetweenFileSystems(relativize(src))
                if (dstFile.exists()) {
                    if (!src.isDirectory() || !dstFile.isDirectory()) {
                        if (src.isRegularFile() && dstFile.isRegularFile() && src.size == dstFile.size) {
                            if (IOUtils.contentEquals(src.inputStream(), dstFile.inputStream())) continue
                        }
                        val stillExists = if (overwrite) dstFile.deleteRecursively().exists() else true

                        if (stillExists) {
                            if (onError(dstFile,
                                    fileAlreadyExists(src, dstFile, "The destination file already exists.")) == OnErrorAction.TERMINATE
                            ) return target
                            continue
                        }
                    }
                }

                if (!dstFile.parent.exists()) dstFile.parent.createDirectories()

                if (src.isDirectory()) {
                    if (!dstFile.exists()) {

                        val createdDir = Files.copy(src, dstFile, *CopyOptions.enumArrayOf(replaceExisting = overwrite, copyAttributes = preserve))
                        val isEmpty = createdDir.run { isDirectory() && isEmpty }
                        if (!isEmpty) {
                            if (onError(src, dstFile.directoryNotEmpty()) == OnErrorAction.TERMINATE) return target
                        }
                    }
                } else {
                    val copiedFile = Files.copy(src, dstFile, *CopyOptions.enumArrayOf(replaceExisting = overwrite, copyAttributes = preserve))
                    if (copiedFile.size != src.size) {
                        if (onError(src,
                                fileSystemException(src, dstFile, "Only ${copiedFile.size} out of ${src.size} were copied.")) == OnErrorAction.TERMINATE
                        )
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
 * an exception is thrown to avoid serious data loss.
 *
 * @see copyTo
 */
fun Path.copyToDirectory(
    targetDirectory: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    if (notExists()) {
        onError(this, NoSuchFileException(this.toString(), "$targetDirectory", "The source file doesn't exist.")) != OnErrorAction.TERMINATE
        return targetDirectory
    }

    if (!targetDirectory.exists()) targetDirectory.createDirectories()
    if (!targetDirectory.isDirectory()) {
        onError(this,
            FileAlreadyExistsException(this.toString(), "$targetDirectory", "The destination must not exist or be a directory.")) != OnErrorAction.TERMINATE
        return targetDirectory
    }

    return copyTo(targetDirectory.resolveBetweenFileSystems(fileName), overwrite = overwrite, preserve = preserve, onError = onError)
}

/**
 * Duplicates this file or directory by copying to the same path but with a random string to its name.
 *
 * In contrast to [copyTo] this method allows to specify the [order], that is, by how many ancestors should
 * the path segments differ from this path.
 *
 * - A order of `0` is identical to a making a copy with [copyTo].
 * - `1` (*default*) appends the suffix to parent's [Path.getFileName] instead.
 * - `2` to parent's parent
 * - ...  and so on
 *
 * E.g. `/a/b/c`'s 2 order duplication can be found at `/a/b-random/c`.
 */
fun Path.duplicate(order: Int = 1, suffix: String = "".withRandomSuffix()): Path {
    val sibling = resolveSibling(order) { resolveSibling(fileName.asString() + suffix) }
    return copyTo(sibling)
}


class TerminateException(path: Path) : FileSystemException(path.toString())
