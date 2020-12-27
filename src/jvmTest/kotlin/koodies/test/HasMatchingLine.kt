package koodies.test

import koodies.io.file.readLines
import strikt.api.Assertion
import strikt.assertions.any
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.hasMatchingLine(curlyPattern: String) =
    get("lines") { readLines() }.any { matchesCurlyPattern(curlyPattern) }
