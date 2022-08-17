package com.bkahlert.logging

import java.text.MessageFormat
import java.util.regex.Pattern

public object StructuredLogging {
    /**
     * Formats the given SL4JF formatted message using [MessageFormat].
     * This approach is especially useful for exception messages.
     *
     * <dl>
     * <dt>**Example Part 1: Formatting a log message using SLF4j**</dt>
     * <dd>`log.info("Computation {} succeeded"`, "foo"}.</dd>
     * <dt>**Example Part 2: Formatting a log message for an exception**</dt>
     * <dd>
     * <dl>
     * <dt>Using manual concatenation</dt>
     * <dd>`new RuntimeException("Computation " + computationName + " failed`</dd>
     * <dt>Using [MessageFormat]</dt>
     * <dd>`new RuntimeException(Message.format("Computation {0} failed", computationName))`</dd>
     * <dt>Using StructuredLogging</dt>
     * <dd>`new RuntimeException(StructuredLogging.formatForException("Computation {} failed", computationName))`</dd>
    </dl> *
    </dd> *
     * <dt>**Example Part 3: Explanation**</dt>
     * <dd>Using this method allows you to only have one log message format. Because of that
     * you can organize and store predefined messages centrally and use them for both SLF4j messages and exception messages.</dd>
    </dl> *
     *
     * @param slf4jLogMessage the message originally intended for SL4J logging
     * @param args            the args to be used for the message placeholders
     *
     * @return the fully formatted string for an exception's message
     */
    public fun formatForException(slf4jLogMessage: String, vararg args: Any?): String {
        var messageFormatPattern = slf4jLogMessage
        var index = 0
        val slf4jAnchorPattern = Pattern.compile(Pattern.quote(SLF4J_ANCHOR))
        var matcher = slf4jAnchorPattern.matcher(messageFormatPattern)
        while (matcher.find()) {
            messageFormatPattern = matcher.replaceFirst(String.format(MESSAGE_FORMAT_REPLACEMENT, index))
            matcher = slf4jAnchorPattern.matcher(messageFormatPattern)
            index++
        }
        val messageFormat = MessageFormat(messageFormatPattern)
        return messageFormat.format(args, StringBuffer(slf4jLogMessage.length shl 1), null).toString()
    }

    private const val SLF4J_ANCHOR = "{}"
    private const val MESSAGE_FORMAT_REPLACEMENT = "{%d}"
}


/**
 * Serializes this object and wraps it with double quotes.
 *
 * `null` is serialized as `␀`.
 */
@Suppress("GrazieInspection")
public inline val Any?.quoted: String
    get() = """"${this ?: "␀"}""""
