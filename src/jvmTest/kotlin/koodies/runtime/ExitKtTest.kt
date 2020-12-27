package koodies.runtime

import koodies.io.path.Locations
import koodies.time.Now
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.exists
import java.nio.file.Path
import kotlin.io.path.writeText

@Execution(CONCURRENT)
class ExitKtTest {

    @Isolated
    @Execution(CONCURRENT)
    class DeleteOnExit {

        private val name = "koodies.onexit.does-not-work.txt"
        private val markerFile: Path = Locations.Temp.resolve(name)

        @BeforeAll
        fun setUp() {
            markerFile.deleteOnExit()
        }

        @Test
        fun `should clean up on shutdown`() {
            expectThat(markerFile).not { exists() }
        }

        @AfterAll
        fun tearDown() {
            markerFile.writeText("""
            This file was created $Now.
            It used to be cleaned up by the koodies library
            the moment the application in question shut down.
            
            The application was started by ${System.getProperty("sun.java.command")}.
        """.trimIndent())
        }
    }
}
