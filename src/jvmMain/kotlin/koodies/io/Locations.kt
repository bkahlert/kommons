package koodies.io

import koodies.exec.Process.State.Exited.Failed
import koodies.exec.parse
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.or
import koodies.shell.ShellScript
import koodies.text.Semantics.formattedAs
import koodies.time.days
import koodies.time.hours
import koodies.time.minutes
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.time.Duration

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
        ShellScript { !"ls $glob" }.exec.logging(BACKGROUND, this) {
            errorsOnly("${this@ls.formattedAs.input} $ ls ${glob.formattedAs.input}")
        }.parse.columns<Path, Failed>(1) {
            resolve(it[0])
        } or { emptyList() }

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

    /**
     * Creates a managed [TempDirectory].
     */
    public fun Temp(name: String, minAge: Duration = 1.hours, maximumFileCount: Int = 100): TempDirectory =
        TempDirectory(Temp.resolve(name), minAge, maximumFileCount)

    /**
     * Directory in which Koodies-specific data can be stored.
     */
    internal val InternalTemp: TempDirectory = Temp("com.bkahlert.koodies", 30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: TempDirectory = TempDirectory(InternalTemp.resolve("exec"), 1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: TempDirectory = TempDirectory(InternalTemp.resolve("files"), 10.minutes, 20)
}
