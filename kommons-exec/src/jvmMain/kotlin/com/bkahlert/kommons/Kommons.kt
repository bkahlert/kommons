package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Entrypoint for library-internal functionality.
 */
public object Kommons {

    /**
     * Directory in which library-specific data can be stored.
     */
    internal val InternalTemp: Path by SystemLocations.Temp.resolve("kommons").selfCleaning(30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.resolve("exec").selfCleaning(1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.resolve("files").selfCleaning(10.minutes, 20)
}
