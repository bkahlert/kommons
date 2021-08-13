package koodies.io.compress

import koodies.io.compress.Archiver.addToArchive
import koodies.io.compress.Archiver.unarchiveTo
import koodies.io.path.addExtensions
import koodies.io.path.bufferedInputStream
import koodies.io.path.bufferedOutputStream
import koodies.io.path.deleteRecursively
import koodies.io.path.removeExtensions
import koodies.io.path.requireEmpty
import koodies.io.path.requireExists
import koodies.io.path.requireExistsNot
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Provides (un-)archiving functionality for the TAR archive format.
 */
public object TarArchiver {

    /**
     * Archives this directory using the TAR archive format.
     *
     * By default the existing directory name is used and `.tar` appended.
     */
    public fun Path.tar(
        destination: Path = addExtensions("tar"),
        overwrite: Boolean = false,
        predicate: (Path) -> Boolean = { true },
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively() else destination.requireExistsNot()
        TarArchiveOutputStream(destination.bufferedOutputStream())
            .apply { setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX) }
            .use { addToArchive(it, predicate) }
        return destination
    }

    /**
     * Unarchives this TAR archive.
     *
     * By default the existing file name is used with the `.tar` suffix removed.
     */
    public fun Path.untar(
        destination: Path = removeExtensions("tar"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.deleteRecursively()
        if (!destination.exists()) destination.createDirectories()
        if (!overwrite) destination.requireEmpty()
        TarArchiveInputStream(bufferedInputStream())
            .use { it.unarchiveTo(destination) }
        return destination
    }
}
