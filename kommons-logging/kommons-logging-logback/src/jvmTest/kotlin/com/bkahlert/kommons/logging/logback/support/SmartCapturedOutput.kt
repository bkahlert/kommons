package com.bkahlert.kommons.logging.logback.support

import com.bkahlert.kommons.logging.logback.support.SmartCaptureSupport.getLine
import com.bkahlert.kommons.logging.logback.support.SmartCaptureSupport.toJson
import org.assertj.core.api.MapAssert
import org.springframework.boot.test.json.JsonContent
import org.springframework.boot.test.system.CapturedOutput

/**
 * Extension of Spring's [CapturedOutput] that provides methods
 * to access certain lines of the captures output.
 *
 *
 * Further features are accessing outputted lines starting from the most recent log line
 * using negative indices, and returning the captures output as parsed JSON
 * as well as a fully set up matchers that asserts on that.
 *
 * @author Björn Kahlert
 * @see SmartCapturedLog
 *
 * @see SmartOutputCaptureExtension
 */
interface SmartCapturedOutput : CapturedOutput {
    /**
     * Returns the n-th line of the captured [System.out].
     *
     * @param n index of the line to convert
     *
     * @return n-th [System.out] captured output
     */
    fun getOut(n: Int): String = getLine(n, out)

    /**
     * Returns a testable JSON object that comprises the most recent output line, e.g.<br></br>
     * Given an output like:
     * <pre>`2020-03-17 04:47:42.876 INFO message 1
     * 2020-03-17 04:47:42.878 INFO message 2
     * 2020-03-17 04:47:42.880 INFO message 3
    `</pre> *
     * this method will return a JSON tester for `2020-03-17 04:47:42.880 INFO message 3`.
     *
     * @return testable JSON object
     */
    fun toJSON(): JsonContent<Any> = toJson(getOut(-1), javaClass)

    /**
     * Returns a testable JSON object that comprises the n-th output n, e.g.<br></br>
     * Given an output like:
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
    fun toJSON(n: Int): JsonContent<Any> = toJson(getOut(n), javaClass)

    /**
     * Returns an AssertJ assertable map consisting of all key-value pairs the most recent JSON log contained at `path`.
     * Given an output like:
     * <pre>`{"timestamp": "2020-03-17 04:47:42.876","level":"INFO","message":"message 1", "x": { "y": "z1"}}
     * {"timestamp": "2020-03-17 04:47:42.878","level":"INFO","message":"message 2", "x": { "y": "z2"}}
     * {"timestamp": "2020-03-17 04:47:42.880","level":"INFO","message":"message 3", "x": { "y": "z3"}}
    `</pre> *
     * Using `path = "x"` this method will return an assertable map with the entry `{"y": "z3"}`.
     *
     * @param path that describes the node to be returned, use `"$"` for the root
     *
     * @return [MapAssert] testing a ``Map<String></String>, Object>}
     */
    fun assertThatMappedJSON(line: Int = -1, path: String = "$"): MapAssert<String, Any?> {
        return SmartCaptureSupport.toJsonMapAssert(getLine(line, all), path)
    }

    companion object {
        /**
         * Enriches the given captured output as described on the
         * [class level documentation][SmartCapturedOutput].
         *
         * @param output the captured output to be enriched
         *
         * @return the enriched captured output
         */
        fun enrich(output: CapturedOutput): SmartCapturedOutput {
            return object : SmartCapturedOutput {
                override fun getOut(): String = output.out
                override fun getErr(): String = output.all
                override fun getAll(): String = output.all
                override fun toString(): String = all
            }
        }
    }
}
