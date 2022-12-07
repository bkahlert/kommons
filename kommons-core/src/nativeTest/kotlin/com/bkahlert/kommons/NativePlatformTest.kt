package com.bkahlert.kommons

import com.bkahlert.kommons.Platform.Native
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import com.bkahlert.kommons.Platform as KommonsPlatform

class NativePlatformTest {

    @Test fun current() = testAll {
        KommonsPlatform.Current shouldBe Native
    }

    @Test fun ansi_support() = testAll {
        KommonsPlatform.Current.ansiSupport shouldBe AnsiSupport.ANSI24
    }

    @Test fun file_separator() = testAll {
        KommonsPlatform.Current.fileSeparator shouldBe "/"
    }
}
