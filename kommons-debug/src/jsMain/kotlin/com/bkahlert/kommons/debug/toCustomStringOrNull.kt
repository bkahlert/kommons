package com.bkahlert.kommons.debug

internal actual fun Any?.toCustomStringOrNull(): String? =
    if (this == null) null else toString().takeUnless { it == "[object Object]" }
