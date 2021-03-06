package koodies.logging

import java.text.MessageFormat
import java.util.regex.Pattern

public object SLF4J {

    private const val SLF4J_ANCHOR = "{}"
    private val SLF4J_PATTERN = Pattern.compile(Pattern.quote(SLF4J_ANCHOR))
    private const val MESSAGE_FORMAT_REPLACEMENT = "{%d}"

    /**
     * `slf4jFormat("This is a log {} with {} {}", "message", "some", "parameters") == "This is a log message with some parameters"`
     */
    public fun format(slf4jLogMessage: String, vararg args: Any?): String {
        var messageFormatPattern = slf4jLogMessage
        var index = 0

        var matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
        while (matcher.find()) {
            messageFormatPattern = matcher.replaceFirst(String.format(MESSAGE_FORMAT_REPLACEMENT, index))
            matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
            index++
        }
        val messageFormat = MessageFormat(messageFormatPattern)
        return messageFormat.format(args, StringBuffer(slf4jLogMessage.length shl 1), null).toString()
    }
}
