package com.bkahlert.kommons.builder

import com.bkahlert.kommons.test.TextFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty

class ArrayBuilderTest {

    @Test
    fun `should build with separately built builder`() {

        val array = ArrayBuilder {
            +"" + TextFixture.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFixture.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using top-level function`() {

        val array = buildArray {
            +"" + TextFixture.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFixture.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build using companion object`() {

        val array = ArrayBuilder {
            +"" + TextFixture.text
            +"𓌈🥸𓂈"
        }

        expectThat(array.toList()).containsExactly("", TextFixture.text, "𓌈🥸𓂈")
    }

    @Test
    fun `should build empty array by default`() {

        val array = ArrayBuilder<String> {}

        expectThat(array.toList()).isEmpty()
    }
}
