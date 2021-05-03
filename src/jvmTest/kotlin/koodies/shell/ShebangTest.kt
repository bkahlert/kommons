package koodies.shell

import koodies.test.testEach
import koodies.text.LineSeparators.lines
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.containsExactly
import java.nio.file.Path

class ShebangTest {

    @TestFactory
    fun `should add to script`() = testEach({
        ShellScript {
            shebang
            !"echo 'shebang'"
        }
    }, {
        ShellScript {
            shebang()
            !"echo 'shebang'"
        }
    }) { scriptFactory ->
        expecting { scriptFactory() } that { linesAreEqualTo("#!/bin/sh", "echo 'shebang'", "") }
    }

    @TestFactory
    fun `should support custom interpreter`() = testEach({
        ShellScript {
            shebang("/my/custom/interpreter")
            !"echo 'shebang'"
        }
    }, {
        ShellScript {
            shebang(Path.of("/my/custom/interpreter"))
            !"echo 'shebang'"
        }
    }) { scriptFactory ->
        expecting { scriptFactory() } that { linesAreEqualTo("#!/my/custom/interpreter", "echo 'shebang'", "") }
    }

    @TestFactory
    fun `should always insert in first line`() = testEach({
        ShellScript {
            !"echo 'shebang'"
            shebang("/my/custom/interpreter")
            ""
        }
    }, {
        ShellScript {
            !"echo 'shebang'"
            shebang(Path.of("/my/custom/interpreter"))
            ""
        }
    }) { scriptFactory ->
        expecting { scriptFactory() } that { linesAreEqualTo("#!/my/custom/interpreter", "echo 'shebang'", "") }
    }

    @TestFactory
    fun `should override existing shebang`() = testEach({
        ShellScript {
            shebang
            !"echo 'shebang'"
            shebang("/my/custom/interpreter")
            shebang("/I/win")
            ""
        }
    }, {
        ShellScript {
            shebang
            !"echo 'shebang'"
            shebang(Path.of("/my/custom/interpreter"))
            shebang(Path.of("/I/win"))
            ""
        }
    }) { scriptFactory ->
        expecting { scriptFactory() } that { linesAreEqualTo("#!/I/win", "echo 'shebang'", "") }
    }
}

fun Assertion.Builder<ShellScript>.linesAreEqualTo(vararg lines: String) {
    get("build %s") { build().lines() }.containsExactly(*lines)
}
