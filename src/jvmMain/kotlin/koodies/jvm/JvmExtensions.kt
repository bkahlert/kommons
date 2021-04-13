package koodies.jvm

import java.util.Optional

/**
 * If a value [Optional.isPresent], returns the value. Otherwise returns `null`.
 */
public fun <T> Optional<T>?.orNull(): T? = this?.orElse(null)
