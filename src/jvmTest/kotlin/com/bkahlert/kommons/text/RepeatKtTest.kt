package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.too
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.assertions.isEqualTo

class RepeatKtTest {

    @TestFactory
    fun `should repeat`() = testEachOld(
        3 to "AAA" too "ABC 🤯ABC 🤯ABC 🤯",
        1 to "A" too "ABC 🤯",
        0 to "" too "",
    ) { (repeatCount, repeatedChar, repeatedString) ->

        expecting("$repeatCount x A = $repeatedChar") { 'A'.repeat(repeatCount) } that { isEqualTo(repeatedChar) }
        expecting("$repeatCount x ABC🤯 = $repeatedString") { "ABC 🤯".repeat(repeatCount) } that { isEqualTo(repeatedString) }
    }

    @Test
    fun `should return throw on negative Char repeat`() {
        expectCatching { 'A'.repeat(-1) }
    }

    @Test
    fun `should return throw on negative String repeat`() {
        expectCatching { "ABC 🤯".repeat(-1) }
    }
}
