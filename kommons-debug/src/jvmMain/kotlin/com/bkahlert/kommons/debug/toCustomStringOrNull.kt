package com.bkahlert.kommons.debug

private fun Any.toDefaultString() =
    javaClass.name + "@" + Integer.toHexString(hashCode())

internal actual fun Any?.toCustomStringOrNull(): String? =
    if (this == null) null else toString().takeUnless { it == toDefaultString() }
