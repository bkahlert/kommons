package koodies.io.path

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import java.nio.file.Path
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
val Path.isEmpty: Boolean
    get() {
        requireExists()
        return when {
            isRegularFile() -> size.bytes == BigDecimal.ZERO
            isDirectory() -> listDirectoryEntries().none()
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
val Path.isNotEmpty: Boolean
    get() {
        requireExists()
        return when {
            isRegularFile() -> size.bytes > BigDecimal.ZERO
            isDirectory() -> listDirectoryEntries().any()
            else -> throw IllegalArgumentException("$this must either be a file or a directory.")
        }
    }
