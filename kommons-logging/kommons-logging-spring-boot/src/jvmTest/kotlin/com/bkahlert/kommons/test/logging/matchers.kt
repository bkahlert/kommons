package com.bkahlert.kommons.test.logging

import com.bkahlert.kommons.logging.logback.StructuredArguments
import com.bkahlert.kommons.randomString
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldMatch
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import java.nio.file.Path
import kotlin.io.path.readText


/** Returns a [PrintedLog] instance for the contents of this log file. */
val Path.allLogs: PrintedLog get() = PrintedLog(readText())

/** Returns the last [PrintedLogEntry] contained in this log file, or throws an exception otherwise. */
val Path.lastLog: PrintedLogEntry get() = allLogs.last()


/** Returns a [PrintedLog] instance for the captured [System.out] and [System.err]. */
val CapturedOutput.allLogs: PrintedLog get() = PrintedLog(all)

/** Returns a [PrintedLog] instance for the captured [System.out]. */
val CapturedOutput.outLogs: PrintedLog get() = PrintedLog(out)

/** Returns a [PrintedLog] instance for the captured [System.err]. */
val CapturedOutput.errLogs: PrintedLog get() = PrintedLog(err)


/** Returns the last [PrintedLogEntry] contained in the captured [System.out] and [System.err], or throws an exception otherwise. */
val CapturedOutput.lastLog: PrintedLogEntry get() = allLogs.last()

/** Returns the last [PrintedLogEntry] contained in the captured [System.out], or throws an exception otherwise. */
val CapturedOutput.lastOutLog: PrintedLogEntry get() = outLogs.last()

/** Returns the last [PrintedLogEntry] contained in the captured [System.err], or throws an exception otherwise. */
val CapturedOutput.lastErrLog: PrintedLogEntry get() = errLogs.last()


infix fun PrintedLogEntry?.shouldContain(substr: String): PrintedLogEntry? {
    this?.toString() should contain(substr)
    return this
}

internal fun logInfo(vararg args: Any?, message: String = "message") =
    LoggerFactory.getLogger("TestLogger").info("$message with {}", StructuredArguments.v("key", "value"), *args)

internal fun logInfoWithException(vararg args: Any?, message: String = "message") =
    logInfo(*args, RuntimeException("message"), message = message)

internal fun logRandomInfo(): String =
    randomString(64).also { logInfo(message = it) }

internal fun logRandomInfoWithException(): String =
    randomString(64).also { logInfoWithException(message = it) }


fun PrintedLogEntry.shouldMatchMinimalPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{2}:\d{2}.\d{3} {2}INFO TestLogger {21}: $message with value
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchCustomMinimalPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{2}:\d{2}.\d{3} I TestLogger + : $message with value
    java\.lang\.RuntimeException: message
    \tat .*
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}.\d{3} {2}INFO +\d* +--- \[.*] TestLogger + : $message with value
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchCustomSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2} I +\d* +--- \[.*] TestLogger + : $message with value
    java\.lang\.RuntimeException: message
    \tat .*
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchJsonPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    @Suppress("LongLine")
    """
    \{"@timestamp":"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?(?:[+-]\d{2}:\d{2}|Z)","level":"INFO","thread_name":".+","logger_name":"TestLogger","message":"$message with value","key":"value","foo":"bar"}
    """.trimIndent()
)
