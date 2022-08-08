package com.bkahlert.kommons.test

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class RootCauseKtTest {

    @Test fun root_cause() = testAll(
        IllegalArgumentException("error message"),
        IllegalStateException(IllegalArgumentException("error message")),
        RuntimeException(IllegalStateException(IllegalArgumentException("error message"))),
        Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message")))),
        RuntimeException(Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message"))))),
    ) { ex ->
        ex.rootCause should {
            it.shouldBeInstanceOf<IllegalArgumentException>()
            it.message shouldBe "error message"
        }
        ex.shouldHaveRootCauseInstanceOf<IllegalArgumentException>()
        ex.shouldHaveRootCauseMessage("error message")
    }
}
