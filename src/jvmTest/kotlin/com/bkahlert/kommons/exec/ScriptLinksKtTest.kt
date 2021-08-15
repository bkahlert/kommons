package com.bkahlert.kommons.exec

import com.bkahlert.kommons.docker.DockerImage
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory

class ScriptLinksKtTest {

    @Nested
    inner class ToLink {

        private val commandLine = CommandLine("printenv", "TEST_PROP", name = "command line printing TEST_PROP")
        private val shellScript = ShellScript("shell script printing TEST_PROP") { shebang; !commandLine }
        private val dockerRunCommandLine = DockerRunCommandLine(DockerImage { "repo" / "name" tag "tag" }, commandLine)

        @TestFactory
        fun `should provide URI pointing to valid script`() = testEach<Pair<Executable<*>, String>>(
            commandLine to """
                #!/bin/sh
                'printenv' 'TEST_PROP'
            """.trimIndent(),
            shellScript to """
                #!/bin/sh
                'printenv' 'TEST_PROP'
            """.trimIndent(),
            dockerRunCommandLine to """
                #!/bin/sh
                'docker' 'run' '--name' 'printenv-TEST_PROP--{}' '--rm' '--interactive' 'repo/name:tag' 'printenv' 'TEST_PROP'
            """.trimIndent(),
        ) { (executable, pattern) ->
            expecting { executable.toLink() } that {
                asPath().textContent.matchesCurlyPattern(pattern)
            }
        }
    }
}
