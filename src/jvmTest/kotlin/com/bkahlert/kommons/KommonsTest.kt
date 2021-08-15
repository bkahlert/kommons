package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.isInside
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.test.toStringContains
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.last

class KommonsTest {

    @Test
    fun `should resolve InternalTemp`() {
        expectThat(Kommons.InternalTemp) {
            toStringContains("kommons")
            isInside(Locations.Temp)
        }
    }

    @Test
    fun `should resolve ExecTemp`() {
        expectThat(Kommons.ExecTemp) {
            last().pathString.isEqualTo("exec")
            isInside(Locations.Temp)
        }
    }

    @Test
    fun `should resolve FilesTemp`() {
        expectThat(Kommons.FilesTemp) {
            last().pathString.isEqualTo("files")
            isInside(Locations.Temp)
        }
    }
}
