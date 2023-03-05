package com.bkahlert.kommons.time

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringsKtTest {

    @Test fun as_emoji() = testAll {
        null.asEmoji() shouldBe "❔"
        true.asEmoji() shouldBe "✅"
        false.asEmoji() shouldBe "❌"

        instant0202.asEmoji() shouldBe "🕑"
        instant2232.asEmoji() shouldBe "🕥"

        "other".asEmoji() shouldBe "🔣"
    }
}
