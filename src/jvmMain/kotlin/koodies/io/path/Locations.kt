package koodies.io.path

import java.nio.file.FileSystems
import java.nio.file.Path

object Locations {
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
