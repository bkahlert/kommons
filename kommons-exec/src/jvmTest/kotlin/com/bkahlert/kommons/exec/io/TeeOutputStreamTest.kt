package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TeeOutputStreamTest {

    @TestFactory fun write() = testAll {
        buildList {
            TeeOutputStream(testOutputStream(), testOutputStream(), testOutputStream()).write("foo".toByteArray())
        }.forAll {
            it.toString() shouldBe "foo"
        }

        buildList {
            TeeOutputStream(testOutputStream(), testOutputStream(), testOutputStream()).write("foo".toByteArray(), 1, 2)
        }.forAll {
            it.toString() shouldBe "oo"
        }

        buildList {
            TeeOutputStream(testOutputStream(), testOutputStream(), testOutputStream()).write("foo".toByteArray().last().toInt())
        }.forAll {
            it.toString() shouldBe "o"
        }
    }

    @TestFactory fun flush() = testAll {
        buildList {
            TeeOutputStream(testOutputStream(), testOutputStream(), testOutputStream()).flush()
        }.forAll {
            it.shouldBeInstanceOf<TestOutputStream>()
            it.flushed shouldBe true
        }
    }

    @TestFactory fun close() = testAll {
        buildList {
            TeeOutputStream(testOutputStream(), testOutputStream(), testOutputStream()).close()
        }.forAll {
            it.closed shouldBe true
        }
    }

    @Test fun to_string() {
        TeeOutputStream(TestOutputStream(), TestOutputStream(), TestOutputStream())
            .also { it.write("a".toByteArray()) }
            .toString() shouldBe "TeeOutputStream { output: a, branches: [ a, a ] }"
    }
}
