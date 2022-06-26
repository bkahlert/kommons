package com.bkahlert.kommons.exception

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.capitalize
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.io.path.withExtension
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.isSubPathOf
import com.bkahlert.kommons.withSuffix
import java.io.IOException
import java.nio.file.Path

private object Dump {
    val dumpDir: Path = Kommons.ExecTemp
    const val dumpPrefix = "dump--"
    const val dumpSuffix = ".log"
}

/**
 * Dumps whatever is returned by [data] to the specified [file] in this directory and
 * returns a description of the dump.
 *
 * If an error occurs in this process—so to as a last resort—the returned description
 * includes the complete dump itself.
 */
public fun Path.dump(
    errorMessage: String?,
    file: Path = (takeUnless { it.isSubPathOf(SystemLocations.Temp) } ?: Dump.dumpDir).createTempFile(Dump.dumpPrefix, Dump.dumpSuffix),
    data: () -> String,
): String = runCatching {
    var dumped: String? = null
    val dumps = persistDump(file) { data().also { dumped = it } }

    val dumpedLines = (dumped ?: error("Dump seems empty")).lines()
    val recentLineCount = dumpedLines.size.coerceAtMost(10)

    (errorMessage?.withSuffix(LF)?.capitalize() ?: "") +
        "➜ A dump has been written to:$LF" +
        dumps.entries.joinToString("") { "  - ${it.value.toUri()} (${it.key})$LF" } +
        "➜ The last $recentLineCount lines are:$LF" +
        dumpedLines.takeLast(recentLineCount).joinToString("") { "  $it$LF" }
}.recover { ex: Throwable ->
    (errorMessage?.withSuffix(LF)?.capitalize() ?: "") +
        "In the attempt to persist the corresponding dump the following error occurred:$LF" +
        "${ex.toCompactString()}$LF" +
        LF +
        "➜ The not successfully persisted dump is as follows:$LF" +
        data()
}.getOrThrow()

/**
 * Dumps whatever is returned by [data] to the specified [path] and
 * returns a description of the dump.
 *
 * If an error occurs in this process—so to as as a last resort—the returned description
 * includes the complete dump itself.
 */
public fun Path.dump(
    errorMessage: String?,
    path: Path = createTempFile(Dump.dumpPrefix, Dump.dumpSuffix),
    data: String,
): String =
    dump(errorMessage, path) { data }

/**
 * Dumps whatever is returned by [data] to the specified [path].
 *
 * This method returns a map of file format to [Path] mappings.
 *
 * ***Note:** To also create an default error message, use [Path.dump].
 */
public fun persistDump(
    path: Path,
    data: () -> String,
): Map<String, Path> = runCatching {
    data().run {
        mapOf(
            "unchanged" to path.withExtension("log").writeText(this),
            "ANSI escape/control sequences removed" to path.withExtension("ansi-removed.log").writeText(this.ansiRemoved)
        )
    }
}.getOrElse {
    if (it is IOException) throw it
    throw IOException(it)
}
