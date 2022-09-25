package com.bkahlert.kommons.logging

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SLF4JTest {

    @Test fun get_logger() = testAll {
        SLF4J.getLogger("test") shouldBe LoggerFactory.getILoggerFactory().getLogger("test")
    }

    @Test fun format() = testAll {
        SLF4J.format("A {} C {} E", "B", "D") shouldBe "A B C D E"
        SLF4J.format("A {} C {} E", "B", "D", "Z") shouldBe "A B C D E"
        SLF4J.format("A {} C {} E", "B") shouldBe "A B C {1} E"
    }
}
