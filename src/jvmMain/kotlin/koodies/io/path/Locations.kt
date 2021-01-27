package koodies.io.path

import koodies.concurrent.output
import koodies.concurrent.script
import java.nio.file.FileSystems
import java.nio.file.Path

object Locations {

    /**
     * Resolves [glob] using the system's `ls` command line tool.
     */
    fun ls(glob: String): List<Path> =
        Temp.ls(glob)

    /**
     * Resolves [glob] using the system's `ls` command line tool.
     */
    fun Path.ls(glob: String): List<Path> =
        kotlin.runCatching { script { !"ls $glob" }.output().lines().map { resolve(it) } }.getOrDefault(emptyList())

    /**
     * Working directory, that is, the directory in which this binary can be found.
     */
    val WorkingDirectory: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

    /**
     * Home directory of the currently logged in user.
     */
    val HomeDirectory: Path = Path.of(System.getProperty("user.home"))

    /**
     * Directory in which temporary data can be stored.
     */
    val Temp: Path = Path.of(System.getProperty("java.io.tmpdir"))
}
