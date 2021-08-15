package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.time.days
import com.bkahlert.kommons.time.hours
import com.bkahlert.kommons.time.minutes
import java.nio.file.Path

/**
 * Entrypoint for library-internal functionality.
 */
internal object Kommons : Locations {

    /**
     * Directory in which library-specific data can be stored.
     */
    internal val InternalTemp: Path by Temp.resolve("kommons").selfCleaning(30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.resolve("exec").selfCleaning(1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.resolve("files").selfCleaning(10.minutes, 20)
}
