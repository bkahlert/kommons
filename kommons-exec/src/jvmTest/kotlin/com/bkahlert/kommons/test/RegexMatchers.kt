package com.bkahlert.kommons.test

import com.bkahlert.kommons.debug.render
import strikt.api.Assertion
import strikt.assertions.isNotNull

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence): Assertion.Builder<MatchResult> =
    get("match entirely ${input.render()}") { matchEntire(input) }.isNotNull()

fun Assertion.Builder<MatchResult>.group(groupName: String) =
    get("group with name $groupName: %s") { (groups as MatchNamedGroupCollection)[groupName] }

fun Assertion.Builder<MatchResult>.group(index: Int) =
    get("group with index $index: %s") { groups[index] }

val Assertion.Builder<MatchResult>.groupValues
    get() = get("group values: %s") { groupValues }

val Assertion.Builder<MatchGroup?>.value
    get() = get("value %s") { this?.value }
