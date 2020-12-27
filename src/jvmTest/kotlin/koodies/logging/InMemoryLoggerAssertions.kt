package koodies.logging

import koodies.test.matchesCurlyPattern
import strikt.api.Assertion


fun Assertion.Builder<InMemoryLogger>.matches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = true,
) = get("logged content") {
    finalizedDump(Result.success(Unit))
}.matchesCurlyPattern(curlyPattern, removeTrailingBreak, removeEscapeSequences, trimmed)
