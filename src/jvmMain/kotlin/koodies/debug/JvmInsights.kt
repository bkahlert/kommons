package koodies.debug

import koodies.debug.Debug.meta
import koodies.text.LineSeparators.LF

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by providing the current stacktrace.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
val currentStackTrace: Array<StackTraceElement>
    get() = Thread.currentThread().stackTrace

private val methodRegex = Regex("(.*\\.)(.*?)(\\(.*)")
private fun highlightMethod(element: StackTraceElement) =
    element.toString().replace(methodRegex) { it.groupValues[1] + it.groupValues[2].meta() + it.groupValues[3] }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` stacktrace and highlighting the method names.
 */
val Array<StackTraceElement>.trace
    get() =
        also {
            println(joinToString("$LF\t${"at".meta()} ", postfix = LF) {
                highlightMethod(it)
            })
        }
