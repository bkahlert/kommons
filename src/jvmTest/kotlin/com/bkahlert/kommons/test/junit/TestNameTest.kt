package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class TestNameTest {

    @Test
    fun `should resolve test name`(testName: TestName) {
        expectThat(testName).toStringIsEqualTo("TestNameTest ➜ should resolve test name")
    }
}
