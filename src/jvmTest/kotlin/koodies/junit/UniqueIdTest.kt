package koodies.junit

import koodies.runtime.CallStackElement
import koodies.test.TesterTest.PlainAssertionsTest
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import org.junit.platform.engine.UniqueId.parse
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UniqueIdTest {

    @Test
    fun `should resolve unique id`(uniqueId: UniqueId) {
        expectThat(uniqueId).toStringIsEqualTo("UniqueIdTest.should_resolve_unique_id")
    }

    @Test
    fun `should resolve unique id`(uniqueId: UniqueId, @Suppress("UNUSED_PARAMETER") `with differing arguments`: TestInfo) {
        expectThat(uniqueId).toStringIsEqualTo("UniqueIdTest.should_resolve_unique_id-TestInfo")
    }

    @TestFactory
    fun `should resolve dynamic unique id`(uniqueId: UniqueId): DynamicContainer =
        dynamicContainer("dynamic container", listOf(
            dynamicTest("dynamic test/container") {
                expectThat(uniqueId).toStringIsEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id")
            },
            dynamicTest("dynamic test") {
                expectThat(uniqueId).toStringIsEqualTo("UniqueIdTest.should_resolve_dynamic_unique_id")
            }
        ))

    @TestFactory
    fun `should instantiate unique id`() = testEach(
        UniqueId.from(parse("[engine:junit-jupiter]/[class:koodies.test.TesterTest]/[nested-class:PlainAssertionsTest]/[method:method name()]")),
        UniqueId.from(CallStackElement.from(PlainAssertionsTest::class.java.name, "method name", "Tests.kt", 1135)),
    ) {
        expecting { value } that { isEqualTo("TesterTest.PlainAssertionsTest.method_name") }
    }
}
