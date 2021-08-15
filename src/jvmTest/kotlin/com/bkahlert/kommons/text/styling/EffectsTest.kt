package com.bkahlert.kommons.text.styling

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class EffectsTest {

    @Test
    fun `should style echo`() {
        expectThat("echo".effects.echo()).isEqualTo("·<❮❰❰❰ echo ❱❱❱❯>·")
    }

    @Test
    fun `should style saying`() {
        expectThat("saying".effects.saying()).isEqualTo("͔˱❮❰( saying")
    }

    @Test
    fun `should style tag`() {
        expectThat("tag".effects.tag()).isEqualTo("【tag】")
    }

    @Nested
    inner class UnitStyle {
        @Test
        fun `should style empty string`() {
            expectThat("".effects.unit()).isEqualTo("❲❳")
        }

        @Test
        fun `should style single char string`() {
            expectThat("x".effects.unit()).isEqualTo("❲x❳")
        }

        @Test
        fun `should style two char string`() {
            expectThat("az".effects.unit()).isEqualTo("❲az❳")
        }

        @Test
        fun `should style multi char string`() {
            expectThat("unit".effects.unit()).isEqualTo("❲unit❳")
        }
    }
}
