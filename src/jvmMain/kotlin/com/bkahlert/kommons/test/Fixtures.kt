package com.bkahlert.kommons.test

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.delete
import com.bkahlert.kommons.deleteRecursively
import com.bkahlert.kommons.io.compress.Archiver.archive
import com.bkahlert.kommons.io.compress.Compressor.compress
import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.io.path.renameTo
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

public object Fixtures {

    public fun Path.symbolicLink(): Path = createTempFile()
        .also { it.delete() }
        .also { link -> Files.createSymbolicLink(link, createTempFile().also { it.delete() }) }
        .apply { check(exists(NOFOLLOW_LINKS)) { "Failed to create symbolic link $this." } }

    public fun Path.singleFile(): Path = HtmlFixture.copyToDirectory(this)
        .apply { check(exists()) { "Failed to provide archive with single file." } }

    public fun Path.archiveWithSingleFile(format: String = CompressorStreamFactory.BZIP2): Path =
        singleFile().run {
            val archive = compress(format, overwrite = true)
            delete()
            archive
        }.apply { check(exists()) { "Failed to provide archive with single file." } }

    public fun Path.directoryWithTwoFiles(): Path = createTempDirectory().also {
        HtmlFixture.copyToDirectory(it)
        TextFixture.copyToDirectory(it.resolve("sub-dir")).renameTo("config.txt")
    }.apply { check(listDirectoryEntries().size == 2) { "Failed to provide directory with two files." } }

    public fun Path.archiveWithTwoFiles(format: String = ArchiveStreamFactory.ZIP): Path =
        directoryWithTwoFiles().run {
            val archive = archive(format, overwrite = true)
            deleteRecursively()
            archive
        }.apply { check(exists()) { "Failed to provide directory with two files." } }
}
