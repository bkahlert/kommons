package koodies.io

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
    internal val InternalTemp: Path by Locations.Temp.selfCleaning("com.bkahlert.koodies", 30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.selfCleaning("exec", 1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.selfCleaning("files", 10.minutes, 20)
}
