package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.test.KommonsTest
import com.bkahlert.kommons.test.LambdaBody
import com.bkahlert.kommons.test.SLF4J
import com.bkahlert.kommons.test.UnicodeFont
import com.bkahlert.kommons.test.rootCause
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.decapitalize
import com.bkahlert.kommons.text.quoted
import java.nio.file.Path
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.reflect
import kotlin.streams.asSequence

/** A generator for dynamic test names. */
public object DynamicTestDisplayNameGenerator {

    internal const val FOR: String = "ꜰᴏʀ"

    @Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
    internal const val PROPERTY: String = "ᴩʀᴏᴩᴇʀᴛy"

    @Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
    internal const val FUNCTION: String = "ꜰᴜɴᴄᴛɪᴏɴ"

    @Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
    internal const val VALUE_OF: String = "ᴠᴀʟᴜᴇ ᴏꜰ"

    @Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
    internal const val RETURN: String = "ʀᴇᴛᴜʀɴ"

    @Suppress("MemberVisibilityCanBePrivate")
    internal val CLASS: String = UnicodeFont.SansSerifItalic.format("class")

    @Suppress("MemberVisibilityCanBePrivate")
    internal val OBJECT: String = UnicodeFont.SansSerifItalic.format("object")

    @Suppress("MemberVisibilityCanBePrivate")
    internal val NULL: String = UnicodeFont.SansSerifItalic.format("null")

    /**
     * Calculates the display name for a test with the specified [subject],
     * and the optional [testNamePattern] which supports curly placeholders `{}` like [SLF4J] does.
     *
     * If no [testNamePattern] is specified a [displayNameFallback] is calculated heuristically.
     *
     * @see displayNameFallback
     */
    public fun <T> displayNameFor(subject: T, testNamePattern: String? = null): String {
        val (fallbackPattern: String, args: Array<*>) = displayNameFallback(subject)
        return SLF4J.format(testNamePattern ?: fallbackPattern, *args).ansiRemoved
    }

    /**
     * Attempts to calculate a display name for a test case testing the specified [subject].
     */
    private fun displayNameFallback(subject: Any?): Pair<String, Array<String>> = when (subject) {
        null -> "{}" to arrayOf(NULL)
        is KProperty<*> -> "$PROPERTY {}" to arrayOf(subject.name)
        is KFunction<*> -> "$FUNCTION {}" to arrayOf(subject.name)
        is Function<*> -> kotlin.runCatching { subject.reflect() }.getOrNull()
            ?.let { displayNameFallback(it) }
            ?: ("{}" to arrayOf(subject.renderType()))

        is KClass<*> -> "{}" to arrayOf(subject.simpleName?.let { "$CLASS $it" } ?: "$OBJECT $subject")
        is Triple<*, *, *> -> "( {}, {}, {} )" to arrayOf(
            displayNameFor(subject.first),
            displayNameFor(subject.second),
            displayNameFor(subject.third),
        )

        is Pair<*, *> -> "( {}, {} )" to arrayOf(
            displayNameFor(subject.first),
            displayNameFor(subject.second),
        )

        is Map.Entry<*, *> -> "{} → {}" to arrayOf(displayNameFor(subject.key), displayNameFor(subject.value))
        is Char -> "{} {}" to arrayOf(subject.quoted, subject.describe())
        is CharSequence -> when (subject.toString().let { it.codePointCount(0, it.length) }) {
            1 -> "{} {}" to arrayOf(subject.quoted, subject.describe())
            else -> "{}" to arrayOf(subject.quoted)
        }

        else -> "{}" to arrayOf(subject.toCompactString())
    }

    private fun Char.describe(): String = toString().describe()
    private fun CharSequence.describe(): String =
        codePoints().asSequence().map { codePoint ->
            when (val name: String? = Character.getName(codePoint)) {
                null -> "0x${Integer.toHexString(codePoint).uppercase()}"
                else -> name
            }
        }.joinToString(separator = ", ")

    /** Returns an object with its [Any.toString] returning this string in order to protect it from being quoted (again). */
    private val CharSequence.protected: Any
        get() = object {
            override fun toString(): String = this@protected.toString()
        }

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    public fun <T, R> String.property(fn: T.() -> R): String = when (fn) {
        is KProperty<*> -> "$this $VALUE_OF $PROPERTY ${fn.name}"
        is KFunction<*> -> "$this $RETURN $VALUE_OF ${fn.name}"
        is KCallable<*> -> KommonsTest.locateCall().run { "$this $VALUE_OF ${fn.getPropertyName(methodName)}" }
        else -> "$this " + KommonsTest.locateCall().run {
            getLambdaBodyOrNull(this, methodName)?.let { " ❴ $it ❵ " } ?: fn.toCompactString()
        }
    }

    /**
     * Returns the display name for an [subject] asserting test.
     */
    public fun <T> StackTraceElement.assertingDisplayName(subject: T, assertions: Assertions<T>): String =
        buildString {
            append("❕ ")
            append(displayNameFor(subject))
            append(" ")
            append(this@assertingDisplayName.displayName(assertions))
        }

    /**
     * Returns the display name for a transforming test.
     */
    public fun <T, R> StackTraceElement.expectingDisplayName(transform: (T) -> R): String =
        this.displayName("❔", transform)

    /**
     * Returns the display name for a catching test.
     */
    public fun <T, R> StackTraceElement.catchingDisplayName(transform: (T) -> R): String =
        this.displayName("❓", transform)

    /**
     * Returns the display name for an [exceptionType] throwing test.
     */
    public fun throwingDisplayName(exceptionType: KClass<out Throwable>): String =
        buildString {
            append("❗")
            append(" ")
            append(exceptionType.simpleName)
        }

    /**
     * Returns the display name for a test applying [transform].
     */
    private fun <T, R> StackTraceElement.displayName(symbol: String, transform: (T) -> R): String =
        buildString {
            append(symbol)
            append(" ")
            append(this@displayName.displayName(transform))
            getLambdaBodyOrNull(this@displayName, "that", "it")?.also {
                append(" ")
                append(it)
            }
        }

    /**
     * Returns the display name for a test involving the subject returned by [provideSubject].
     */
    public fun <R> StackTraceElement.expectingDisplayName(provideSubject: () -> R): String =
        displayName("❔", provideSubject)

    /**
     * Returns the display name for a test involving an eventually thrown exception
     * by [provideSubject].
     */
    public fun <R> StackTraceElement.catchingDisplayName(provideSubject: () -> R): String =
        displayName("❓", provideSubject)

    /**
     * Returns the display name for a test involving the subject returned by [provide].
     */
    private fun <R> StackTraceElement.displayName(symbol: String, provide: () -> R): String =
        buildString {
            append(symbol)
            append(" ")
            append(displayNameFor(this@displayName.displayName(provide, null).protected))
            getLambdaBodyOrNull(this@displayName, "that", "it")?.also {
                append(" ")
                append(it)
            }
        }

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    private fun <T, R> StackTraceElement.displayName(fn: T.() -> R, fnName: String? = null): String {
        return when (fn) {
            is KProperty<*> -> fn.name
            is KFunction<*> -> fn.name
            is KCallable<*> -> run { fn.getPropertyName(methodName) }
            else -> fnName?.let { getLambdaBodyOrNull(this, it) } ?: getLambdaBodyOrNull(this) ?: fn.toCompactString()
        }
    }

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    private fun <R> StackTraceElement.displayName(fn: () -> R, fnName: String? = null): String {
        return when (fn) {
            is KProperty<*> -> fn.name
            is KFunction<*> -> fn.name
            is KCallable<*> -> run { fn.getPropertyName(methodName) }
            else -> fnName?.let { getLambdaBodyOrNull(this, it) } ?: getLambdaBodyOrNull(this) ?: fn.toCompactString()
        }
    }

    private fun KCallable<*>.getPropertyName(callerMethodName: String): String =
        "^$callerMethodName(?<arg>.+)$".toRegex().find(name)?.destructured?.let { (arg) -> arg.decapitalize() } ?: name

    private fun getLambdaBodyOrNull(
        stackTraceElement: StackTraceElement,
        vararg methodNameHints: String,
    ) = LambdaBody.parseOrNull(stackTraceElement, *methodNameHints)?.removePrefix("it.")?.toString()
}


internal fun Any?.toCompactString(): String {
    return when (this) {
        is Path -> toUri().toString()
        is ByteArray -> "0x" + joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        is Array<*> -> toList().toCompactString()
        is Iterable<*> -> joinToString(prefix = "[", postfix = "]") { it.toCompactString() }
        is Process -> also { waitFor() }.exitValue().toString()
        is CharSequence -> split(*LineSeparators.Unicode).joinToString(separator = "⏎").removeSuffix("⏎").quoted
        else -> when (this) {
            null -> "null"
            Unit -> ""
            else -> {
                val string = toCustomStringOrNull() ?: renderType(simplified = true)
                string.split(*LineSeparators.Unicode).joinToString(separator = "⏎").removeSuffix("⏎")
            }
        }
    }
}

private fun Any.toDefaultString() =
    javaClass.name + "@" + Integer.toHexString(hashCode())

private fun Any?.toCustomStringOrNull(): String? =
    if (this == null) null else toString().takeUnless { it == toDefaultString() }


internal fun Throwable?.toCompactString(): String {
    if (this == null) return ""
    val messagePart = message?.let { ": " + it.lines()[0] } ?: ""
    return rootCause.run {
        this::class.simpleName + messagePart + stackTrace?.firstOrNull()
            ?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

internal fun Result<*>?.toCompactString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toCompactString()
    else exceptionOrNull().toCompactString()
}
