package koodies.io.path

import koodies.io.fileAlreadyExists
import koodies.jvm.deleteOnExit
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

public val cloneFileSupport: Boolean by lazy {
    val file: Path = tempFile().apply {
        writeText("cloneFile test")
        deleteOnExit(this)
    }
    val clone = deleteOnExit(file.resolveSibling("cloned"))
    Runtime.getRuntime()?.exec(arrayOf("cp", "-c", file.asString(), clone.asString()))
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
        Runtime.getRuntime()?.exec(arrayOf("cp", "-c", asString(), target.asString()))?.waitFor()?.let { exitValue ->
            check(exitValue == 0) { "Cloning failed with $exitValue" }
            target
        } ?: throw IllegalStateException("Error executing file cloning")
    } else {
        copyTo(target)
    }
}

