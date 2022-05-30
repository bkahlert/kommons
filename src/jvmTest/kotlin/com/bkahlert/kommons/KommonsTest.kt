package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.isSubPathOf
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.test.toStringContains
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.last

class KommonsTest {

    @Test
    fun `should resolve InternalTemp`() {
        expectThat(Kommons.InternalTemp) {
            toStringContains("kommons")
            isSubPathOf(Locations.Default.Temp)
        }
    }

    @Test
    fun `should resolve ExecTemp`() {
        expectThat(Kommons.ExecTemp) {
            last().pathString.isEqualTo("exec")
            isSubPathOf(Locations.Default.Temp)
        }
    }

    @Test
    fun `should resolve FilesTemp`() {
        expectThat(Kommons.FilesTemp) {
            last().pathString.isEqualTo("files")
            isSubPathOf(Locations.Default.Temp)
        }
    }

    @Test
    fun `should return name`() {
        expectThat(Kommons.name).isEqualTo("kommons")
    }

    @Test
    fun `should return group`() {
        expectThat(Kommons.group).isEqualTo("com.bkahlert.kommons")
    }

    @Test
    fun `should return version`() {
        expectThat(Kommons.version).isNotEqualTo(SemVer(0, 0, 1))
    }
}
