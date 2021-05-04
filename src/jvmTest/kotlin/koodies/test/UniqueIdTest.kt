package koodies.test

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UniqueIdTest {

    @Test
    fun `should resolve unique id`(uniqueId: UniqueId) {
        expectThat(uniqueId.simplified).isEqualTo("UniqueIdTest.should_resolve_unique_id")
    }

    @Test
    fun `should resolve unique id`(uniqueId: UniqueId, @Suppress("UNUSED_PARAMETER") `with differing arguments`: TestInfo) {
        expectThat(uniqueId.simplified).isEqualTo("UniqueIdTest.should_resolve_unique_id-TestInfo")
    }

    @TestFactory
    fun `should resolve dynamic unique id`(uniqueId: UniqueId): DynamicContainer {
        return dynamicContainer("dynamic container", listOf(
            dynamicTest("dynamic test/container") {
                expectThat(uniqueId.simplified).isEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id")
            },
            dynamicTest("dynamic test") {
                expectThat(uniqueId.simplified).isEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id")
            }
        ))
    }
}
