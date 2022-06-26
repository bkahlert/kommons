package com.bkahlert.kommons.time

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class SleepingKtTest {

    @Nested
    @Execution(CONCURRENT)
    inner class SleepKtTest {

        @Test
        fun `should sleep on positive duration`() {
            expectThat(measureTime { 1.seconds.sleep() }).isGreaterThan(0.9.seconds).isLessThan(1.1.seconds)
        }

        @Test
        fun `should sleep and call block on positive duration`() {
            expectThat(1.seconds.sleep { 42 }).isEqualTo(42)
        }

        @Test
        fun `should not sleep on zero duration`() {
            expectThat(measureTime { Duration.ZERO.sleep() }).isLessThanOrEqualTo(0.1.seconds)
        }

        @Test
        fun `should not sleep and call block on zero duration`() {
            expectThat(Duration.ZERO.sleep { 42 }).isEqualTo(42)
        }

        @Test
        fun `should throw on negative duration`() {
            expectCatching { measureTime { (-1).seconds.sleep() } }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw and not run block on negative duration`() {
            expectCatching { (-1).seconds.sleep { throw RuntimeException("error") } }.isFailure().isA<IllegalArgumentException>()
        }
    }
}
