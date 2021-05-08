package koodies.jvm

import koodies.logging.InMemoryLogger
import koodies.test.Slow
import koodies.time.sleep
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.measureTime

@Isolated // time critical
class ThreadsKtTest {

    @Nested
    inner class BusyThreadTest {

        @Slow @Test
        fun `should not complete until asked`(tracer: InMemoryLogger) {
            val thread = BusyThread(tracer)

            val start = System.currentTimeMillis()
            while (thread.isAlive) {
                if (System.currentTimeMillis() - start > 2000) {
                    thread.complete()
                }
            }

            expectThat(System.currentTimeMillis() - start).isGreaterThan(2000)
        }
    }


    @Nested
    inner class CompletableFutureTest {

        private val executor = Executors.newCachedThreadPool()

        @TestFactory
        fun `should start immediately`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture { finished.set(true) } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture { finished.set(true) } },
        ).map { (caption, exec) ->
            dynamicTest(caption) {
                val finished = AtomicBoolean(false)
                measureTime {
                    exec(finished)
                    while (!finished.get()) {
                        Duration.milliseconds(1).sleep()
                    }
                }.let { expectThat(it).isLessThan(Duration.milliseconds(80)) }
            }
        }

        @TestFactory
        fun `should start delayed`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture(Duration.milliseconds(500)) { finished.set(true) } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture(Duration.milliseconds(500)) { finished.set(true) } },
        ).map { (caption, exec) ->
            dynamicTest(caption) {
                val finished = AtomicBoolean(false)
                measureTime {
                    exec(finished)
                    while (!finished.get()) {
                        Duration.milliseconds(1).sleep()
                    }
                }.let { expectThat(it).isGreaterThan(Duration.milliseconds(400)).isLessThan(Duration.milliseconds(600)) }
            }
        }

        @TestFactory
        fun `should block until value is returned`() = listOf(
            "without explicit executor" to { finished: AtomicBoolean -> completableFuture { finished.set(true); "Hello World" } },
            "with explicit executor" to { finished: AtomicBoolean -> executor.completableFuture { finished.set(true); "Hello World" } },
        ).map { (caption: String, exec: (AtomicBoolean) -> CompletableFuture<String>) ->
            dynamicTest(caption) {
                val finished = AtomicBoolean(false)
                val value = exec(finished).get()
                expectThat(value).isEqualTo("Hello World")
            }
        }
    }
}

public inline fun <reified T> Assertion.Builder<out CompletableFuture<out T>>.wait(): DescribeableBuilder<Result<T>> =
    get { runCatching { get() } }
