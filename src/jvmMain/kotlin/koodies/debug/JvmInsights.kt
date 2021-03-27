@file:Suppress("DEPRECATION")

package koodies.debug

import koodies.debug.Debug.meta
import koodies.regex.groupValue
import koodies.text.LineSeparators.LF

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` thread and highlighting its details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T : Thread> T.trace: Thread
    get() = apply { println(xray({ highlightThreadName() }, null)) }

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` thread and `this` thread applied to the given [transform] to [println]
 * while still returning `this`.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T : Thread> T.trace(transform: (T.() -> Any?)): T =
    apply { println(xray({ highlightThreadName() }, { transform().toString() })) }


private val threadDetailsRegex = Regex("(?<prefix>.*?\\[)(?<details>.*)(?<suffix>].*?)")
private fun Thread.highlightThreadName() =
    toString().replace(threadDetailsRegex) {
        val prefix = it.groupValue("prefix")
        val suffix = it.groupValue("suffix")
        val details = it.groupValue("details")?.split(",")?.joinToString(", ") { detail -> detail.meta() }
        prefix + details + suffix
    }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` stacktrace and highlighting the method names.
 */
public val Array<StackTraceElement>.trace: Array<StackTraceElement>
    get() = also { println(joinToString("$LF\t${"at".meta()} ", postfix = LF) { highlightMethod(it) }) }

private val methodRegex = Regex("(?<prefix>.*\\.)(?<method>.*?)(?<suffix>\\(.*)")
private fun highlightMethod(element: StackTraceElement) =
    element.toString().replace(methodRegex) {
        val prefix = it.groupValue("prefix")
        val suffix = it.groupValue("suffix")
        val methodName = it.groupValue("method")?.meta()
        prefix + methodName + suffix
    }
