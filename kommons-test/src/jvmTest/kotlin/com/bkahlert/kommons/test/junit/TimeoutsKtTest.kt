package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class TimeoutsKtTest {

    @Test fun slow() = testAll {
        Slow::class.annotations.forAny {
            (it as? Tag)?.value shouldBe "slow"
        }
    }
}
