package com.bkahlert.kommons.runtime

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan

class SystemResourcesKtTest {

    @Test
    fun `should resolve to valid URL`() {
        expectThat("junit-platform.properties".asSystemResourceUrl().readBytes().size).isGreaterThan(500)
    }
}
