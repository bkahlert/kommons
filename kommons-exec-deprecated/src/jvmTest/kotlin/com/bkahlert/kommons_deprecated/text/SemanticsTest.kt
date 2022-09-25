package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons_deprecated.test.testOld
import com.bkahlert.kommons_deprecated.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons_deprecated.text.Semantics.Symbols
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons_deprecated.text.UnicodeOld.Emojis.VARIATION_SELECTOR_15
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class SemanticsTest {

    @Nested
    inner class WithSymbols {

        @TestFactory
        fun `should provide symbols`() = testOld(Semantics) {
            expecting { Symbols.OK } that { isEqualTo("✔$VARIATION_SELECTOR_15".ansi.green.done) }
            expecting { Symbols.Negative } that { isEqualTo("━".ansi.red.done) }
            expecting { Symbols.Error } that { isEqualTo("ϟ".ansi.bold.red.done) }
            expecting { Symbols.PointNext } that { isEqualTo("➜".ansi.italic.gray.done) }
            expecting { Symbols.Document } that { isEqualTo("📄") }
            expecting { Symbols.Null } that { isEqualTo("␀".ansi.brightYellow.done) }
            expecting { Symbols.Unknown } that { isEqualTo("❓") }
        }

        @TestFactory
        fun `should format`() = testOld("test") {
            expecting { formattedAs.success } that { isEqualTo("test".ansi.green.done) }
            expecting { formattedAs.warning } that { isEqualTo("test".ansi.brightYellow.done) }
            expecting { formattedAs.failure } that { isEqualTo("test".ansi.red.done) }
            expecting { formattedAs.error } that { isEqualTo("test".ansi.red.done) }
            expecting { formattedAs.debug } that { isEqualTo("test".ansi.brightCyan.done) }
            expecting { formattedAs.input } that { isEqualTo("test".ansi.cyan.done) }
            expecting { formattedAs.meta } that { isEqualTo("test".ansi.italic.gray.done) }
        }
    }
}
