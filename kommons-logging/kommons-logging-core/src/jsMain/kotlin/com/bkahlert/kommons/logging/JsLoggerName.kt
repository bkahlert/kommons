package com.bkahlert.kommons.logging

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/** Name used for logging. */
internal actual val KClass<*>.loggerName: String
    get() = js.name

/** Returns the name for logging using the specified [fn] to compute the subject in case `this` object is `null`. */
internal actual fun Any?.loggerName(fn: KFunction<*>): String =
    when (this) {
        null -> caller("loggerName", "provideDelegate") ?: "<global>"
        else -> this::class.loggerName
    }

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun caller(vararg callers: String): String? {
    val callSites = callSites()
    val name = if (callSites != null) callSites.caller(callers) else parsedCaller(callers)
    return name
        ?.takeUnless { it.isEmpty() }
        // `init_properties_fixtures_kt_abc123` (K1) and `_init_properties_fixtures_kt__v9exze` (K2) → `init_properties_fixtures_kt`
        ?.replace(Regex("^_?(.*_kt)_+[a-z0-9]+$")) {
            it.groupValues[1]
        }
}

/**
 * Returns the structured stack trace V8 passes to `Error.prepareStackTrace`,
 * or `null` on engines without that hook, which leave `stack` a string.
 */
private fun callSites(): Array<dynamic>? {
    val error = js("Error")
    val prepareStackTrace = error.prepareStackTrace
    error.prepareStackTrace = { _: dynamic, callSites: dynamic -> callSites }
    val stack: dynamic = try {
        js("new Error()").stack
    } finally {
        error.prepareStackTrace = prepareStackTrace
    }
    return if (jsTypeOf(stack) == "string") null else stack.unsafeCast<Array<dynamic>?>()
}

/**
 * Returns the class of the receiver that called one of [callers], like the JVM's stack trace element does,
 * or the calling function's name if it has no receiver, or `null` if that frame is anonymous.
 *
 * Kotlin 2 emits methods as `protoOf(C).m = function () {}`, which V8 names `protoOf.m` in the `stack` string;
 * only the call site's `getTypeName()` knows `C`.
 */
private fun Array<dynamic>.caller(callers: Array<out String>): String? {
    fun isCaller(callSite: dynamic): Boolean {
        val function = callSite.getFunctionName().unsafeCast<String?>() ?: return false
        return callers.any { function == it || function.endsWith(".$it") }
    }

    val callSite = asSequence()
        .dropWhile { !isCaller(it) }
        .dropWhile { isCaller(it) }
        .firstOrNull() ?: return null
    return callSite.getTypeName().unsafeCast<String?>()?.takeUnless { it.isEmpty() }
        ?: callSite.getFunctionName().unsafeCast<String?>()
}

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun parsedCaller(callers: Array<out String>): String? {
    val callerPatterns = callers.flatMap { it.patterns() }
    return stackTrace()
        .dropWhile { callerPatterns.any { pattern -> it.contains(pattern) } }
        .firstOrNull()
        ?.replaceFirst(Regex("^(?:\\s*at\\s+|[^<]*</)"), "") // Remove a `  at ` resp. `./path/file.js/</` prefix
        ?.split('.', ' ', limit = 2)
        ?.first()
        // Firefox frames are `name@location`; an anonymous frame (e.g. a Kotlin 2 method, `protoOf(C).m = function () {}`,
        // which Firefox cannot name) has nothing before the `@` and yields no caller.
        ?.substringBefore('@')
}

private fun String.patterns() = listOf(
    ".$this",
    " $this",
    "$this@",
)

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun stackTrace() = try {
    throw RuntimeException()
} catch (ex: Throwable) {
    ex.stackTraceToString().removeSuffix("\n")
}.lineSequence()
    .dropWhile {
        it.startsWith("RuntimeException") ||
            it.startsWith("captureStack@") || // Firefox
            it.startsWith("captureStack ") // Chrome
    }
