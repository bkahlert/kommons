package koodies.test

import koodies.io.compress.Archiver.archive
import koodies.io.compress.Compressor.compress
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.randomDirectory
import koodies.io.path.renameTo
import koodies.io.path.withDirectoriesCreated
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeBytes

object Fixtures {

    fun Fixture.copyTo(file: Path): Path =
        file.withDirectoriesCreated().apply { writeBytes(data) }

    fun Fixture.copyToDirectory(directory: Path): Path =
        copyTo(directory.resolve(name))

    fun Path.singleFile(): Path = HtmlFile.copyToDirectory(this)
        .apply { check(exists()) { "Failed to provide archive with single file." } }

    fun Path.archiveWithSingleFile(format: String = CompressorStreamFactory.BZIP2): Path =
        singleFile().run {
            val archive = compress(format, overwrite = true)
            delete()
            archive
        }.apply { check(exists()) { "Failed to provide archive with single file." } }

    fun Path.directoryWithTwoFiles(): Path = randomDirectory().also {
        HtmlFile.copyToDirectory(it)
        TextFile.copyToDirectory(it.resolve("sub-dir")).renameTo("config.txt")
    }.apply { check(listDirectoryEntries().size == 2) { "Failed to provide directory with two files." } }

    fun Path.archiveWithTwoFiles(format: String = ArchiveStreamFactory.ZIP): Path =
        directoryWithTwoFiles().run {
            val archive = archive(format, overwrite = true)
            deleteRecursively()
            archive
        }.apply { check(exists()) { "Failed to provide directory with two files." } }
}
