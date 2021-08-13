package koodies

import koodies.io.path.Locations
import koodies.io.selfCleaning
import koodies.time.days
import koodies.time.hours
import koodies.time.minutes
import java.nio.file.Path

/**
 * Entrypoint for library-internal functionality.
 */
internal object Koodies : Locations {

    /**
     * Directory in which Koodies-specific data can be stored.
     */
    internal val InternalTemp: Path by Temp.resolve("koodies").selfCleaning(30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.resolve("exec").selfCleaning(1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.resolve("files").selfCleaning(10.minutes, 20)
}
