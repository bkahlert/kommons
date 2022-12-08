package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain

/** A tab */
internal const val t = "\t"

internal enum class EmptyEnum

internal enum class FooBarEnum {
    @Suppress("EnumEntryName") foo_bar,
    FOO_BAR,
}

internal val supportsSoftAssertions by lazy {
    val error = shouldThrow<AssertionError> {
        forAllEnumValues<FooBarEnum> {
            it.name shouldContain "baz"
            it.name shouldContain "BAZ"
        }
    }
    checkNotNull(error.message) {
        "Failed to check soft assertions support"
    }.contains("The following 2 assertions failed")
}
