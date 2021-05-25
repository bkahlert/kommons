package koodies.io.path

import koodies.io.InternalLocations
import koodies.io.fileAlreadyExists
import koodies.io.tempFile
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

public val cloneFileSupport: Boolean by lazy {
    val file: Path = InternalLocations.FilesTemp.tempFile().apply {
        writeText("cloneFile test")
        deleteOnExit()
    }
    val clone = file.resolveSibling("cloned").deleteOnExit()
    Runtime.getRuntime()?.exec(arrayOf("cp", "-c", file.pathString, clone.pathString))
        ?.waitFor()
        ?.let { exitValue ->
            exitValue == 0 && clone.exists() && clone.readText() == "cloneFile test"
        } ?: false
}

/**
 * Copies a file or directory located by this path to the given target path.
 *
 * In contract to [copyTo] this method tries a [clone copy](https://www.unix.com/man-page/mojave/2/clonefile/) first.
 */
public fun Path.cloneTo(target: Path): Path {
    return if (cloneFileSupport) {
        if (target.exists()) throw fileAlreadyExists(this, target)
        Runtime.getRuntime()?.exec(arrayOf("cp", "-c", pathString, target.pathString))?.waitFor()?.let { exitValue ->
            check(exitValue == 0) { "Cloning failed with $exitValue" }
            target
        } ?: throw IllegalStateException("Error executing file cloning")
    } else {
        copyTo(target)
    }
}
