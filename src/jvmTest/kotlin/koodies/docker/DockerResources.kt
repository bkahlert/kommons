package koodies.docker

import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources

/**
 * Common resource names for synchronizing [Docker] test execution.
 *
 * @see ResourceLock
 * @see Resources
 */
object DockerResources {
    /**
     * Represents the device `/dev/serial1`.
     */
    const val SERIAL = "/dev/serial1"
}
