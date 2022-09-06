package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ExceptionsKtTest {

    @Test fun root_cause() = testAll(
        IllegalArgumentException("error message"),
        IllegalStateException(IllegalArgumentException("error message")),
        RuntimeException(IllegalStateException(IllegalArgumentException("error message"))),
        Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message")))),
        RuntimeException(Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message"))))),
    ) { ex: Throwable ->
        ex.rootCause should {
            it.shouldBeInstanceOf<IllegalArgumentException>()
            it.message shouldBe "error message"
        }
    }

    @Test fun causes() = testAll {
        val cause1 = IllegalArgumentException("error message")
        val cause2 = IllegalStateException(cause1)
        val cause3 = RuntimeException(cause2)
        val cause4 = Error(cause3)
        val cause5 = RuntimeException(cause4)
        cause1.causes.shouldBeEmpty()
        cause2.causes.shouldContainExactly(cause1)
        cause3.causes.shouldContainExactly(cause2, cause1)
        cause4.causes.shouldContainExactly(cause3, cause2, cause1)
        cause5.causes.shouldContainExactly(cause4, cause3, cause2, cause1)
    }
}
