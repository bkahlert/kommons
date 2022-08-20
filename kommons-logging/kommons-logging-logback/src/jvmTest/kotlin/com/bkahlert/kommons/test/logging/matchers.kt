package com.bkahlert.kommons.test.logging

import io.kotest.matchers.should
import io.kotest.matchers.string.contain
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
