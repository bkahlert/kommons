package koodies.builder

import koodies.test.TextFile
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

class ListBuilderTest {

    @Test
    fun `should build with separately built builder`() {

        val list = ListBuilder<String>().build {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(list).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using top-level function`() {

        val list = buildList {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(list).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using companion object`() {

        val list = ListBuilder {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(list).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build empty list by default`() {

        val list = ListBuilder<String> {}

        expectThat(list).isEmpty()
    }
}
