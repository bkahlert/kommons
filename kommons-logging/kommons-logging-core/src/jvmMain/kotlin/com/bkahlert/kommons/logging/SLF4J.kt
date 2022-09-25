package com.bkahlert.kommons.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.regex.Pattern
import kotlin.reflect.KProperty

/**
 * [SLF4J](https://www.slf4j.org/) [ILoggerFactory] singleton that
 * serves as a quasi companion object.
 *
 * *Can be removed the moment [static extension functions](https://youtrack.jetbrains.com/issue/KT-11968) are possible.*
 */
public object SLF4J {

    private const val SLF4J_ANCHOR = "{}"
    private val SLF4J_PATTERN = Pattern.compile(Pattern.quote(SLF4J_ANCHOR))
    private const val MESSAGE_FORMAT_REPLACEMENT = "{%d}"

    /** Uses [SLF4J]'s [ILoggerFactory] to get a logger with the specified [name]. */
    public fun getLogger(name: String): Logger =
        LoggerFactory.getILoggerFactory().getLogger(name)

    /**
     * Returns a [Lazy] logger property of which the name is derived from
     * the owning class,
     * respectively the companion object's owning class, or if missing,
     * the file class.
     *
     * Uses [SLF4J]'s [ILoggerFactory] to get the logger.
     */
    public operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<Logger> =
        LoggerFactory.getILoggerFactory().provideDelegate(thisRef, property)

    /**
     * Formats the specified [message] by replacing `{}` placeholders with the
     * specified [args].
     */
    public fun format(message: String, vararg args: Any?): String {
        var messageFormatPattern = message
        var index = 0

        var matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
        while (matcher.find()) {
            messageFormatPattern = matcher.replaceFirst(String.format(MESSAGE_FORMAT_REPLACEMENT, index))
            matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
            index++
        }
        val messageFormat = MessageFormat(messageFormatPattern)
        return messageFormat.format(args, StringBuffer(message.length shl 1), null).toString()
    }
}

/**
 * Returns a [Lazy] logger property of which the name is derived from
 * the owning class,
 * respectively the companion object's owning class, or if missing,
 * the file class.
 *
 * Uses [SLF4J]'s [ILoggerFactory] to get the logger.
 */
public operator fun ILoggerFactory.provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<Logger> {
    val name = thisRef.loggerName(::provideDelegate)
    return lazy { getLogger(name) }
}
