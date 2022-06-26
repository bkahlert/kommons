package com.bkahlert.kommons.exec

import com.bkahlert.kommons.docker.DockerImage
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.shouldMatchGlob
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import java.nio.file.Paths
import kotlin.io.path.readText

class ScriptLinksKtTest {

    @Nested

    inner class ToLink {

        private val commandLine = CommandLine("printenv", "TEST_PROP", name = "command line printing TEST_PROP")
        private val shellScript = ShellScript("shell script printing TEST_PROP") { shebang; !commandLine }
        private val dockerRunCommandLine = DockerRunCommandLine(DockerImage { "repo" / "name" tag "tag" }, commandLine)

        @TestFactory
        fun `should provide URI pointing to valid script`() = testEach(
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
                'docker' 'run' '--name' 'printenv-TEST_PROP--*' '--rm' '--interactive' 'repo/name:tag' 'printenv' 'TEST_PROP'

            """.trimIndent(),
        ) { (executable, pattern) ->
            Paths.get(executable.toLink()).readText() shouldMatchGlob pattern
        }
    }
}
