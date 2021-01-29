package koodies.shell

import koodies.concurrent.process.exitValue
import koodies.concurrent.process.io
import koodies.concurrent.script
import koodies.docker.docker
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.hasContent
import koodies.io.path.randomFile
import koodies.io.path.single
import koodies.shell.HereDocBuilder.hereDoc
import koodies.terminal.AnsiCode.Companion.ESC
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.matchesCurlyPattern
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
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
    fun `should write valid script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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
    fun `should write executable script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile(extension = ".sh")
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isExecutable()
    }

    @Test
    fun `should return same file as saved to file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile(extension = ".sh")
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isEqualTo(file)
    }

    @Nested
    inner class FileOperations {

        @Test
        fun `should provide file operations by string`() {
            expectThat(ShellScript {
                file("file.txt") {
                    appendLine("content")
                }
            }.build()).matchesCurlyPattern("""
                cat <<HERE-{} >>"file.txt"
                content
                HERE-{}
                
            """.trimIndent())
        }

        @Test
        fun `should provide file operations by path`() {
            expectThat(ShellScript {
                file("file.txt".asPath()) {
                    appendLine("content")
                }
            }.build()).matchesCurlyPattern("""
                cat <<HERE-{} >>"file.txt"
                content
                HERE-{}
                
            """.trimIndent())
        }
    }

    @Nested
    inner class Embed {
        private fun Path.getEmbeddedShellScript() = ShellScript("embedded script 📝") {
            shebang
            changeDirectoryOrExit(this@getEmbeddedShellScript)
            !"""mkdir "dir""""
            !"""cd "dir""""
            !"""sleep 1"""
            !"""echo "test" > file.txt"""
        }

        private fun Path.shellScript() = ShellScript {
            shebang
            !"""echo "about to run embedded script""""
            embed(getEmbeddedShellScript())
            !"""echo "finish to run embedded script""""
            !"""echo $(pwd)"""
        }

        @Test
        fun `should embed shell script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(shellScript()).built.matchesCurlyPattern("""
                #!/bin/sh
                echo "about to run embedded script"
                (
                cat <<'EMBEDDED-SCRIPT-{}'
                #!/bin/sh
                echo "{}"
                cd "${asString()}" || exit -1
                mkdir "dir"
                cd "dir"
                sleep 1
                echo "test" > file.txt
                EMBEDDED-SCRIPT-{}
                ) > "embedded-script-_.sh"
                if [ -f "embedded-script-_.sh" ]; then
                  chmod 755 "embedded-script-_.sh"
                  "./embedded-script-_.sh"
                  wait
                  rm "embedded-script-_.sh"
                else
                  echo "Error creating \"embedded-script-_.sh\""
                fi
                echo "finish to run embedded script"
                echo $(pwd)
            """.trimIndent())
        }

        @Smoke @Test
        fun `should preserve functionality`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = script(shellScript())
            expect {
                that(process) {
                    exitValue.isEqualTo(0)
                    io.matchesCurlyPattern("""
                        Executing ${asString()}/koodies.process.{}.sh
                        📄 file://${asString()}/koodies.process.{}.sh
                        about to run embedded script
                        ░░░░░░░ EMBEDDED SCRIPT 📝
                        finish to run embedded script
                        ${asString()}
                        Process {} terminated successfully at {}.
                    """.trimIndent())
                }
                that(resolve("dir/file.txt")).hasContent("test\n")
            }
        }
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
                    commandLine {
                        arguments {
                            +"-arg1"
                            +"--argument" + "2"
                            +hereDoc(label = "HEREDOC") {
                                +"heredoc 1"
                                +"-heredoc-line-2"
                            }
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
                    options { name { "container-name" } }
                    commandLine {
                        redirects { +"2>&1" }
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
            image/name

        """.trimIndent())
        }
    }

    @Nested
    inner class Name {
        private val testBanner = "$ESC[40;90m░$ESC[49;39m$ESC[46;96m░$ESC[49;39m" +
            "$ESC[44;94m░$ESC[49;39m$ESC[42;92m░$ESC[49;39m$ESC[43;93m░" +
            "$ESC[49;39m$ESC[45;95m░$ESC[49;39m$ESC[41;91m░$ESC[49;39m " +
            "$ESC[96mTEST$ESC[39m"

        @Test
        fun `should have an optional name`() {
            val sh = ShellScript("test") { !"exit 0" }
            expectThat(sh).toStringIsEqualTo("Script(name=test;content=echo \"$testBanner\";exit 0})")
        }

        @Test
        fun `should echo name`() {
            val sh = ShellScript("test") { !"exit 0" }

            expectThat(sh.build()).isEqualTo("""
            echo "$testBanner"
            exit 0
            
        """.trimIndent())
        }

        @Test
        fun `should accept name during build`() {
            val sh = ShellScript { !"exit 0" }
            expectThat(sh.build("test")).isEqualTo("""
            echo "$testBanner"
            exit 0
            
        """.trimIndent())
        }
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
        fun `should create sudo line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(ShellScript {
                sudo("a password", "a command")
            }).get { lines.last() }
                .isEqualTo("echo \"a password\" | sudo -S a command")
        }
    }

    @Nested
    inner class DeleteOnCompletion {

        @Test
        fun `should create rm line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(ShellScript {
                deleteOnCompletion()
            }).get { lines.last() }
                .isEqualTo("rm -- \"\$0\"")
        }

        @Test
        fun `should not remove itself without`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            script { }
            expectThat(this) {
                get { listDirectoryEntries() }.single { fileName.endsWith(".sh") }
            }
        }

        @Test
        fun `should remove itself`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            script { deleteOnCompletion() }
            expectThat(this) {
                get { listDirectoryEntries() }.isEmpty()
            }
        }
    }
}

val Assertion.Builder<ShellScript>.built
    get() = get("built shell script %s") { build() }
