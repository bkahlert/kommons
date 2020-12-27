package koodies.shell

import koodies.concurrent.script
import koodies.docker.docker
import koodies.io.path.Locations
import koodies.io.path.hasContent
import koodies.io.path.randomFile
import koodies.io.path.single
import koodies.shell.HereDocBuilder.hereDoc
import koodies.test.matchesCurlyPattern
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isExecutable
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@Execution(CONCURRENT)
class ShellScriptTest {

    private fun shellScript() = ShellScript("Test").apply {
        shebang
        changeDirectoryOrExit(Path.of("/some/where"))
        !"""
                echo "Hello World!"
                echo "Bye!"
            """.trimIndent()
        exit(42)
    }

    @Test
    fun `should build valid script`() {
        expectThat(shellScript().build()).isEqualTo("""
            #!/bin/sh
            echo "[40;90m░[49;39m[46;96m░[49;39m[44;94m░[49;39m[42;92m░[49;39m[43;93m░[49;39m[45;95m░[49;39m[41;91m░[49;39m [96mTEST[39m"
            cd "/some/where" || exit -1
            echo "Hello World!"
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should write valid script`() = withTempDir {
        val file = randomFile(extension = ".sh")
        shellScript().buildTo(file)
        expectThat(file).hasContent("""
            #!/bin/sh
            echo "[40;90m░[49;39m[46;96m░[49;39m[44;94m░[49;39m[42;92m░[49;39m[43;93m░[49;39m[45;95m░[49;39m[41;91m░[49;39m [96mTEST[39m"
            cd "/some/where" || exit -1
            echo "Hello World!"
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should sanitize script`() {
        val sanitized = ShellScript(name = "Custom Name", content = """
              
              
            cd "/some/where" 
            echo "Hello World!"
            
            #!/bin/sh
            echo "Bye!"
            exit 42
        """.trimIndent()).sanitize(Locations.Temp)

        expectThat(sanitized.build()).matchesCurlyPattern("""
            #!/bin/sh
            echo "░░░░░░░ CUSTOM NAME"
            cd "{}" || exit -1
            echo "Hello World!"
            
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should write executable script`() = withTempDir {
        val file = randomFile(extension = ".sh")
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isExecutable()
    }

    @Test
    fun `should return same file as saved to file`() = withTempDir {
        val file = randomFile(extension = ".sh")
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isEqualTo(file)
    }

    @Nested
    inner class DockerCommand {
        @Test
        fun `should build valid docker run`() {
            expectThat(ShellScript {
                `#!`
                docker { "image" / "name" } run {
                    options {
                        name { "container-name" }
                        mounts {
                            Path.of("/a/b") mountAt "/c/d"
                            Path.of("/e/f/../g") mountAt "//h"
                        }
                    }
                    arguments {
                        +"-arg1"
                        +"--argument" + "2"
                        +hereDoc(label = "HEREDOC") {
                            +"heredoc 1"
                            +"-heredoc-line-2"
                        }
                    }
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker \
            run \
            --name \
            container-name \
            --rm \
            -i \
            --mount \
            type=bind,source=/a/b,target=/c/d \
            --mount \
            type=bind,source=/e/f/../g,target=/h \
            image/name \
            -arg1 \
            --argument \
            2 \
            <<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC

        """.trimIndent())
        }

        @Test
        fun `should build allow redirection`() {
            expectThat(ShellScript().apply {
                shebang
                docker { "image" / "name" } run {
                    redirects { +"2>&1" }
                    options { name { "container-name" } }
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker \
            run \
            --name \
            container-name \
            --rm \
            -i \
            image/name

        """.trimIndent())
        }
    }

    @Test
    fun `should have an optional name`() {
        val sh = ShellScript("test") { !"exit 0" }
        expectThat(sh).toStringIsEqualTo("Script(name=test;content=echo \"\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mTEST\u001B[39m\";exit 0})")
    }

    @Test
    fun `should echo name`() {
        val sh = ShellScript("test") { !"exit 0" }
        expectThat(sh.build()).isEqualTo("""
            echo "[40;90m░[49;39m[46;96m░[49;39m[44;94m░[49;39m[42;92m░[49;39m[43;93m░[49;39m[45;95m░[49;39m[41;91m░[49;39m [96mTEST[39m"
            exit 0
            
        """.trimIndent())
    }

    @Test
    fun `should build comments`() {
        val sh = ShellScript {
            comment("test")
            !"exit 0"
        }
        expectThat(sh.lines).containsExactly("# test", "exit 0")
    }

    @Test
    fun `should build multi-line comments`() {
        expectThat(ShellScript {

            comment("""
                line 1
                line 2
            """.trimIndent())
            !"exit 0"

        }.lines).containsExactly("# line 1", "# line 2", "exit 0")
    }

    @Nested
    inner class Sudo {

        @Test
        fun `should create sudo line`() = withTempDir {
            expectThat(ShellScript {
                sudo("a password", "a command")
            }).get { lines.last() }
                .isEqualTo("echo \"a password\" | sudo -S a command")
        }
    }

    @Nested
    inner class DeleteOnCompletion {

        @Test
        fun `should create rm line`() = withTempDir {
            expectThat(ShellScript {
                deleteOnCompletion()
            }).get { lines.last() }
                .isEqualTo("rm -- \"\$0\"")
        }

        @Test
        fun `should not remove itself without`() = withTempDir {
            val process = script { }
            expectThat(this) {
                get { listDirectoryEntries() }.single { fileName.endsWith(".sh") }
            }
        }

        @Test
        fun `should remove itself`() = withTempDir {
            val process = script { deleteOnCompletion() }
            expectThat(this) {
                get { listDirectoryEntries() }.isEmpty()
            }
        }
    }
}

val Assertion.Builder<ShellScript>.built
    get() = get("built shell script %s") { build() }
