@file:Suppress("DEPRECATION")

package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldMatch
import kotlin.test.Test

class InspectJsKtTest {

    @Test fun inspect_js() = testAll {
        buildString {
            "subject".inspectJs(out = this::append)
        } should {
            it shouldMatch "subject".toRegex()
        }
    }
}