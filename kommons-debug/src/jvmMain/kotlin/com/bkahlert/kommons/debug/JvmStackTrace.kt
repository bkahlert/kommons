package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.EMPTY
import com.bkahlert.kommons.Platform
import java.lang.reflect.Method
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction

/** Representation of a single element of a [StackTrace] on [Platform.JVM]. */
public data class JvmStackTraceElement(
    override val receiver: String,
    override val function: String,
    override val file: String?,
    override val line: Int,
    override val column: Int?,
) : StackTraceElement {
    public constructor(native: java.lang.StackTraceElement) : this(native.className, native.methodName, native.fileName, native.lineNumber, null)

    public override val demangledFunction: String = StackTrace.demangleFunction(function)

    override fun toString(): String = "$receiver.$function($file:$line${column?.let { ":$it" } ?: String.EMPTY})"
}

private val functionMangleRegex: Regex = "\\$.*$".toRegex()
private val generatedFunctionRegex = "\\\$.*".toRegex()

/** Returns the specified [function] with mangling information removed. */
public actual fun StackTrace.Companion.demangleFunction(function: String): String =
    function.replace(functionMangleRegex, String.EMPTY)


/** The [Class] containing the execution point represented by this element. */
public val StackTraceElement.`class`: Class<*> get() = Class.forName(receiver)

/** The [KClass] containing the execution point represented by this element. */
public val StackTraceElement.kClass: KClass<*> get() = `class`.kotlin


/** The method containing the execution point represented by this element. */
public val StackTraceElement.method: Method get() = `class`.declaredMethods.single { it.name == function }

/** Gets the current [StackTrace]. */
@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
public actual inline fun StackTrace.Companion.get(): StackTrace =
    Thread.currentThread().stackTrace
        .dropWhile { it.className == Thread::class.qualifiedName }
        .map { JvmStackTraceElement(it) }
        .let { StackTrace(it) }

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public fun StackTrace.findByLastKnownCallsOrNull(vararg functions: Pair<String?, String>): StackTraceElement? {
    var skipInvoke = false
    val demangledFunctions = functions.map { (receiver, function) -> receiver to StackTrace.demangleFunction(function) }
    return findOrNull {
        if (demangledFunctions.any { (receiver, demangledFunction) -> it.receiver == receiver && it.demangledFunction == demangledFunction }) {
            skipInvoke = true
            true
        } else {
            if (skipInvoke) it.function == "invoke" else false
        }
    }
}

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [receiver] and [function].
 */
public fun StackTrace.findByLastKnownCallsOrNull(receiver: KClassifier, function: String): StackTraceElement? =
    findByLastKnownCallsOrNull(receiver.toString() to function)

private fun String.javaBeanMethodToKotlinProperty() = takeUnless {
    length >= 4 && substring(0, 3).let { it == "get" || it == "set" } && this[3].isUpperCase()
} ?: substring(3).replaceFirstChar { it.lowercase(Locale.getDefault()) }

private fun String.simplifyFunction() =
    replace(generatedFunctionRegex, String.EMPTY).javaBeanMethodToKotlinProperty()

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public actual fun StackTrace.findByLastKnownCallsOrNull(vararg functions: String): StackTraceElement? {
    var skipInvoke = false
    val simplifiedFunctions = functions.map { StackTrace.demangleFunction(it).simplifyFunction() }

    return findOrNull {
        if (simplifiedFunctions.contains(it.demangledFunction?.simplifyFunction())) {
            skipInvoke = true
            true
        } else {
            if (skipInvoke) it.function == "invoke" else false
        }
    }
}

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public actual fun StackTrace.findByLastKnownCallsOrNull(vararg functions: KFunction<*>): StackTraceElement? =
    findByLastKnownCallsOrNull(*functions.map { it.name }.toTypedArray())
