package com.bkahlert.kommons.test

import kotlin.test.Test

class RootCauseKtTest {

    @Test fun root_cause() = testAll(
        IllegalArgumentException("error message"),
        IllegalStateException(IllegalArgumentException("error message")),
        RuntimeException(IllegalStateException(IllegalArgumentException("error message"))),
        Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message")))),
        RuntimeException(Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message"))))),
    ) { ex ->
        ex.shouldHaveRootCauseInstanceOf<IllegalArgumentException>()
        ex.shouldHaveRootCauseMessage("error message")
    }
}
