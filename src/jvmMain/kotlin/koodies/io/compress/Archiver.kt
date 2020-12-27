package koodies.io.compress

import koodies.io.compress.Archiver.archive
import koodies.io.compress.Archiver.unarchive
import koodies.io.compress.TarArchiveGzCompressor.tarGunzip
import koodies.io.compress.TarArchiveGzCompressor.tarGzip
import koodies.io.file.bufferedInputStream
import koodies.io.file.outputStream
import koodies.io.path.addExtensions
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.extensionOrNull
import koodies.io.path.hasExtensions
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.removeExtensions
import koodies.io.path.requireEmpty
import koodies.io.path.requireExists
import koodies.io.path.requireExistsNot
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import koodies.io.compress.TarArchiveGzCompressor.listArchive as tarGzListArchive

/**
 * Provides (un-)archiving functionality for a range of archive formats.
 *
 * For the sake of convenience [archive] and [unarchive] also handle `tar.gz` files.
 *
 * @see ArchiveStreamFactory
 */
object Archiver {
    /**
     * Archives this directory using the provided archive format.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.tar` or `.zip`) appended.
     */
    fun Path.archive(
        format: String = ArchiveStreamFactory.ZIP,
        destination: Path = addExtensions(format),
        overwrite: Boolean = false,
    ): Path =
        if (format == "tar.gz") {
            tarGzip(destination, overwrite = overwrite)
        } else {
            requireExists()
            if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
            ArchiveStreamFactory().createArchiveOutputStream(format, destination.outputStream()).use { addToArchive(it) }
            destination
        }

    /**
     * Archives this directory by adding each entry to the [archiveOutputStream].
     */
    fun Path.addToArchive(archiveOutputStream: ArchiveOutputStream) {
        listDirectoryEntriesRecursively().forEach { path ->
            val entryName = "${relativize(path)}"
            val entry: ArchiveEntry = archiveOutputStream.createArchiveEntry(path.toFile(), entryName)
            archiveOutputStream.putArchiveEntry(entry)
            if (path.isRegularFile()) path.bufferedInputStream().copyTo(archiveOutputStream)
            archiveOutputStream.closeArchiveEntry()
        }
    }

    /**
     * Unarchives this archive.
     *
     * By default the existing file name is used with the extension removed.
     */
    fun Path.unarchive(
        destination: Path = if (hasExtensions("tar", "gz")) removeExtensions("tar", "gz") else extensionOrNull?.let { removeExtensions(it) }
            ?: throw IllegalArgumentException("Cannot auto-detect the archive format due to missing file extension."),
        overwrite: Boolean = false,
    ): Path =
        if (hasExtensions("tar", "gz")) {
            tarGunzip(destination, overwrite = overwrite)
        } else {
            requireExists()
            if (overwrite) destination.deleteRecursively()
            if (!destination.exists()) destination.createDirectories()
            if (!overwrite) destination.requireEmpty()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.unarchiveTo(destination) }
            destination
        }

    /**
     * Unarchives this archive input stream to [destination].
     */
    fun ArchiveInputStream.unarchiveTo(
        destination: Path,
    ) {
        var archiveEntry: ArchiveEntry?
        while (nextEntry.also { archiveEntry = it } != null) {
            if (!canReadEntryData(archiveEntry)) {
                println("$archiveEntry makes use on unsupported features. Skipping.")
                continue
            }
            val path: Path = destination.resolve(archiveEntry!!.name)
            if (archiveEntry!!.isDirectory) {
                require(path.createDirectories().exists()) { "$path could not be created." }
            } else {
                require(path.parent.createDirectories().exists()) { "${path.parent} could not be created." }
                path.delete().outputStream().also { copyTo(it) }.also { it.close() }
            }
        }
    }

    /**
     * Lists this archive input stream.
     */
    fun ArchiveInputStream.list(): List<ArchiveEntry> {
        val archiveEntries = mutableListOf<ArchiveEntry>()
        var archiveEntry: ArchiveEntry?
        while (nextEntry.also { archiveEntry = it } != null) {
            archiveEntries.add(archiveEntry!!)
        }
        return archiveEntries
    }

    /**
     * Lists this archive without unarchiving it.
     */
    fun Path.listArchive(): List<ArchiveEntry> =
        if ("$fileName".endsWith("tar.gz")) {
            tarGzListArchive()
        } else {
            requireExists()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.list() }
        }
}
