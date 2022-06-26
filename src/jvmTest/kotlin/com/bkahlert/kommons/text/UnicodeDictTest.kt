package com.bkahlert.kommons.text

import com.bkahlert.kommons.CodePoint
import com.bkahlert.kommons.test.test
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UnicodeDictTest {

    @Test fun parse() = test {
        UnicodeDict should {
            it[0x0] shouldBe "NULL CHARACTER"
            it[0x2007] shouldBe "FIGURE SPACE"
            it[0xE01EF] shouldBe "VARIATION SELECTOR-256"
        }
    }

    @Test fun default() = test {
        UnicodeDict should {
            it[0x2FA1E] shouldBe "\\u2fa1e!!OTHER_LETTER"
        }
    }

    @Test fun unicode_name() = test {
        CodePoint(0x0).unicodeName shouldBe "NULL CHARACTER"
        CodePoint(0x2007).unicodeName shouldBe "FIGURE SPACE"
        CodePoint(0xE01EF).unicodeName shouldBe "VARIATION SELECTOR-256"
    }

    @Test fun formatted_name() = test {
        CodePoint(0x0).formattedName shouldBe "❲NULL CHARACTER❳"
        CodePoint(0x2007).formattedName shouldBe "❲FIGURE SPACE❳"
        CodePoint(0xE01EF).formattedName shouldBe "❲VARIATION SELECTOR-256❳"
    }
}
