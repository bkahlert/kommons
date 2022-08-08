package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.AnsiSupport
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import kotlin.test.Test

class JvmPlatformTest {

    @Test fun current() = testAll {
        Platform.Current shouldBe Platform.JVM
    }

    @Test fun ansi_support() = testAll {
        if (Program.isIntelliJ) Platform.Current.ansiSupport shouldNotBe AnsiSupport.NONE
        else Platform.Current.ansiSupport shouldNotBe AnsiSupport.ANSI24
    }

    @Test fun file_separator() = testAll {
        Platform.Current.fileSeparator shouldBe File.separator
    }
}
