package koodies.runtime.koodies.persistence

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Functions to access unknown and well known paths.
 */
object Paths {

    /**
     * Converts the path string [first], and the optionally [more] paths strings
     * joined as a [Path].
     *
     * @see Paths.get
     * @see Path.of
     */
    operator fun get(first: String, vararg more: String): Path =
        Paths.get(first, *more)

    /**
     * Converts the given URI to a [Path].
     *
     * @see Paths.get
     * @see Path.of
     */
    operator fun get(uri: URI): Path =
        Paths.get(uri)

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
