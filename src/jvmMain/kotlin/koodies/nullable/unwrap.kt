package koodies.nullable

import java.util.Optional

/**
 * 👍 ✅ `... val text:String? = optionalString.unwrap ...`
 *
 * 👎 ❌ `... val text:Optional<String> = optionalString ...`
 *
 * ❌ ❌ `... val text:Optional<String>? = #$@&%*!!!!!!! ...`
 *
 * This helper lambda extension function allows an Java [Optional]
 * be unwrapped to `T?`—no matter if the [Optional]
 * is `Optional<T>` or `Optional<T>?` (luckily `Optional<T?>` or `Optional<T?>?` can't be declared).
 *
 * @sample Samples.process
 * @sample Samples.throwException
 */
public val <T> Optional<T>?.unwrap: T?
    get() = this?.orElse(null)

private object Samples {
    private fun someJavaMethod(): Optional<String> = Optional.empty()

    fun process() {
        val optional: Optional<String> = someJavaMethod()
        println("${optional.unwrap?.length ?: 0} characters left")
    }

    fun throwException(): Int {
        val optional: Optional<String> = someJavaMethod()
        return optional.unwrap?.count() ?: throw NoSuchElementException("input missing")
    }
}
