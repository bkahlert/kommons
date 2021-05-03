package koodies.shell

import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerStopCommandLine
import koodies.exec.exitCodeOrNull
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.io.path.hasContent
import koodies.io.path.pathString
import koodies.io.path.randomFile
import koodies.io.path.writeBytes
import koodies.logging.InMemoryLogger
import koodies.shell.HereDocBuilder.hereDoc
import koodies.shell.ShellScript.Companion.isScript
import koodies.shell.ShellScript.ScriptContext
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.tests
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.joinLinesToString
import koodies.text.matchesCurlyPattern
import koodies.text.toByteArray
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.isExecutable
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import koodies.text.Unicode.escape as e

class ShellScriptTest {

    private fun shellScript() = ShellScript("Test") {
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
            echo "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m $e[96mTEST$e[39m"
            cd "/some/where" || exit 1
            echo "Hello World!"
            echo "Bye!"
            exit 42

        """.trimIndent())
    }

    @TestFactory
    fun `should build with simple string`() = tests {
        expecting {
            ShellScript { "printenv HOME" }.build()
        } that {
            isEqualTo("""
                      printenv HOME
                      
                      """.trimIndent())
        }

        expecting {
            ShellScript { shebang; "printenv HOME" }.build()
        } that {
            isEqualTo("""
                      #!/bin/sh
                      printenv HOME
                      
                      """.trimIndent())
        }
    }

    @Test
    fun `should write valid script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile(extension = ".sh")
        shellScript().buildTo(file)
        expectThat(file).hasContent("""
            #!/bin/sh
            echo "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m $e[96mTEST$e[39m"
            cd "/some/where" || exit 1
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
            cd "{}" || exit 1
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

        private fun getEmbeddedShellScript() = ShellScript("embedded script 📝") {
            shebang
            !"""mkdir "dir""""
            !"""cd "dir""""
            !"""sleep 1"""
            !"""echo "test" > file.txt"""
        }

        private fun ScriptContext.shellScript(): String {
            shebang
            !"""echo "about to run embedded script""""
            embed(getEmbeddedShellScript())
            !"""echo "finished to run embedded script""""
            !"""echo $(pwd)"""
            return ""
        }

        @Test
        fun `should embed shell script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(ShellScript { shellScript() }.build()).matchesCurlyPattern("""
                #!/bin/sh
                echo "about to run embedded script"
                (
                cat <<'EMBEDDED-SCRIPT-{}'
                #!/bin/sh
                echo "{}"
                mkdir "dir"
                cd "dir"
                sleep 1
                echo "test" > file.txt
                EMBEDDED-SCRIPT-{}
                ) > "./embedded-script-_.sh"
                if [ -f "./embedded-script-_.sh" ]; then
                  chmod 755 "./embedded-script-_.sh"
                  "./embedded-script-_.sh"
                  wait
                  rm "./embedded-script-_.sh"
                else
                  echo "Error creating ""embedded-script-_.sh"
                fi
                echo "finished to run embedded script"
                echo $(pwd)
            """.trimIndent())
        }

        @Smoke @Test
        fun InMemoryLogger.`should preserve functionality`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val exec = ShellScript {
                changeDirectoryOrExit(this@withTempDir)
                shellScript()
            }.exec.logging(this@`should preserve functionality`)

            expect {
                that(exec.exitCodeOrNull).isEqualTo(0)
                that(exec.io.ansiRemoved.lines().filter { "terminated successfully at" !in it }.joinLinesToString())
                    .matchesCurlyPattern("""
                        Executing {}
                        about to run embedded script
                        ░░░░░░░ EMBEDDED SCRIPT 📝
                        finished to run embedded script
                        $pathString
                    """.trimIndent())
                that(resolve("dir/file.txt")) {
                    exists()
                    hasContent("test$LF")
                }
            }
        }
    }

    @Nested
    inner class DockerCommand {
        @Test
        fun `should build valid docker run`() {
            expectThat(ShellScript {
                shebang
                !DockerRunCommandLine {
                    image { "image" / "name" }
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
            --interactive \
            --mount \
            type=bind,source=/a/b,target=/c/d \
            --mount \
            type=bind,source=/e/f/../g,target=/h \
            image/name \
            -arg1 \
            --argument \
            2 \
            "<<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC"

        """.trimIndent())
        }

        @Test
        fun `should build valid docker stop`() {
            expectThat(ShellScript {
                shebang
                !DockerStopCommandLine {
                    containers { +"busybox" + "guestfish" }
                    options { time by 42 }
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker \
            stop \
            --time \
            42 \
            busybox \
            guestfish

        """.trimIndent())
        }
    }

    @Nested
    inner class Name {
        private val testBanner = "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m" +
            "$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░" +
            "$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m " +
            "$e[96mTEST$e[39m"

        @Test
        fun `should have an optional name`() {
            val sh = ShellScript("test") { !"exit 0" }
            expectThat(sh).toStringContains("Script(\"test\": ")
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
        expectThat(sh).containsExactly("# test", "exit 0")
    }

    @Test
    fun `should build multi-line comments`() {
        expectThat(ShellScript {

            comment("""
                line 1
                line 2
            """.trimIndent())
            !"exit 0"

        }).containsExactly("# line 1", "# line 2", "exit 0")
    }

    @Nested
    inner class Sudo {

        @Test
        fun `should create sudo line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(ShellScript {
                sudo("a password", "a command")
            }).get { last() }
                .isEqualTo("echo \"a password\" | sudo -S a command")
        }
    }

    @Nested
    inner class DeleteOnCompletion {

        @Test
        fun `should create rm line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(ShellScript {
                deleteSelf()
            }).get { last() }
                .isEqualTo("rm -- \"\$0\"")
        }

        @Test
        fun InMemoryLogger.`should not remove itself by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val script = ShellScript().buildTo(resolve("script.sh"))
            ShellScript { !script.pathString }.exec.logging(this@`should not remove itself by default`)
            expectThat(resolve("script.sh")).exists()
        }

        @Test
        fun InMemoryLogger.`should remove itself`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val script = ShellScript { deleteSelf() }.buildTo(resolve("script.sh"))
            ShellScript { !script.pathString }.exec.logging(this@`should remove itself`)
            expectThat(resolve("script.sh")).not { exists() }
        }
    }

    @TestFactory
    fun `should check if is script`(uniqueId: UniqueId) = tests {
        withTempDir(uniqueId) {
            expecting { "#!".toByteArray() } that { isScript() }
            expecting { "#".toByteArray() } that { not { isScript() } }
            expecting { "foo".toByteArray() } that { not { isScript() } }

            expecting { "#!" } that { isScript() }
            expecting { "#" } that { not { isScript() } }
            expecting { "foo" } that { not { isScript() } }

            expecting { randomFile().writeBytes("#!".toByteArray()) } that { isScript() }
            expecting { randomFile().writeBytes("#".toByteArray()) } that { not { isScript() } }
            expecting { randomFile().writeBytes("foo".toByteArray()) } that { not { isScript() } }
            expecting { resolve("does-not-exist") } that { not { isScript() } }
        }
    }
}

fun Builder<ByteArray>.isScript(): Builder<ByteArray> =
    assert("is script") {
        if (it.isScript) pass()
        else fail("starts with ${it.take(2)}")
    }

@JvmName("charSequenceIsScript")
inline fun <reified T : CharSequence> Builder<T>.isScript() =
    assert("is script") {
        if (it.isScript) pass()
        else fail("starts with ${it.take(2)}")
    }

@JvmName("fileIsScript")
inline fun <reified T : Path> Builder<T>.isScript() =
    assert("is script") {
        if (it.isScript) pass()
        else if (!it.exists()) fail("does not exist")
        else fail("starts with ${it.inputStream().readNBytes(2)}")
    }
