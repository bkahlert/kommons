@file:Suppress("DEPRECATION")

package koodies.debug

import koodies.debug.Debug.meta
import koodies.text.LineSeparators.LF
import java.lang.reflect.Method

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by providing the current stacktrace.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val currentStackTrace: Array<StackTraceElement>
    get() = Thread.currentThread().stackTrace

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by returning the given [transform] function applied to the current stacktrace.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith(""))
public fun <T> currentStackTrace(transform: StackTraceElement.() -> T): Sequence<T> =
    currentStackTrace.asSequence().map(transform)

/**
 * The class containing the execution point represented by this stack trace element.
 */
public val StackTraceElement.clazz: Class<*> get() = Class.forName(className)

/**
 * The method containing the execution point represented by this stack trace element.
 *
 * If the execution point is contained in an instance or class initializer,
 * this method will be the appropriate *special method name*, `<init>` or
 * `<clinit>`, as per Section 3.9 of *The Java Virtual Machine Specification*.
 */
public val StackTraceElement.method: Method get() = clazz.declaredMethods.single { it.name == methodName }


private val methodRegex = Regex("(?<prefix>.*\\.)(?<method>.*?)(?<suffix>\\(.*)")
private fun highlightMethod(element: StackTraceElement) =
    element.toString().replace(methodRegex) { it.groupValues[1] + it.groupValues[2].meta() + it.groupValues[3] }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` stacktrace and highlighting the method names.
 */
public val Array<StackTraceElement>.trace: Array<StackTraceElement>
    get() =
        also {
            println(joinToString("$LF\t${"at".meta()} ", postfix = LF) {
                highlightMethod(it)
            })
        }
