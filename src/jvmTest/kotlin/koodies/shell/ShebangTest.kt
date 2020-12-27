package koodies.shell

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ShebangTest {
    @Test
    fun `should add to script`() {
        expectThat(ShellScript {
            `#!`
            !"echo 'shebang'"
        }).built.isEqualTo("""
            #!/bin/sh
            echo 'shebang'
            
        """.trimIndent())
    }

    @Test
    fun `should support custom interpreter`() {
        expectThat(ShellScript {
            `#!`("/my/custom/interpreter")
            !"echo 'shebang'"
        }).built.isEqualTo("""
            #!/my/custom/interpreter
            echo 'shebang'
            
        """.trimIndent())
    }

    @Test
    fun `should always insert in first line`() {
        expectThat(ShellScript {
            !"echo 'shebang'"
            `#!`("/my/custom/interpreter")
        }).built.isEqualTo("""
            #!/my/custom/interpreter
            echo 'shebang'
            
        """.trimIndent())
    }

    @Test
    fun `should override existing shebang`() {
        expectThat(ShellScript {
            `#!`
            !"echo 'shebang'"
            `#!`("/my/custom/interpreter")
            `#!`("/I/win")
        }).built.isEqualTo("""
            #!/I/win
            echo 'shebang'
            
        """.trimIndent())
    }

    @Test
    fun `should allow normal function name`() {
        expectThat(ShellScript {
            shebang("/bin/csh")
            !"echo 'shebang'"
        }).built.isEqualTo("""
            #!/bin/csh
            echo 'shebang'
            
        """.trimIndent())
    }
}
