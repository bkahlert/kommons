package com.bkahlert.kommons

import com.bkahlert.kommons.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull

class SemVerTest {

    @Test
    fun `should parse`() {
        expectThat(SemVer.parse("1.10.0-dev.11.uncommitted+refactor.kommons.2e94661")) {
            get { major }.isEqualTo(1)
            get { minor }.isEqualTo(10)
            get { patch }.isEqualTo(0)
            get { preRelease }.isEqualTo("dev.11.uncommitted")
            get { build }.isEqualTo("refactor.kommons.2e94661")
        }
    }

    @Test
    fun `should parse without build`() {
        expectThat(SemVer.parse("1.10.0-dev.11.uncommitted")) {
            get { major }.isEqualTo(1)
            get { minor }.isEqualTo(10)
            get { patch }.isEqualTo(0)
            get { preRelease }.isEqualTo("dev.11.uncommitted")
            get { build }.isNull()
        }
    }

    @Test
    fun `should parse without pre-release`() {
        expectThat(SemVer.parse("1.10.0+refactor.kommons.2e94661")) {
            get { major }.isEqualTo(1)
            get { minor }.isEqualTo(10)
            get { patch }.isEqualTo(0)
            get { preRelease }.isNull()
            get { build }.isEqualTo("refactor.kommons.2e94661")
        }
    }

    @Test
    fun `should parse without pre-release and build`() {
        expectThat(SemVer.parse("1.10.0")) {
            get { major }.isEqualTo(1)
            get { minor }.isEqualTo(10)
            get { patch }.isEqualTo(0)
            get { preRelease }.isNull()
            get { build }.isNull()
        }
    }

    @Test
    fun `should be equal if all fields match`() {
        expectThat(SemVer(1, 10, 0, "dev", "2e94661")).isEqualTo(SemVer(1, 10, 0, "dev", "2e94661"))
    }

    @Test
    fun `should not be equal if not all fields match`() {
        expectThat(SemVer(1, 10, 0, "dev", "2e94661")).isNotEqualTo(SemVer(1, 20, 0, "dev"))
    }

    @Test
    fun `should implement toString`() {
        expectThat(SemVer(1, 10, 0, "dev", "2e94661")).toStringIsEqualTo("1.10.0-dev+2e94661")
    }
}
