package com.bkahlert.kommons.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JvmKClassesKtTest {

    @Test fun best_name() = testAll {
        val result = String::class.bestName()
        result shouldBe "kotlin.String"
    }

    @Test fun best_name_of_anonymous_object() = testAll {
        val anonymous = object {}
        val result = anonymous::class.bestName()
        result shouldBe anonymous.javaClass.name
    }
}
