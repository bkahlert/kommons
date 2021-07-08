package koodies.io.path

import koodies.math.isZero
import koodies.unit.DecimalPrefixes
import koodies.unit.Size
import koodies.unit.bytes
import koodies.unit.sumBy
import koodies.unit.toSize
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

public object FileSizeComparator : (Path, Path) -> Int {
    override fun invoke(path1: Path, path2: Path): Int = path1.getSize().compareTo(path2.getSize())
}

private val Path.size
    get() = takeIf { it.isRegularFile() }
        ?.let { file -> Files.size(file).takeUnless { fileSize -> fileSize.isZero } }
        ?.bytes ?: Size.ZERO

/**
 * Contains the decimal size of this file (`e.g. 3.12 MB`).
 *
 * If this target actually points to a directory, this property
 * contains the overall size of all contained files.
 */
public fun Path.getSize(vararg options: LinkOption): Size {
    requireExists(*options)
    return if (!isDirectory(*options)) {
        toAbsolutePath().size
    } else {
        useDirectoryEntriesRecursively(options = options) { seq ->
            seq.sumBy { path ->
                if (options.contains(LinkOption.NOFOLLOW_LINKS)) check(!path.isSymbolicLink())
                path.size
            }
        }
    }
}

/**
 * Contains the size of this file or directory rounded so that
 * —if printed— no decimals are needed (e.g. `3 MB`).
 *
 * Please note that the size is rounded in the decimal system (1 KB = 1.000 B).
 */
public fun Path.getRoundedSize(vararg options: LinkOption): Size = getSize(*options).toString(DecimalPrefixes, decimals = 0).toSize()
