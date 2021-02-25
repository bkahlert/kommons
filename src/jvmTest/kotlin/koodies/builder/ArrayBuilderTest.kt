package koodies.builder

import koodies.test.TextFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

@Execution(SAME_THREAD)
class ArrayBuilderTest {

    @Test
    fun `should build with separately built builder`() {

        val array = ArrayBuilder {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using companion method`() {

        val array = ArrayBuilder.buildArray {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using companion object`() {

        val array = ArrayBuilder {
            +"" + TextFile.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFile.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build empty array by default`() {

        val array = ArrayBuilder<String> {}

        expectThat(array.toList()).isEmpty()
    }
}
