package koodies.io

import koodies.Koodies
import koodies.io.file.pathString
import koodies.test.toStringContains
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.last

class InternalLocationsTest {

    @Test
    fun `should resolve InternalTemp`() {
        expectThat(Koodies.InternalTemp) {
            toStringContains("koodies")
            isInside(Locations.Temp)
        }
    }

    @Test
    fun `should resolve ExecTemp`() {
        expectThat(Koodies.ExecTemp) {
            last().pathString.isEqualTo("exec")
            isInside(Locations.Temp)
        }
    }

    @Test
    fun `should resolve FilesTemp`() {
        expectThat(Koodies.FilesTemp) {
            last().pathString.isEqualTo("files")
            isInside(Locations.Temp)
        }
    }
}
