@file:Suppress("unused")

package koodies.debug

import koodies.text.CodePoint
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.isMultiline
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode
import koodies.text.Unicode.replacementSymbol
import koodies.text.asCodePointSequence

public class XRay<T>(
    private val subject: T,
    private val stringifier: T.() -> String,
    private val transform: (T.() -> Any)?,
) : CharSequence {

    private fun <T> asString(subject: T): String = when (subject) {
        is Array<*> -> asString(subject.toList())
        else -> subject.toString()
    }

    private fun <T> T.selfString(): String = if (subject.stringifier().isMultiline) {
        "${selfBrackets.first}$LF${asString(this)}$LF${selfBrackets.second}"
    } else {
        "${selfBrackets.first} ${asString(this)} ${selfBrackets.second}"
    }

    private fun <T> T.transformedString(): String = if (asString(this).isMultiline) {
        "${transformedBrackets.first}$LF${asString(this)}$LF${transformedBrackets.second}"
    } else {
        "${transformedBrackets.first} ${asString(this)} ${transformedBrackets.second}"
    }

    private val string: String by lazy {
        transform?.let { subject.selfString() + " " + subject.it().transformedString() } ?: subject.selfString()
    }

    override val length: Int = string.length
    override fun get(index: Int): Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
    override fun toString(): String = string

    /**
     * Returns an instance that applies the given [transform] to [subject].
     */
    public fun transform(transform: T.() -> String): XRay<T> =
        XRay(subject, stringifier, transform)

    /**
     * Returns an instance that applies the given [transform] to
     * the code points the stringified [subject] consists of.
     */
    private fun xray(transform: CodePoint.() -> String): XRay<T> =
        XRay(subject, stringifier) {
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

        private fun highlight(subject: Any?) = subject.toString().formattedAs.debug
        private val selfBrackets = highlight("⟨") to highlight("⟩")
        private val transformedBrackets = highlight("{") to highlight("}")
    }
}

public val <T> T.xray: XRay<T> get() = XRay(this, stringifier = { toString() }, transform = null)
public fun <T> T.xray(): XRay<T> = XRay(this, stringifier = { toString() }, transform = null)
public fun <T> T.xray(transform: T.() -> String): XRay<out T> = XRay(this, stringifier = { toString() }, transform = transform)
public fun <T> T.xray(stringifier: T.() -> String, transform: (T.() -> String)?): XRay<out T> = XRay(this, stringifier = stringifier, transform = transform)

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
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
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
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * at the property `prop` of that value are printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T> T.trace(transform: T.() -> Any?): T =
    apply { println(xray(transform = { transform().toString() })) }
