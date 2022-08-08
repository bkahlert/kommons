package com.bkahlert.kommons.test

import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeAbsolute
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class KommonsTestTest {

    @Test
    fun locations() = testAll(
        KommonsTest.Work,
        KommonsTest.Home,
        KommonsTest.Temp,
        KommonsTest.JavaHome,
    ) {
        it.shouldBeAbsolute()
        it.shouldExist()
        it.shouldBeADirectory()
    }

    @Test fun locate_call() = testAll {
        KommonsTest.locateCall() should {
            it.fileName shouldBe "KommonsTestTest.kt"
            it.className shouldBe KommonsTestTest::class.qualifiedName
            it.methodName shouldBe "locate_call"
            it.lineNumber shouldBe 26
        }

        KommonsTest.locateCall(StackTrace(StackTrace.get().dropWhile { !it.className.startsWith("org.junit") })) should {
            it.fileName shouldNotBe "KommonsTestTest.kt"
        }

        KommonsTest.locateCall(RuntimeException()) should {
            it.fileName shouldBe "KommonsTestTest.kt"
            it.className shouldBe KommonsTestTest::class.qualifiedName
            it.methodName shouldBe "locate_call"
            it.lineNumber shouldBe 37
        }
    }
}
