package koodies.io.compress

import koodies.io.path.addExtensions
import koodies.io.path.bufferedInputStream
import koodies.io.path.deleteRecursively
import koodies.io.path.extensionOrNull
import koodies.io.path.removeExtensions
import koodies.io.path.requireExists
import koodies.io.path.requireExistsNot
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path
import kotlin.io.path.outputStream

/**
 * Provides (de-)compression functionality for a range of compression algorithms.
 *
 * @see CompressorStreamFactory
 */
public object Compressor {
    /**
     * Compresses this file using the provided compression algorithm.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.gz` or `.bzip2`) appended.
     */
    public fun Path.compress(
        format: String = CompressorStreamFactory.BZIP2,
        destination: Path = addExtensions(format),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        bufferedInputStream().use { inputStream ->
            CompressorStreamFactory().createCompressorOutputStream(format, destination.outputStream()).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destination
    }

    /**
     * Decompresses this compressed file.
     *
     * By default the existing file name is used with the extension removed.
     */
    public fun Path.decompress(
        destination: Path = removeExtensions(extensionOrNull!!),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        CompressorStreamFactory().createCompressorInputStream(bufferedInputStream()).use { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destination
    }
}
