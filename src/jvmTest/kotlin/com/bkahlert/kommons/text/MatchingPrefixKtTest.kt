package com.bkahlert.kommons.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrBlank

class MatchingPrefixKtTest {

    @Test
    fun `should find matching prefix`() {
        expectThat("Prom!§\$%&/())pt".matchingPrefix("pt", "Prom!§\$", "om", "&/())p")).isEqualTo("Prom!§\$")
    }

    @Test
    fun `should not find non-matching prefix`() {
        expectThat("Prompt!".matchingPrefix("pt!".trimMargin(), "def")).isNullOrBlank()
    }
}
