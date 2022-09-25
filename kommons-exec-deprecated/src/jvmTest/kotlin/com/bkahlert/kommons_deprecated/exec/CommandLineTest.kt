package com.bkahlert.kommons_deprecated.exec

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons_deprecated.time.sleep
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo
import kotlin.time.Duration.Companion.milliseconds

val <T : CharSequence> Assertion.Builder<T>.continuationsRemoved: DescribeableBuilder<String>
    get() = get("continuation removed %s") { replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<Exec>
    get() = get("evaluated %s") { toExec(false, emptyMap(), SystemLocations.Temp, null).process() }

fun Assertion.Builder<CommandLine>.evaluated(block: Assertion.Builder<Exec>.() -> Unit) =
    evaluated.block()

val Assertion.Builder<Exec>.output
    get() = get("output of type IO.Output %s") { io.output.ansiRemoved }

val <P : Exec> Assertion.Builder<P>.exitCodeOrNull
    get() = get("exit value %s") { exitCodeOrNull }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String) {
    with(evaluated) {
        io.output.ansiRemoved.isEqualTo(expectedOutput)
        50.milliseconds.sleep()
    }
}
