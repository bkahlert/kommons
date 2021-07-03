package koodies

import koodies.io.Locations
import koodies.io.SelfCleaningDirectory.CleanUpMode.OnStart
import koodies.io.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object KoodiesTest : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.Temp.selfCleaning("com.bkahlert.koodies-test", Duration.ZERO, 0, cleanUpMode = OnStart)
}
