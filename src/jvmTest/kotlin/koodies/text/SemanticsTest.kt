package koodies.text

import koodies.test.test
import koodies.text.ANSI.Colors.brightCyan
import koodies.text.ANSI.Colors.brightYellow
import koodies.text.ANSI.Colors.cyan
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Colors.green
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.Style.italic
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode.Emojis.variationSelector15
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class SemanticsTest {

    @TestFactory
    fun `should provide symbols`() = test(Semantics) {
        expect { OK }.that { isEqualTo("✔$variationSelector15".green().toString()) }
        expect { Error }.that { isEqualTo("ϟ".red().toString()) }
        expect { PointNext }.that { isEqualTo("➜".gray().italic().toString()) }
        expect { Document }.that { isEqualTo("📄") }
        expect { Null }.that { isEqualTo("␀".bold().toString()) }
    }

    @TestFactory
    fun `should format`() = test("test".formattedAs) {
        expect { success }.that { isEqualTo("test".green().toString()) }
        expect { warning }.that { isEqualTo("test".brightYellow().toString()) }
        expect { failure }.that { isEqualTo("test".red().toString()) }
        expect { error }.that { isEqualTo("test".red().toString()) }
        expect { debug }.that { isEqualTo("test".brightCyan().toString()) }
        expect { input }.that { isEqualTo("test".cyan().toString()) }
        expect { meta }.that { isEqualTo("test".gray().italic().toString()) }
    }
}
