package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.delete
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.io.fileAlreadyExists
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

public val cloneFileSupport: Boolean by lazy {
    val file: Path = Kommons.FilesTemp.createTempFile().writeText("cloneFile test")
    val clone = file.resolveSibling("${file.fileName}-cloned")
    val exitCode = CommandLine("cp", "-c", file.pathString, clone.pathString).exec().waitFor().exitCode
    file.delete()
    clone.delete()
    exitCode == 0 && clone.exists() && clone.readText() == "cloneFile test"
}

/**
 * Copies a file or directory located by this path to the given target path.
 *
 * In contract to [copyTo] this method tries a [clone copy](https://www.unix.com/man-page/mojave/2/clonefile/) first.
 */
public fun Path.cloneTo(target: Path): Path =
    if (cloneFileSupport) {
        if (target.exists()) throw fileAlreadyExists(this, target)
        val exitCode = CommandLine("cp", "-c", pathString, target.pathString).exec().waitFor().exitCode
        check(exitCode == 0) { "Cloning failed with $exitCode" }
        target
    } else {
        copyTo(target)
    }
