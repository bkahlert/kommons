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
            expect { Symbols.OK }.that { isEqualTo(!"✔$variationSelector15".ansi.green) }
            expect { Symbols.Negative }.that { isEqualTo(!"━".ansi.red) }
            expect { Symbols.Error }.that { isEqualTo(!"ϟ".ansi.red) }
            expect { Symbols.PointNext }.that { isEqualTo(!"➜".ansi.italic.gray) }
            expect { Symbols.Document }.that { isEqualTo("📄") }
            expect { Symbols.Null }.that { isEqualTo(!"␀".ansi.brightYellow) }
        }

        @TestFactory
        fun `should format`() = test("test") {
            expect { formattedAs.success }.that { isEqualTo(!"test".ansi.green) }
            expect { formattedAs.warning }.that { isEqualTo(!"test".ansi.brightYellow) }
            expect { formattedAs.failure }.that { isEqualTo(!"test".ansi.red) }
            expect { formattedAs.error }.that { isEqualTo(!"test".ansi.red) }
            expect { formattedAs.debug }.that { isEqualTo(!"test".ansi.brightCyan) }
            expect { formattedAs.input }.that { isEqualTo(!"test".ansi.cyan) }
            expect { formattedAs.meta }.that { isEqualTo(!"test".ansi.italic.gray) }
        }
    }
}
