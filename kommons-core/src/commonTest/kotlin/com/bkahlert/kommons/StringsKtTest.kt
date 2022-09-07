package com.bkahlert.kommons

import com.bkahlert.kommons.RoundingMode.Ceiling
import com.bkahlert.kommons.RoundingMode.Floor
import com.bkahlert.kommons.RoundingMode.HalfUp
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import kotlin.test.Test

class StringsKtTest {

    @Test fun empty() = testAll {
        String.EMPTY shouldBe ""
    }

    @Test fun random_string() = testAll {
        randomString() shouldHaveLength 16
        randomString(7) shouldHaveLength 7

        val allowedByDefault = (('0'..'9') + ('a'..'z') + ('A'..'Z')).toList()
        randomString(10).forAll { allowedByDefault shouldContain it }

        randomString(10, 'A', 'B').forAll { listOf('A', 'B') shouldContain it }
    }

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

    @Test fun pluralize() = testAll {
        "bus".pluralize() shouldBe "buses"
        "tax".pluralize() shouldBe "taxes"
        "blitz".pluralize() shouldBe "blitzes"
        "lunch".pluralize() shouldBe "lunches"
        "marsh".pluralize() shouldBe "marshes"
        "city".pluralize() shouldBe "cities"
        "ray".pluralize() shouldBe "rays"
        "word".pluralize() shouldBe "words"
    }
}

internal val String.cs: CharSequence get() = StringBuilder(this)
