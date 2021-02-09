package koodies.builder

import koodies.test.TextFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Execution(SAME_THREAD)
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
    fun `should build using companion method`() {

        val list = ListBuilder.buildList {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(list).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using companion object`() {

        val list = ListBuilder<String> {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(list).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }
}
