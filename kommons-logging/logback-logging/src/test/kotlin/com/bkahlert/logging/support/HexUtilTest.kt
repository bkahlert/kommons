package com.bkahlert.logging.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HexUtilTest {

    @Test fun should_compute_span() {
        val span = HexUtil.longToHex(10L)
        assertThat(span).isEqualTo("000000000000000a")
    }

    @Test fun should_compute_very_big_span() {
        val span = HexUtil.longToHex(Long.MAX_VALUE)
        assertThat(span).isEqualTo("7fffffffffffffff")
    }

    @Test fun should_compute_even_bigger_span() {
        val span = HexUtil.longToHex(Long.MIN_VALUE)
        assertThat(span).isEqualTo("8000000000000000")
    }
}
