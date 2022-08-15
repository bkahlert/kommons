package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class AnsiKtTest {

    @Test fun ansi_contained() = testAll {
        ansiLessCharSequence.ansiContained shouldBe false
        ansiLessString.ansiContained shouldBe false
        ansiCsiCharSequence.ansiContained shouldBe true
        ansiCsiString.ansiContained shouldBe true
        ansiOscCharSequence.ansiContained shouldBe true
        ansiOscString.ansiContained shouldBe true
    }

    @Test fun ansi_removed() = testAll {
        ansiLessCharSequence.ansiRemoved shouldBeSameInstanceAs ansiLessCharSequence
        ansiLessString.ansiRemoved shouldBeSameInstanceAs ansiLessString
        ansiCsiCharSequence.ansiRemoved.toString() shouldBe "bold and blue"
        ansiCsiString.ansiRemoved shouldBe "bold and blue"
        ansiOscCharSequence.ansiRemoved.toString() shouldBe "↗ link"
        ansiOscString.ansiRemoved shouldBe "↗ link"
    }
}


/** [String] containing no escape sequences */
internal const val ansiLessString: String = "bold and blue"

/** [CharSequence] containing no escape sequences */
internal val ansiLessCharSequence: CharSequence = StringBuilder().append(ansiLessString)

/** [String] containing CSI (`control sequence intro`) escape sequences */
internal const val ansiCsiString: String = "[1mbold [34mand blue[0m"

/** [CharSequence] containing CSI (`control sequence intro`) escape sequences */
internal val ansiCsiCharSequence: CharSequence = StringBuilder().append(ansiCsiString)

/** [String] containing CSI (`control sequence intro`) escape sequences */
internal const val ansiOscString: String = "[34m↗(B[m ]8;;https://example.com\\link]8;;\\"

/** [CharSequence] containing CSI (`control sequence intro`) escape sequences */
internal val ansiOscCharSequence: CharSequence = StringBuilder().append(ansiOscString)
