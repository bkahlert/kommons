package com.bkahlert.kommons.text

import com.bkahlert.kommons.RoundingMode.Ceiling
import com.bkahlert.kommons.RoundingMode.Floor
import com.bkahlert.kommons.RoundingMode.HalfUp
import com.bkahlert.kommons.debug.asEmoji
import com.bkahlert.kommons.instant0202
import com.bkahlert.kommons.instant2232
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EmojiKtTest {

    @Test fun as_emoji() = testAll {
        null.asEmoji() shouldBe "❔"
        true.asEmoji() shouldBe "✅"
        false.asEmoji() shouldBe "❌"

        instant0202.asEmoji() shouldBe "🕝"
        instant0202.asEmoji(Ceiling) shouldBe "🕝"
        instant0202.asEmoji(Floor) shouldBe "🕑"
        instant0202.asEmoji(HalfUp) shouldBe "🕑"

        instant2232.asEmoji() shouldBe "🕚"
        instant2232.asEmoji(Ceiling) shouldBe "🕚"
        instant2232.asEmoji(Floor) shouldBe "🕥"
        instant2232.asEmoji(HalfUp) shouldBe "🕥"

        "other".asEmoji() shouldBe "🔣"
    }
}
