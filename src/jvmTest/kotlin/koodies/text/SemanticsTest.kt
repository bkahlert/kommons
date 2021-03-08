package koodies.text

import koodies.test.test
import koodies.text.ANSI.Colors.brightCyan
import koodies.text.ANSI.Colors.brightYellow
import koodies.text.ANSI.Colors.cyan
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Colors.green
import koodies.text.ANSI.Colors.red
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
        expect { OK }.that { isEqualTo("✔$variationSelector15".green()) }
        expect { Error }.that { isEqualTo("ϟ".red()) }
        expect { PointNext }.that { isEqualTo("➜") }
    }

    @TestFactory
    fun `should format`() = test("test".formattedAs) {
        expect { success }.that { isEqualTo("test".green()) }
        expect { warning }.that { isEqualTo("test".brightYellow()) }
        expect { failure }.that { isEqualTo("test".red()) }
        expect { error }.that { isEqualTo("test".red()) }
        expect { debug }.that { isEqualTo("test".brightCyan()) }
        expect { input }.that { isEqualTo("test".cyan()) }
        expect { meta }.that { isEqualTo("test".gray()) }
    }
}
