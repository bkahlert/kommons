package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Archiver.addToArchive
import com.bkahlert.kommons.io.compress.Archiver.unarchiveTo
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.bufferedInputStream
import com.bkahlert.kommons.io.path.bufferedOutputStream
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.requireEmpty
import com.bkahlert.kommons.io.path.requireExists
import com.bkahlert.kommons.io.path.requireExistsNot
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
