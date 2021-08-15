package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.SelfCleaningDirectory.CleanUpMode.OnStart
import com.bkahlert.kommons.io.path.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKommons : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.Temp.resolve("kommons-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)
}
