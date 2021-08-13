package koodies

import koodies.io.SelfCleaningDirectory.CleanUpMode.OnStart
import koodies.io.path.Locations
import koodies.io.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKoodies : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.Temp.resolve("koodies-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)
}
