package com.bkahlert.kommons.test

/** A tab */
internal const val t = "\t"

internal enum class EmptyEnum

internal enum class FooBarEnum {
    @Suppress("EnumEntryName") foo_bar,
    @Suppress("EnumEntryName") FOO_BAR,
}
