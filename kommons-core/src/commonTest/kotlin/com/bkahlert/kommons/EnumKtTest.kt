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


        firstEnumValueOfOrNull<InterfaceImplementingEnum> { it.prop == 37 } shouldBe InterfaceImplementingEnum.Bar
        firstEnumValueOfOrNull<InterfaceImplementingEnum, Int>(EnumInterface::prop) { it == 37 } shouldBe InterfaceImplementingEnum.Bar
        firstEnumValueOfOrNull<InterfaceImplementingEnum, Int>(EnumInterface::prop, 37) shouldBe InterfaceImplementingEnum.Bar

        firstEnumValueOfOrNull<InterfaceImplementingEnum> { it.prop == 99 } shouldBe null
        firstEnumValueOfOrNull<InterfaceImplementingEnum, Int>(EnumInterface::prop) { it == 99 } shouldBe null
        firstEnumValueOfOrNull<InterfaceImplementingEnum, Int>(EnumInterface::prop, 99) shouldBe null
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


        firstEnumValueOf<InterfaceImplementingEnum> { it.prop == 37 } shouldBe InterfaceImplementingEnum.Bar
        firstEnumValueOf<InterfaceImplementingEnum, Int>(EnumInterface::prop) { it == 37 } shouldBe InterfaceImplementingEnum.Bar
        firstEnumValueOf<InterfaceImplementingEnum, Int>(EnumInterface::prop, 37) shouldBe InterfaceImplementingEnum.Bar

        shouldThrow<NoSuchElementException> { firstEnumValueOf<InterfaceImplementingEnum> { it.prop == 99 } }
            .message shouldBe "InterfaceImplementingEnum contains no value matching the predicate."
        shouldThrow<NoSuchElementException> { firstEnumValueOf<InterfaceImplementingEnum, Int>(EnumInterface::prop) { it == 99 } }
            .message shouldBe "InterfaceImplementingEnum contains no value of which the property \"prop\" matches the predicate."
        shouldThrow<NoSuchElementException> { firstEnumValueOf<InterfaceImplementingEnum, Int>(EnumInterface::prop, 99) }
            .message shouldBe "InterfaceImplementingEnum contains no value of which the property \"prop\" is \"99\"."
    }


    @Test
    fun predecessor() = testAll {
        TestEnum.Foo.predecessor shouldBe TestEnum.Baz
        TestEnum.Bar.predecessor shouldBe TestEnum.Foo
        TestEnum.Baz.predecessor shouldBe TestEnum.Bar
    }

    @Test
    fun successor() = testAll {
        TestEnum.Foo.successor shouldBe TestEnum.Bar
        TestEnum.Bar.successor shouldBe TestEnum.Baz
        TestEnum.Baz.successor shouldBe TestEnum.Foo
    }
}


internal interface EnumInterface {
    val prop: Int
}

private enum class InterfaceImplementingEnum(
    override val prop: Int
) : EnumInterface {
    Foo(42), Bar(37)
}


enum class TestEnum {
    Foo, Bar, Baz
}
