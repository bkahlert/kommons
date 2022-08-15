package com.bkahlert.kommons

import com.bkahlert.kommons.io.isSubPathOf
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import kotlin.io.path.pathString

class KommonsTest {

    @Test
    fun `should resolve InternalTemp`() {
        Kommons.InternalTemp should {
            it.toString() shouldContain "kommons"
            it.isSubPathOf(SystemLocations.Temp) shouldBe true
        }
    }

    @Test
    fun `should resolve ExecTemp`() {
        Kommons.ExecTemp should {
            it.last().pathString shouldBe "exec"
            it.isSubPathOf(SystemLocations.Temp) shouldBe true
        }
    }

    @Test
    fun `should resolve FilesTemp`() {
        Kommons.FilesTemp should {
            it.last().pathString shouldBe "files"
            it.isSubPathOf(SystemLocations.Temp) shouldBe true
        }
    }
}
