package koodies.io.path

import koodies.concurrent.process.output
import koodies.concurrent.script
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * A couple of well known locations.
 */
public object Locations {

    /**
     * Resolves [glob] using the system's `ls` command line tool.
     */
    public fun ls(glob: String = ""): List<Path> =
        Temp.ls(glob)

    /**
     * Resolves [glob] using the system's `ls` command line tool.
     */
    public fun Path.ls(glob: String = ""): List<Path> =
        kotlin.runCatching {
            script { !"ls $glob" }.output { resolve(this) }
        }.getOrDefault(emptyList())

    /**
     * Working directory, that is, the directory in which this binary can be found.
     */
    public val WorkingDirectory: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

    /**
     * Home directory of the currently logged in user.
     */
    public val HomeDirectory: Path = Path.of(System.getProperty("user.home"))

    /**
     * Directory in which temporary data can be stored.
     */
    public val Temp: Path = Path.of(System.getProperty("java.io.tmpdir"))
}
