package koodies.io.compress

import koodies.io.compress.Archiver.addToArchive
import koodies.io.compress.Archiver.list
import koodies.io.compress.Archiver.unarchiveTo
import koodies.io.path.addExtensions
import koodies.io.path.bufferedInputStream
import koodies.io.path.deleteRecursively
import koodies.io.path.removeExtensions
import koodies.io.path.requireEmpty
import koodies.io.path.requireExists
import koodies.io.path.requireExistsNot
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

/**
 * Provides (de-)compression and (un-)archiving functionality for the TAR archive format compressed using the GNU GZIP format.
 */
public object TarArchiveGzCompressor {
    /**
     * Archives this directory using the TAR archive format and compresses the archive using GNU ZIP.
     *
     * By default the existing file name is used and `.tar.gz` appended.
     */
    public fun Path.tarGzip(
        destination: Path = addExtensions("tar.gz"),
        overwrite: Boolean = false,
        predicate: (Path) -> Boolean = { true },
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        GzipCompressorOutputStream(destination.outputStream()).use { gzipOutput ->
            TarArchiveOutputStream(gzipOutput)
                .apply { setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX) }
                .use { addToArchive(it, predicate) }
        }
        return destination
    }

    /**
     * Decompresses this GNU ZIP compressed file and unarchives the decompressed TAR archive.
     *
     * By default the existing file name is used with the `.tar.gz` suffix removed.
     */
    public fun Path.tarGunzip(
        destination: Path = removeExtensions("tar", "gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively()
        if (!destination.exists()) destination.createDirectories()
        if (!overwrite) destination.requireEmpty()
        GzipCompressorInputStream(bufferedInputStream()).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { it.unarchiveTo(destination) }
        }
        return destination
    }

    /**
     * Lists this archive without unarchiving it.
     */
    public fun Path.listArchive(): List<ArchiveEntry> {
        requireExists()
        var archiveEntries: List<ArchiveEntry>
        GzipCompressorInputStream(bufferedInputStream()).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { archiveEntries = it.list() }
        }
        return archiveEntries
    }
}
