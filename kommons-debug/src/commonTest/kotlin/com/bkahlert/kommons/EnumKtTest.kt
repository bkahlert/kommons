package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EnumKtTest {

    @Test fun first_enum_value_of_or_null() = testAll {
        firstEnumValueOfOrNull<RoundingMode> { it.name == "HalfEven" } shouldBe RoundingMode.HalfEven
        firstEnumValueOfOrNull(RoundingMode::name) { it == "HalfEven" } shouldBe RoundingMode.HalfEven
        firstEnumValueOfOrNull(RoundingMode::name, "HalfEven") shouldBe RoundingMode.HalfEven

        firstEnumValueOfOrNull<RoundingMode> { it.name == "-" } shouldBe null
        firstEnumValueOfOrNull(RoundingMode::name) { it == "-" } shouldBe null
        firstEnumValueOfOrNull(RoundingMode::name, "-") shouldBe null
    }

    @Test fun first_enum_value_of() = testAll {
        firstEnumValueOf<RoundingMode> { it.name == "HalfEven" } shouldBe RoundingMode.HalfEven
        firstEnumValueOf(RoundingMode::name) { it == "HalfEven" } shouldBe RoundingMode.HalfEven
        firstEnumValueOf(RoundingMode::name, "HalfEven") shouldBe RoundingMode.HalfEven

        shouldThrow<NoSuchElementException> { firstEnumValueOf<RoundingMode> { it.name == "-" } }
            .message shouldBe "RoundingMode contains no value matching the predicate."
        shouldThrow<NoSuchElementException> { firstEnumValueOf(RoundingMode::name) { it == "-" } }
            .message shouldBe "RoundingMode contains no value of which the property \"name\" matches the predicate."
        shouldThrow<NoSuchElementException> { firstEnumValueOf(RoundingMode::name, "-") }
            .message shouldBe "RoundingMode contains no value of which the property \"name\" is \"-\"."
    }
}
