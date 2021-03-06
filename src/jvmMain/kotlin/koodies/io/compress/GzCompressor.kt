package koodies.io.compress

import koodies.io.path.addExtensions
import koodies.io.path.bufferedInputStream
import koodies.io.path.deleteRecursively
import koodies.io.path.removeExtensions
import koodies.io.path.requireExists
import koodies.io.path.requireExistsNot
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.outputStream

/**
 * Provides (de-)compression functionality for the GNU GZIP format.
 */
public object GzCompressor {
    /**
     * Compresses this file using GNU ZIP.
     *
     * By default the existing file name is used and `.gz` appended.
     */
    public fun Path.gzip(
        destination: Path = addExtensions("gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        bufferedInputStream().use { fileInput ->
            GZIPOutputStream(destination.outputStream()).use { gzipOutput ->
                fileInput.copyTo(gzipOutput)
            }
        }
        return destination
    }

    /**
     * Decompresses this GNU ZIP compressed file.
     *
     * By default the existing file name is used with the `.gz` suffix removed.
     */
    public fun Path.gunzip(
        destination: Path = removeExtensions("gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        GZIPInputStream(bufferedInputStream()).use { gzipInput ->
            destination.outputStream().use { fileOutput ->
                gzipInput.copyTo(fileOutput)
            }
        }
        return destination
    }
}
