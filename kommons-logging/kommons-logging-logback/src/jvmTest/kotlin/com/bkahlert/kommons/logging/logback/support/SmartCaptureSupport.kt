package com.bkahlert.kommons.logging.logback.support

import org.assertj.core.api.MapAssert
import org.assertj.core.api.SoftAssertions
import org.springframework.boot.test.json.BasicJsonTester
import org.springframework.boot.test.json.JsonContent
import org.springframework.boot.test.json.JsonContentAssert

/**
 * Shared functionality for [SmartCapturedOutput] and [SmartCapturedLog].
 *
 * @author Björn Kahlert
 * @see SmartCapturedOutput
 *
 * @see SmartCapturedLog
 */
object SmartCaptureSupport {
    /**
     * Returns the n-th line of the given output.<br></br>
     * Negative indices are supported and counted backwards, that is `n = -1`
     * returns the last line.
     *
     * @param n      index of the line to be returned
     * @param output of what to extract the n-th line from
     *
     * @return n-th line of output
     */
    fun getLine(n: Int, output: String): String {
        val lines = output.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (lines.isEmpty()) return ""
        val normalizedIndex = calcPositiveModulo(n, lines.size)
        return lines[normalizedIndex]
    }

    /**
     * Returns the n-th line of the given output.<br></br>
     * Negative indices are supported and counted backwards, that is `n = -1`
     * returns the last line.
     *
     * @param n      index of the line to be returned
     * @param output of what to extract the n-th line from
     *
     * @return n-th line of output
     */
    fun getLine(n: Int, output: List<String>): String {
        if (output.isEmpty()) return ""
        val normalizedIndex = calcPositiveModulo(n, output.size)
        return output[normalizedIndex]
    }

    /**
     * Returns the positive modulo of n % m.
     *
     *
     * Java allows negative remainders - that's why we implement it here.
     *
     * @param n dividend
     * @param m divisor
     *
     * @return positive remainder (between 0 inclusive and m exclusive)
     */
    private fun calcPositiveModulo(n: Int, m: Int): Int {
        val remainder = n % m
        return if (remainder < 0) remainder + m else remainder
    }

    /**
     * Returns a testable JSON object that comprises the stringified JSON output.
     *
     * @param clazz used to load the
     * @param json  to be converted
     *
     * @return testable JSON object
     */
    fun toJson(json: CharSequence?, clazz: Class<*>?): JsonContent<Any> =
        BasicJsonTester(clazz).from(json)

    /**
     * Returns an AssertJ assertable map consisting of all key-value pairs the stringified JSON log contains at `path`.
     *
     *
     * **Example:** `{"timestamp": "2020-03-17 04:47:42.876", "level":"INFO", "message":"message", "x": { "y": "z" }}`
     * and `path="x"` will result in an assertable map with the entry `{"y": "z"}`.
     *
     * @param softly [SoftAssertions] instance to be used
     * @param clazz  used to load the
     * @param json   to be converted
     * @param path   that describes the node to be returned, use `"$"` for the root
     *
     * @return [MapAssert] testing a ``Map<String></String>, Object>}
     */
    fun toJsonMapAssert(json: CharSequence, path: CharSequence = "$"): MapAssert<String, Any?> =
        JsonContentAssert(Thread.currentThread().javaClass, json).extractingJsonPathMapValue(path)
}
