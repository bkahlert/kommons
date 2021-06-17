@file:Suppress("unused")

package koodies.debug

import koodies.collections.map
import koodies.debug.XRay.Companion.highlight
import koodies.math.toHexadecimalString
import koodies.regex.groupValue
import koodies.runtime.CallStackElement
import koodies.runtime.getCaller
import koodies.text.CodePoint
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.isMultiline
import koodies.text.Semantics.BlockDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode
import koodies.text.Unicode.replacementSymbol
import koodies.text.asCodePointSequence

public class XRay<T>(
    private val description: CharSequence?,
    private val subject: T,
    private val stringifier: (T.() -> String)?,
    private val transform: (T.() -> Any)?,
) : CharSequence {

    private fun <T> asString(subject: T): String = when (subject) {
        is Array<*> -> asString(subject.toList())
        is ByteArray -> asString(subject.toHexadecimalString())
        is UByteArray -> asString(subject.toHexadecimalString())
        else -> subject.toString()
    }

    private fun <T> T.selfString(): String {
        val selfString = stringifier?.let { it(subject) } ?: asString(subject)
        return if (selfString.isMultiline) {
            "${selfBrackets.first}$LF$selfString$LF${selfBrackets.second}"
        } else {
            "${selfBrackets.first} $selfString ${selfBrackets.second}"
        }
    }

    private fun <T> T.transformedString(): String {
        val transformedString = asString(this)
        return if (transformedString.isMultiline) {
            "${transformedBrackets.first}$LF$transformedString$LF${transformedBrackets.second}"
        } else {
            "${transformedBrackets.first} $transformedString ${transformedBrackets.second}"
        }
    }

    private val string: String = run {
        val source = getCaller {
            receiver?.endsWith(".InsightsKt") == true ||
                receiver?.endsWith(".XRay") == true ||
                function == "trace" ||
                function == "xray"
        }.run { ".⃦⃥ͥ ".formattedAs.debug + "($file:$line) ".formattedAs.meta + (description?.let { "$it " } ?: "").formattedAs.debug }
        source + run {
            transform?.let {
                subject.selfString() + " " + subject.it().transformedString()
            } ?: subject.selfString()
        }
    }

    override val length: Int = string.length
    override fun get(index: Int): Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
    override fun toString(): String = string
    public fun print(): T {
        println(toString())
        return subject
    }

    /**
     * Returns an instance that applies the given [transform] to [subject].
     */
    public fun transform(transform: T.() -> String): XRay<T> =
        XRay(description, subject, stringifier, transform)

    /**
     * Returns an instance that applies the given [transform] to
     * the code points the stringified [subject] consists of.
     */
    private fun xray(transform: CodePoint.() -> String): XRay<T> =
        XRay(description, subject, stringifier) {
            val sb = StringBuilder()
            asString(subject).asCodePointSequence().forEach { sb.append(it.transform()) }
            sb.toString()
        }

    public val invisibles: XRay<T>
        get() = xray {
            val current = string
            if (current.length == 1) {
                Unicode.controlCharacters[current[0]]?.toString() ?: current
            } else {
                current
            }
        }

    public val breaks: XRay<T>
        get() = xray {
            when (val current = string) {
                LineSeparators.NEL -> lineBreakSymbol("␤")
                LineSeparators.PS -> lineBreakSymbol("ₛᷮ")
                LineSeparators.LS -> lineBreakSymbol("ₛᷞ")
                in LineSeparators -> lineBreakSymbol(replacementSymbol.toString())
                else -> current
            }
        }

    public companion object {
        private fun lineBreakSymbol(lineBreak: String) = "⏎$lineBreak"

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
public fun <T> T.xray(description: CharSequence? = null, transform: (T.() -> Any)?): XRay<out T> =
    XRay(description, this, stringifier = null, transform = transform)

public fun <T> T.xray(description: CharSequence? = null, stringifier: T.() -> String, transform: (T.() -> Any)?): XRay<out T> =
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
public fun <T> T.trace(description: CharSequence? = null, transform: (T.() -> Any)? = null): T =
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
