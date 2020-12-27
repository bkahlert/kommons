package koodies.test

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class UniqueIdTest {

    @Test
    fun `should resolve unique id`() {
        expectThat(uniqueId).isEqualTo("UniqueIdTest.should_resolve_unique_id")
    }

    @Test
    fun `should resolve unique id`(`with differing arguments`: TestInfo) {
        expectThat(uniqueId).isEqualTo("UniqueIdTest.should_resolve_unique_id-TestInfo")
    }

    @Test
    fun `should resolve unique id`(extensionContext: ExtensionContext) {
        expectThat(extensionContext.simpleUniqueId).isEqualTo("UniqueIdTest.should_resolve_unique_id-ExtensionContext")
    }

    @TestFactory
    fun `should resolve dynamic unique id`(): DynamicContainer {
        return dynamicContainer("dynamic container", listOf(
            dynamicTest("dynamic test/container") {
                expectThat(uniqueId).isEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id.container-1.test-1")
            },
            dynamicTest("dynamic test") {
                expectThat(uniqueId).isEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id.container-1.test-2")
            }
        ))
    }
}
