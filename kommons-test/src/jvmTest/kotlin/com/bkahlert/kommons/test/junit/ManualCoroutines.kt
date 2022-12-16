package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.test.junit.xxx.iterator2
import io.kotest.matchers.longs.shouldBeEven
import io.kotest.mpp.timeInMillis
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import kotlin.random.Random
import kotlin.streams.asStream
import kotlin.time.Duration.Companion.milliseconds

val logger by SLF4J

class Coroutines {
    @Test
    fun testCoroutine2() {
        val blockIterator = build2 {
            foo()
            bar()
            foo()
            foo()
            bar()
        }

        blockIterator.forEach {
            it.execute()
        }
    }

    fun tests() = build2 {
        foo()
        bar()
        foo()
        foo()
        bar()
    }.asSequence().asStream()

    @TestFactory
    fun testCoroutineSequential() = tests()

    @Execution(CONCURRENT)
    @TestFactory
    fun testCoroutineConcurrent() = tests()
}

fun build2(block: suspend Builder.() -> Unit): Iterator<DynamicTest> {

    var completed = false

    return iterator2<DynamicTest> {
        Builder { yield(it) }.block()
    }
}

val random = Random(timeInMillis())

fun DynamicTest(
    name: String,
): DynamicTest {
    logger.info("Creating test $name")

    return DynamicTest.dynamicTest(name) {
        val timeout = 500 + random.nextLong(500)
        logger.info("Executing test $name in ${timeout.milliseconds}")
        Thread.sleep(timeout)
        timeout.shouldBeEven()
        logger.info("Finished test $name")
    }
}


class Builder(
    val callback: suspend (DynamicTest) -> Unit,
) {

    suspend fun foo(): Unit {
        val block = DynamicTest("foo")
        callback(block)
    }

    suspend fun bar(): Unit {
        val block = DynamicTest("bar")
        callback(block)
    }
}
