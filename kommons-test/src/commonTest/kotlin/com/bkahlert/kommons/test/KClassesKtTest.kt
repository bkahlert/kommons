package com.bkahlert.kommons.test

import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeBlank
import kotlin.test.Test

class KClassesKtTest {

    @Test fun best_name() = testAll {
        val result = String::class.bestName()
        result shouldEndWith "String"
    }

    @Test fun best_name_of_anonymous_object() = testAll {
        val result = (object {})::class.bestName()
        result.shouldNotBeBlank()
    }
}
