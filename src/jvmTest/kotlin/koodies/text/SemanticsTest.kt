package koodies.text

import koodies.test.test
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode.Emojis.variationSelector15
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class SemanticsTest {

    @Nested
    inner class WithSymbols {

        @TestFactory
        fun `should provide symbols`() = test(Semantics) {
            expecting { Symbols.OK } that { isEqualTo("✔$variationSelector15".ansi.green.done) }
            expecting { Symbols.Negative } that { isEqualTo("━".ansi.red.done) }
            expecting { Symbols.Error } that { isEqualTo("ϟ".ansi.red.done) }
            expecting { Symbols.PointNext } that { isEqualTo("➜".ansi.italic.gray.done) }
            expecting { Symbols.Document } that { isEqualTo("📄") }
            expecting { Symbols.Null } that { isEqualTo("␀".ansi.brightYellow.done) }
            expecting { Symbols.Unknown } that { isEqualTo("❓") }
        }

        @TestFactory
        fun `should format`() = test("test") {
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
