package com.bkahlert.kommons.test

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class KommonsTestTest {

    @Test fun locate_call() = testAll {
        KommonsTest.locateCall() should {
            it.fileName shouldBe "KommonsTestTest.kt"
            it.className shouldBe KommonsTestTest::class.qualifiedName
            it.methodName shouldBe "locate_call"
            it.lineNumber shouldBe 11
        }

        KommonsTest.locateCall(StackTrace(StackTrace.get().dropWhile { !it.className.startsWith("org.junit") })) should {
            it.fileName shouldNotBe "KommonsTestTest.kt"
        }

        KommonsTest.locateCall(RuntimeException()) should {
            it.fileName shouldBe "KommonsTestTest.kt"
            it.className shouldBe KommonsTestTest::class.qualifiedName
            it.methodName shouldBe "locate_call"
            it.lineNumber shouldBe 22
        }
    }
}
