package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValueRangeTest {

    @Test fun to_string() = testAll {
        ValueRange.Normalized.toString() shouldBe "0.0..1.0"
        ValueRange.Bytes.toString() shouldBe "00..ff"
        ValueRange.Angle.toString() shouldBe "0.0°..360.0°"
        ValueRange.Percent.toString() shouldBe "0.0%..100.0%"
        ValueRange.Scaling.toString() shouldBe "-1.0..+1.0"
    }
}
