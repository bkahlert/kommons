package com.bkahlert.kommons

import java.util.Optional

/** If a value [Optional.isPresent], returns the value. Otherwise, returns `null`. */
public inline fun <reified T> Optional<T>?.orNull(): T? = this?.orElse(null)
