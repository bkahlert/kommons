package koodies.io.path

import koodies.unit.DecimalPrefix
import koodies.unit.Size
import koodies.unit.bytes
import koodies.unit.toSize
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink

public object FileSizeComparator : (Path, Path) -> Int {
    override fun invoke(path1: Path, path2: Path): Int = path1.size.compareTo(path2.size)
}

/**
 * Contains the decimal size of this file (`e.g. 3.12 MB`).
 *
 * If this target actually points to a directory, this property
 * contains the overall size of all contained files.
 */
public val Path.size: Size
    get() {
        requireExists()
        return if (!isDirectory()) Files.size(toAbsolutePath()).bytes
        else (toFile().listFiles() ?: return Size.ZERO) // TODO remove toFile
            .asSequence()
            .map(File::toPath)
            .filterNot { it.isSymbolicLink() }
            .fold(Size.ZERO) { size, path -> size + path.size }
    }

/**
 * Contains the size of this file or directory rounded so that
 * —if printed— no decimals are needed (e.g. `3 MB`).
 *
 * Please note that the size is rounded in the decimal system (1 KB = 1.000 B).
 */
public val Path.roundedSize: Size get() = size.toString<DecimalPrefix>(decimals = 0).toSize()
