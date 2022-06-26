package com.bkahlert.kommons

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    @Test
    fun `should return name`() {
        Kommons.name shouldBe "kommons"
    }

    @Test
    fun `should return group`() {
        Kommons.group shouldBe "com.bkahlert.kommons"
    }

    @Test
    fun `should return version`() {
        Kommons.version shouldNotBe SemanticVersion(0, 0, 1)
    }
}
