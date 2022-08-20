package com.bkahlert.kommons.logging.logback.support

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import com.bkahlert.kommons.logging.logback.Appender
import com.bkahlert.kommons.logging.logback.support.SmartCaptureSupport.getLine
import com.bkahlert.kommons.logging.logback.support.SmartCaptureSupport.toJson
import org.assertj.core.api.MapAssert
import org.assertj.core.api.SoftAssertions
import org.springframework.boot.test.json.JsonContent
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Log file correspondent to [SmartCapturedOutput] which provides access
 * to the currently active log file.
 *
 *
 * Further features are accessing outputted lines starting from the most recent log line
 * using negative indices, and returning the captures output as parsed JSON
 * as well as a fully set up matchers that asserts on that.
 *
 * @author Björn Kahlert
 * @author Dennis Dräger
 * @see SmartCapturedLog
 *
 * @see LogbackConfigurationExtension
 */
@FunctionalInterface
interface SmartCapturedLog : CharSequence {
    val location: String?

    val contentLines: List<String>
        get() = location?.let { activeLogFile: String ->
            try {
                Files.readAllLines(Paths.get(activeLogFile))
            } catch (e: IOException) {
                StatusViaSLF4JLoggerFactory.addInfo("$activeLogFile does not (yet) exist", SmartCapturedLog::class.java)
                emptyList()
            }
        } ?: emptyList()

    val content: String
        get() {
            val content = contentLines.joinToString("\n")
            return if (content.isEmpty()) content else content + "\n"
        }

    /**
     * Returns the n-th line of the log file content in the order that it was captured.
     *
     * @param n index of the n-th line of the log file
     *
     * @return n-th n of the log file
     */
    fun line(n: Int): String = getLine(n, contentLines)

    /**
     * Returns a testable JSON object that comprises the most recent log file entry, e.g.<br></br>
     * Given a log file like:
     * <pre>`2020-03-17 04:47:42.876 INFO message 1
     * 2020-03-17 04:47:42.878 INFO message 2
     * 2020-03-17 04:47:42.880 INFO message 3
    `</pre> *
     * this method will return a JSON tester for `2020-03-17 04:47:42.880 INFO message 3`.
     *
     * @return testable JSON object
     */
    fun toJSON(): JsonContent<Any> = toJson(line(-1), javaClass)

    /**
     * Returns a testable JSON object that comprises the n-th output n, e.g.<br></br>
     * Given a log file like:
     * <pre>`2020-03-17 04:47:42.876 INFO message 1
     * 2020-03-17 04:47:42.878 INFO message 2
     * 2020-03-17 04:47:42.880 INFO message 3
    `</pre> *
     * converting n `1` will return a JSON tester for `2020-03-17 04:47:42.878 INFO message 2`.
     *
     * @param n index of the n to convert
     *
     * @return testable JSON object
     */
    fun toJSON(n: Int): JsonContent<Any> = toJson(line(n), javaClass)

    /**
     * Returns an AssertJ assertable map consisting of all key-value pairs the most recent JSON log contained.
     * Given an output like:
     * <pre>`{"timestamp": "2020-03-17 04:47:42.876","level":"INFO","message":"message 1"}
     * {"timestamp": "2020-03-17 04:47:42.878","level":"INFO","message":"message 2"}
     * {"timestamp": "2020-03-17 04:47:42.880","level":"INFO","message":"message 3"}
    `</pre> *
     * this method will return an assertable map with keys `timestamp`, `level` and `message` ("message 3").
     *
     * @param softly [SoftAssertions] instance to be used
     *
     * @return [MapAssert] testing a ``Map<String></String>, Object>}
     */
    fun assertThatMappedJSON(): MapAssert<String, Any?> =
        SmartCaptureSupport.toJsonMapAssert(getLine(-1, content))

    /**
     * Returns an AssertJ assertable map consisting of all key-value pairs the n-th JSON log contained.
     * Given an output like:
     * <pre>`{"timestamp": "2020-03-17 04:47:42.876","level":"INFO","message":"message 1"}
     * {"timestamp": "2020-03-17 04:47:42.878","level":"INFO","message":"message 2"}
     * {"timestamp": "2020-03-17 04:47:42.880","level":"INFO","message":"message 3"}
    `</pre> *
     * converting n `n = 1` will return an assertable map with keys `timestamp`, `level` and `message` ("message 2").
     *
     * @param softly [SoftAssertions] instance to be used
     * @param n      index of the n to convert
     *
     * @return [MapAssert] testing a ``Map<String></String>, Object>}
     */
    fun assertThatMappedJSON(n: Int): MapAssert<String, Any?> =
        SmartCaptureSupport.toJsonMapAssert(getLine(n, content))

    override val length: Int get() = content.length
    override fun get(index: Int): Char = content[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = content.subSequence(startIndex, endIndex)

    /**
     * A log that gives unfiltered access to what's been logged and is going to be logged.
     */
    class LoggingLog : SmartCapturedLog {
        override val location: String? = Appender.activeLogFileName
        override fun toString(): String = content
    }

    /**
     * A log that gives access to only what's going to be logged.
     */
    class LoggingFromNowLog : SmartCapturedLog {
        override val location: String? = Appender.activeLogFileName
        private val offsetLines: Int = super.contentLines.size
        override val contentLines: List<String> = super.contentLines.drop(offsetLines)
        override fun toString(): String = content
    }

    /**
     * A log that gives access only to what's been already logged.
     */
    class LoggedSoFarLog : SmartCapturedLog {
        override val location: String? = Appender.activeLogFileName
        override val contentLines: List<String> = super.contentLines
        override val content: String = super.content
        override val length: Int = content.length
        override fun get(index: Int): Char = content[index]
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = content.subSequence(startIndex, endIndex)
        override fun toString(): String = content
    }

    companion object {
        /**
         * Returns a live view on the currently written log file.
         *
         *
         * Further changes to the same log file are always reflected.
         * Use [.loggedSoFar] to have a conserved view.
         *
         * @return frozen view on the currently written log file
         */
        fun logging(): SmartCapturedLog {
            return LoggingLog()
        }

        /**
         * Returns a live view on the currently written log file
         * ignoring what has been logged so far..
         *
         *
         * Further changes to the same log file are always reflected.
         * Use [.loggedSoFar] to have a conserved view.
         *
         * @return frozen view on the currently written log file
         */
        fun loggingFromNow(): SmartCapturedLog {
            return LoggingFromNowLog()
        }

        /**
         * Returns a persisted view on currently written log file.
         *
         *
         * Further changes won't be reflected. Use [.logging]
         * to have an always updated view.
         *
         * @return frozen view on the currently written log file
         */
        fun loggedSoFar(): SmartCapturedLog {
            return LoggedSoFarLog()
        }
    }
}
