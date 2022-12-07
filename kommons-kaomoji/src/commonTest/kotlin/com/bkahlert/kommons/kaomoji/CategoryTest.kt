@file:Suppress(
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection"
)

package com.bkahlert.kommons.kaomoji

import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlin.js.JsName
import kotlin.test.Test

class CategoryTest {

    private fun category(
        elements: MutableList<Lazy<Kaomoji>> = mutableListOf(),
    ): Category = object : Category(elements) {
        @JsName("test01")
        val `〔ಠ_¬〕o︠・━・` by parsing()

        val test02 by parsing("〔ಠ_¬〕o︠・━・")

        @JsName("test03")
        val `〔ಠ_¬〕o︠・━・・・・・・・・・` by parts(0..0, 1..1, 2..2, 3..3, 4..4, 5..9)

        val test04 by parts("〔", "ಠ", "_", "¬", "〕", "o︠・━・")
    }

    @Test fun instantiate() = category().testAll {
        it.toString() shouldBe "〔ಠ_¬〕o︠・━・"
    }

    @Test fun lazy() = testAll {
        val elements: MutableList<Lazy<Kaomoji>> = mutableListOf()
        val category = category(elements)
        elements.forAll { it.isInitialized() shouldBe false }

        for (i in category.indices) category[i] // triggers instantiation
        elements.forAll { it.isInitialized() shouldBe true }
    }
}
