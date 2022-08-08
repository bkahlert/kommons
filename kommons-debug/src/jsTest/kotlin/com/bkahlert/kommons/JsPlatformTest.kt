package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.AnsiSupport
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JsPlatformTest {

    @Test fun current() = testAll {
        Platform.Current.shouldBeOneOf(Platform.Browser, Platform.NodeJS)
    }

    @Test fun ansi_support() = testAll {
        Platform.Current.ansiSupport shouldBe AnsiSupport.NONE
    }

    @Test fun file_separator() = testAll {
        Platform.Current.fileSeparator.shouldBeOneOf("\\", "/")
    }
}
