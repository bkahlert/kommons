package koodies.test.junit

import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class TestNameTest {

    @Test
    fun `should resolve test name`(testName: TestName) {
        expectThat(testName).toStringIsEqualTo("TestNameTest ➜ should resolve test name")
    }
}
