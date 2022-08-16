package com.bkahlert.logging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SLF4JTest {

    @Test fun format() = tests {
        SLF4J.format("A {} C {} E", "B", "D") shouldBe "A B C D E"
        SLF4J.format("A {} C {} E", "B", "D", "Z") shouldBe "A B C D E"
        SLF4J.format("A {} C {} E", "B") shouldBe "A B C {1} E"
    }

    // TODO test in combination with sturcturd arguments
}
