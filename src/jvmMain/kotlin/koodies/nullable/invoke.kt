package koodies.nullable

/**
 * 👍 ✅ `... transform(a) ...`
 *
 * 👎 ❌ `... if(transform != null) transform(arg) else arg ...`
 *
 * This helper lambda extension function allows an optional identity lambda `((T) -> T)?`
 * be called as if it was provided:
 * - `f(a) -> a'` if set
 * - `a -> a` otherwise.
 *
 * @sample InvokeSamples.optionalTransformation
 * @sample InvokeSamples.optionalNullableReturningTransformation
 */
operator fun <A, F : (A) -> A> F?.invoke(arg: A) = this?.invoke(arg) ?: arg

/**
 * 👍 ✅ `... transform(a) ...`
 *
 * 👎 ❌ `... if(transform != null) transform(arg) else arg ...`
 *
 * This helper lambda extension function allows an optional identity lambda `((T) -> T)?`
 * be called as if it was provided:
 * - `f(a) -> a'` if set
 * - `a -> a` otherwise.
 *
 * @sample InvokeSamples.optionalTransformation
 * @sample InvokeSamples.optionalNullableReturningTransformation
 */
@Deprecated(
    message = "Replace to easily import \"koodies.nullable.invoke\" ...",
    replaceWith = ReplaceWith("this.invoke(arg)", "koodies.nullable.invoke"),
    DeprecationLevel.WARNING
)
fun <A, F : (A) -> A> F?.invokeIfSet(arg: A) = this.invoke(arg)

/**
 * 👍 ✅ `... transform(a) ...`
 *
 * 👎 ❌ `... if(transform != null) transform(arg) else arg ...`
 *
 * This helper lambda extension function allows an optional identity lambda `((T) -> T)?`
 * be called as if it was provided:
 * - `f(a) -> a'` if set
 * - `a -> a` otherwise.
 *
 * @sample InvokeSamples.optionalTransformation
 * @sample InvokeSamples.optionalNullableReturningTransformation
 */
fun <A> A.let(block: ((A) -> A?)?): A = block?.invoke(this) ?: this

/**
 * 👍 ✅ `...  interceptor.invokeOrArg(additionalInterceptor.invokeOrArg(msg)) ...`
 *
 * 👎 ❌ `... msg: String -> additionalInterceptor(msg)?.let { it -> interceptor(it) } ...`
 *
 * This helper lambda extension function allows an optional identity lambda `((T) -> T?)?`
 * be called as if it was provided and never returned `null`:
 * - `f(a) -> a'` if f and a' set
 * - `f(a) -> a` if f set and f(a) unset
 * - `a -> a` otherwise.
 *
 * @sample InvokeSamples.optionalTransformation
 * @sample InvokeSamples.optionalNullableReturningTransformation
 */
@Deprecated(
    message = "Replace to easily import \"koodies.nullable.let\" ...",
    replaceWith = ReplaceWith("this.let(block)", "koodies.nullable.let"),
    DeprecationLevel.WARNING
) fun <A> A.letIfSet(block: ((A) -> A?)?): A = block?.invoke(this) ?: this

private object InvokeSamples {
    fun optionalTransformation() {
        fun someFunction(arg: String, optionalTransformation: ((String) -> String)? = null): String =
            optionalTransformation(arg)

        println(someFunction("text") { "<strong>$it</strong>" }) // ➜ <strong>text</strong>
        println(someFunction("text")) // ➜ text
    }

    fun optionalNullableReturningTransformation() {
        "text".let { "-$it-" }.let { "->$it<-" } // ➜ ->-text-<-

        "text".let { null }.let { "->$it<-" } // ->null<-
    }
}
