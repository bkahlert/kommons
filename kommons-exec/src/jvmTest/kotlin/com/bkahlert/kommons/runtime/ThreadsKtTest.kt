package com.bkahlert.kommons.runtime

import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.time.sleep
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@Isolated // time critical
class ThreadsKtTest {

    @Nested
    inner class BusyThreadTest {

        @Slow @Test
        fun `should not complete until asked`() {
            val thread = BusyThread()

            val start = System.currentTimeMillis()
            while (thread.isAlive) {
                if (System.currentTimeMillis() - start > 2000) {
                    thread.complete()
                    break
                }
            }
            while (thread.isAlive) {
                // wait for thread to finish
            }

            expectThat(thread.isAlive).isFalse()
            expectThat(System.currentTimeMillis() - start)
                .isGreaterThan(2000)
                .isLessThan(3000)
        }
    }


    @Nested
    inner class CompletableFutureTest {

        private val executor = Executors.newCachedThreadPool()

        @TestFactory
        fun `should start immediately`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture { finished.set(true) } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture { finished.set(true) } },
        ).map { (name, exec) ->
            dynamicTest(name) {
                val finished = AtomicBoolean(false)
                measureTime {
                    exec(finished)
                    while (!finished.get()) {
                        1.milliseconds.sleep()
                    }
                }.let { expectThat(it).isLessThan(80.milliseconds) }
            }
        }

        @TestFactory
        fun `should start delayed`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture(0.5.seconds) { finished.set(true) } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture(0.5.seconds) { finished.set(true) } },
        ).map { (name, exec) ->
            dynamicTest(name) {
                val finished = AtomicBoolean(false)
                measureTime {
                    exec(finished)
                    while (!finished.get()) {
                        1.milliseconds.sleep()
                    }
                }.let { expectThat(it).isGreaterThan(400.milliseconds).isLessThan(600.milliseconds) }
            }
        }

        @TestFactory
        fun `should block until value is returned`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture { finished.set(true); "Hello World" } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture { finished.set(true); "Hello World" } },
        ).map { (name: String, exec: (AtomicBoolean) -> CompletableFuture<String>) ->
            dynamicTest(name) {
                val finished = AtomicBoolean(false)
                val value = exec(finished).get()
                expectThat(value).isEqualTo("Hello World")
            }
        }
    }
}

inline fun <reified T> Assertion.Builder<out CompletableFuture<out T>>.wait(): DescribeableBuilder<Result<T>> =
    get { runCatching { get() } }
