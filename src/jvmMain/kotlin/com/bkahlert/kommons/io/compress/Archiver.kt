package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Archiver.archive
import com.bkahlert.kommons.io.compress.Archiver.unarchive
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGzip
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.bufferedInputStream
import com.bkahlert.kommons.io.path.delete
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.extensionOrNull
import com.bkahlert.kommons.io.path.hasExtensions
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.requireEmpty
import com.bkahlert.kommons.io.path.requireExists
import com.bkahlert.kommons.io.path.requireExistsNot
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.listArchive as tarGzListArchive

/**
 * Provides (un-)archiving functionality for a range of archive formats.
 *
 * For the sake of convenience [archive] and [unarchive] also handle `tar.gz` files.
 *
 * @see ArchiveStreamFactory
 */
public object Archiver {

    /**
     * Archives this directory using the provided archive format.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.tar` or `.zip`) appended.
     */
    public fun Path.archive(
        format: String = ArchiveStreamFactory.ZIP,
        destination: Path = addExtensions(format),
        overwrite: Boolean = false,
        predicate: (Path) -> Boolean = { true },
    ): Path =
        if (format == "tar.gz") {
            tarGzip(destination, overwrite = overwrite)
        } else {
            requireExists()
            if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
            ArchiveStreamFactory()
                .createArchiveOutputStream(format, destination.outputStream())
                .apply { if (this is TarArchiveOutputStream) setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX) }
                .use { addToArchive(it, predicate) }
            destination
        }

    /**
     * Archives this directory by adding each entry to the [archiveOutputStream].
     */
    public fun Path.addToArchive(archiveOutputStream: ArchiveOutputStream, predicate: (Path) -> Boolean = { true }) {
        listDirectoryEntriesRecursively().filter(predicate).forEach { path ->
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
    public fun Path.unarchive(
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
    public fun ArchiveInputStream.unarchiveTo(
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
    public fun ArchiveInputStream.list(): List<ArchiveEntry> {
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
    public fun Path.listArchive(): List<ArchiveEntry> =
        if ("$fileName".endsWith("tar.gz")) {
            tarGzListArchive()
        } else {
            requireExists()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.list() }
        }
}
