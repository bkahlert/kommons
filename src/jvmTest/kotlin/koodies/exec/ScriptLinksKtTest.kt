package koodies.exec

import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.io.path.asPath
import koodies.io.path.text
import koodies.shell.ShellScript
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
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
                asPath().text.matchesCurlyPattern(pattern)
            }
        }
    }
}
