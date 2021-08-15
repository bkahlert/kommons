package com.bkahlert.kommons.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PrefixKtTest {

    @Test
    fun `should add prefix`() {
        expectThat("12345     12345".prefixWith("abc")).isEqualTo("abc12345     12345")
    }

    @Test
    fun `should do nothing on empty prefix`() {
        expectThat("12345     12345".prefixWith("")).isEqualTo("12345     12345")
    }
}
