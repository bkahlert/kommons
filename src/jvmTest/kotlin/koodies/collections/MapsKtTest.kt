package koodies.collections

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class MapsKtTest {

    @Nested
    inner class AddElement {

        @Test
        fun `should add element to list`() {
            val map = mutableMapOf<String, List<String>>()
            map.addElement("test", "element")
            expectThat(map).isEqualTo(mutableMapOf("test" to listOf("element")))
        }
    }

    @Nested
    inner class RemoveElement {

        @Test
        fun `should remove element from list`() {
            val map = mutableMapOf("test" to listOf("element"))
            map.removeElement("test", "element")
            expectThat(map).isEqualTo(mutableMapOf("test" to emptyList()))
        }
    }
}
