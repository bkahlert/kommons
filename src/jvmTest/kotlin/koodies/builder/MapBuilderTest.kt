package koodies.builder

import koodies.test.TextFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

class MapBuilderTest {

    @Test
    fun `should build with separately built builder`() {

        val map = MapBuilder<String, Int>().build {
            "ten" to 5
            put("some", TextFixture.text.length)
            "𓌈🥸𓂈".let { it to it.length }
        }

        expectThat(map.toList()).containsExactly("ten" to 5, "some" to 11, "𓌈🥸𓂈" to 6)
    }

    @Test
    fun `should build using top-level function`() {

        val map = buildMap<String, Int> {
            "ten" to 5
            put("some", TextFixture.text.length)
            "𓌈🥸𓂈".let { it to it.length }
        }

        expectThat(map.toList()).containsExactly("ten" to 5, "some" to 11, "𓌈🥸𓂈" to 6)
    }

    @Test
    fun `should build using companion object`() {

        val map: Map<String, Int> = MapBuilder {
            "ten" to 5
            put("some", TextFixture.text.length)
            "𓌈🥸𓂈".let { it to it.length }
        }

        expectThat(map.toList()).containsExactly("ten" to 5, "some" to 11, "𓌈🥸𓂈" to 6)
    }

    @Test
    fun `should build empty map by default`() {

        val map = MapBuilder<String, Int> {}

        expectThat(map).isEmpty()
    }
}
