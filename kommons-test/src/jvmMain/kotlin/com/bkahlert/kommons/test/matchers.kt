package com.bkahlert.kommons.test

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * Verifies that this path starts with the specified [segments].
 *
 * @see shouldNotStartWith
 * @see shouldEndWith
 * @see shouldNotEndWith
 */
public fun Path.shouldStartWith(vararg segments: String): Unit = this should startWith(*segments)

/**
 * Verifies that this path doesn't start with the specified [segments].
 *
 * @see shouldStartWith
 * @see shouldEndWith
 * @see shouldNotEndWith
 */
public fun Path.shouldNotStartWith(vararg segments: String): Unit = this shouldNot startWith(*segments)

/**
 * Returns a matcher that verifies that the path starts with the specified [segments].
 *
 * @see endWith
 */
public fun startWith(vararg segments: String): Matcher<Path> = object : Matcher<Path> {
    override fun test(value: Path): MatcherResult = MatcherResult(
        value.map { it.pathString }.take(segments.size) == segments.asList(),
        { "Path $value should start with $segments" },
        { "Path $value should not start with $segments" })
}


/**
 * Verifies that this path ends with the specified [segments].
 *
 * @see shouldStartWith
 * @see shouldNotStartWith
 * @see shouldNotEndWith
 */
public fun Path.shouldEndWith(vararg segments: String): Unit = this should endWith(*segments)

/**
 * Verifies that this path doesn't end with the specified [segments].
 *
 * @see shouldStartWith
 * @see shouldNotStartWith
 * @see shouldEndWith
 */
public fun Path.shouldNotEndWith(vararg segments: String): Unit = this shouldNot endWith(*segments)

/**
 * Returns a matcher that verifies that the path ends with the specified [segments].
 *
 * @see startWith
 */
public fun endWith(vararg segments: String): Matcher<Path> = object : Matcher<Path> {
    override fun test(value: Path): MatcherResult = MatcherResult(
        value.map { it.pathString }.takeLast(segments.size) == segments.asList(),
        { "Path $value should end with $segments" },
        { "Path $value should not end with $segments" })
}
