@file:Suppress("unused")

package com.bkahlert.kommons.debug

import com.bkahlert.kommons.collections.map
import com.bkahlert.kommons.debug.XRay.Companion.highlight
import com.bkahlert.kommons.math.toHexadecimalString
import com.bkahlert.kommons.regex.groupValue
import com.bkahlert.kommons.runtime.CallStackElement
import com.bkahlert.kommons.runtime.getCaller
import com.bkahlert.kommons.text.CodePoint
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.Semantics.BlockDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons.text.Unicode.replacementSymbol
import com.bkahlert.kommons.text.asCodePointSequence


public class XRay<T>(
    private val description: CharSequence?,
    private val subject: T,
    private val stringifier: ((T) -> String)?,
    private val transform: ((T) -> Any?)?,
) {

    override fun toString(): String = buildString {
        val caller = getCaller {
            receiver?.endsWith(".InsightsKt") == true ||
                receiver?.endsWith(".XRay") == true ||
                function == "trace" ||
                function == "xray"
        }
        append(".⃦⃥ͥ ".formattedAs.debug)
        append("(${caller.file}:${caller.line}) ".formattedAs.meta)
        description?.also {
            append(it.formattedAs.debug)
            append(" ")
        }

        appendWrapped(stringifier?.invoke(subject) ?: defaultStringify(subject), selfBrackets)
        val transformed = transform?.invoke(subject)
        if (transformed != null) {
            append(" ")
            appendWrapped(defaultStringify(transformed), transformedBrackets)
        }
    }

    /**
     * Returns an instance that applies the given [transform] to [subject].
     */
    public fun transform(transform: (T) -> String): XRay<T> =
        XRay(description, subject, stringifier, transform)

    /**
     * Returns an instance that applies the given [transform] to
     * the code points the stringified [subject] consists of.
     */
    private fun xray(transform: (CodePoint) -> String): XRay<T> =
        XRay(description, subject, stringifier) { subject ->
            buildString {
                defaultStringify(subject).asCodePointSequence().forEach { append(transform(it)) }
            }
        }

    public val invisibles: XRay<T>
        get() = xray { codePoint ->
            codePoint.char
                ?.let { char -> Unicode.controlCharacters[char]?.toString() }
                ?: codePoint.string
        }

    public val breaks: XRay<T>
        get() = xray { codePoint ->
            when (val string = codePoint.string) {
                LineSeparators.NEL -> lineBreakSymbol("␤")
                LineSeparators.PS -> lineBreakSymbol("ₛᷮ")
                LineSeparators.LS -> lineBreakSymbol("ₛᷞ")
                in LineSeparators -> lineBreakSymbol(codePoint.replacementSymbol.toString())
                else -> string
            }
        }

    public companion object {
        private val LINE_BREAK_REGEX = Regex("[${Unicode.CARRIAGE_RETURN}${Unicode.LINE_FEED}|${Unicode.LINE_FEED}|${Unicode.CARRIAGE_RETURN}]")
        private val String.isMultiline: Boolean get() = LINE_BREAK_REGEX.find(this) != null

        private fun lineBreakSymbol(lineBreak: String) = "⏎$lineBreak"
        private fun <T> defaultStringify(subject: T): String {
            return when (subject) {
                is Array<*> -> defaultStringify(subject.toList())
                is ByteArray -> defaultStringify(subject.toHexadecimalString())
                is UByteArray -> defaultStringify(subject.toHexadecimalString())
                else -> subject.toString()
            }
        }

        private fun StringBuilder.appendWrapped(value: String, brackets: Pair<String, String>) {
            val separator = if (value.isMultiline) Unicode.LINE_FEED else ' '
            append(brackets.first)
            append(separator)
            append(value)
            append(separator)
            append(brackets.second)
        }

        internal fun highlight(subject: Any?) = subject.toString().formattedAs.debug
        private val selfBrackets = BlockDelimiters.UNIT.map { it.formattedAs.debug }
        private val transformedBrackets = BlockDelimiters.BLOCK.map { it.formattedAs.debug }
    }
}


/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` to an instance of [XRay] while returning `this` on [XRay.print].
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().xray.breaks.print().calls()
 * ```
 *
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * will be printed.
 */
public val <T> T.xray: XRay<T> get() = XRay(null, this, stringifier = null, transform = null)

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` to an instance of [XRay] while returning `this` on [XRay.print].
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().xray("optional label").breaks.print().calls()
 * ```
 *
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * will be printed.
 */
public fun <T> T.xray(description: CharSequence? = null): XRay<T> = XRay(description, this, stringifier = null, transform = null)
public fun <T> T.xray(description: CharSequence? = null, transform: (T.() -> Any?)?): XRay<out T> =
    XRay(description, this, stringifier = null, transform = transform)

public fun <T> T.xray(description: CharSequence? = null, stringifier: T.() -> String, transform: (T.() -> Any?)?): XRay<out T> =
    XRay(description, this, stringifier = stringifier, transform = transform)

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` to [println] while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.calls()
 * ```
 *
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * will be printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T> T.trace: T
    get() : T = apply { println(xray) }

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` and `this` applied to the given [transform] to [println]
 * while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace { prop }.calls()
 * ```
 *
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * at the property `prop` of that value are printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T> T.trace(description: CharSequence? = null, transform: (T.() -> Any?)? = null): T =
    apply { println(xray(description, transform = transform)) }


public val CallStackElement.highlightedMethod: String
    get() = toString().replace(Regex("(?<prefix>.*\\.)(?<method>.*?)(?<suffix>\\(.*)")) {
        val prefix = it.groupValue("prefix")
        val suffix = it.groupValue("suffix")
        val methodName = highlight(it.groupValue("method"))
        prefix + methodName + suffix
    }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` stacktrace and highlighting the method names.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val Array<CallStackElement>.trace: Array<CallStackElement>
    get() = also { println(joinToString("$LF\t${highlight("at")} ", postfix = LF) { it.highlightedMethod }) }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing `this` stacktrace and highlighting the method names.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val Iterable<CallStackElement>.trace: Iterable<CallStackElement>
    get() = also { println(joinToString("$LF\t${highlight("at")} ", postfix = LF) { it.highlightedMethod }) }
