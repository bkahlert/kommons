package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PluralizeKtTest {

    @Test fun pluralize() = testAll(
        "bus" to "buses",
        "tax" to "taxes",
        "blitz" to "blitzes",
        "lunch" to "lunches",
        "marsh" to "marshes",
        "city" to "cities",
        "ray" to "rays",
        "word" to "words",
    ) { (singular, plural) ->
        singular.pluralize() shouldBe plural
        singular.cs.pluralize() shouldBe plural
    }
}
