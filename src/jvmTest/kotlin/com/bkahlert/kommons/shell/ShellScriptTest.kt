package com.bkahlert.kommons.shell

import com.bkahlert.kommons.docker.DockerContainer
import com.bkahlert.kommons.docker.DockerImage
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.docker.DockerStopCommandLine
import com.bkahlert.kommons.docker.MountOptions
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.exitCode
import com.bkahlert.kommons.exec.exitCodeOrNull
import com.bkahlert.kommons.exec.io
import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.isInside
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.randomFile
import com.bkahlert.kommons.io.path.writeBytes
import com.bkahlert.kommons.shell.ShellScript.Companion.isScript
import com.bkahlert.kommons.shell.ShellScript.ScriptContext
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.string
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.Banner
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.lines
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.time.sleep
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.runSpanning
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.startsWith
import strikt.java.exists
import strikt.java.fileName
import strikt.java.isExecutable
import java.net.URI
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.time.measureTime
import com.bkahlert.kommons.text.Unicode.ESCAPE as e

class ShellScriptTest {

    private fun shellScript(name: String? = "Test") = ShellScript(name) {
        shebang
        changeDirectoryOrExit(Path.of("/some/where"))
        echo("Hello World!")
        echo("Bye!")
        exit(42u)
    }

    @Test
    fun `should build valid script`() {
        expectThat(shellScript()).toStringIsEqualTo("""
            #!/bin/sh
            'cd' '/some/where' || 'exit' '1'
            'echo' 'Hello World!'
            'echo' 'Bye!'
            'exit' '42'

        """.trimIndent(), removeAnsi = false)
    }

    @Test
    fun `should build trim indent content`() {
        expectThat(ShellScript("    echo '👈 no padding'"))
            .toStringIsEqualTo("echo '👈 no padding'$LF")
    }

    @Test
    fun `should build with string`() {
        expectThat(ShellScript { "printenv HOME" }).toStringIsEqualTo("printenv HOME$LF")
    }

    @Test
    fun `should build with command`() {
        expectThat(ShellScript { echo("test") }).toStringIsEqualTo("'echo' 'test'$LF")
    }

    @Test
    fun `should build with command and string`() {
        expectThat(ShellScript { echo("test"); "printenv HOME" }).toStringIsEqualTo("'echo' 'test'${LF}printenv HOME$LF")
    }

    @Nested
    inner class ToFile {

        @Test
        fun `should write valid script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = shellScript().toFile()
            expectThat(file)
                .hasContent("""
                    #!/bin/sh
                    'cd' '/some/where' || 'exit' '1'
                    'echo' 'Hello World!'
                    'echo' 'Bye!'
                    'exit' '42'
        
                """.trimIndent())
        }

        @Test
        fun `should use name for filename`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = shellScript("my script").toFile()
            expectThat(file)
                .isInside(Locations.temp)
                .fileName.pathString.startsWith("my-script")
        }

        @Test
        fun `should write executable script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = randomFile(extension = ".sh")
            val returnedScript = shellScript().toFile(file)
            expectThat(returnedScript).isExecutable()
        }

        @Test
        fun `should return same file as saved to file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = randomFile(extension = ".sh")
            val returnedScript = shellScript().toFile(file)
            expectThat(returnedScript).isEqualTo(file)
        }
    }

    @Nested
    inner class ToCommandLine {

        @Test
        fun `should use existing shebang`() {
            expectThat(ShellScript {
                shebang("/bin/custom -x")
                echo("test")
            }.toCommandLine()).isEqualTo(CommandLine("/bin/custom", "-x", "-c", "'echo' 'test'$LF"))
        }

        @Test
        fun `should use default interpreter on missing shebang`() {
            expectThat(ShellScript {
                echo("test")
            }.toCommandLine()).isEqualTo(CommandLine("/bin/sh", "-c", "'echo' 'test'$LF"))
        }
    }

    @Nested
    inner class HasShebang {

        @Test
        fun `should return true if starts with shebang`() {
            expectThat(ShellScript {
                shebang
                echo("shebang")
            }.hasShebang).isTrue()
        }

        @Test
        fun `should return false if not starts with shebang`() {
            expectThat(ShellScript {
                echo("shebang")
                shebang
            }.hasShebang).isFalse()
        }

        @Test
        fun `should return false if shebang is missing`() {
            expectThat(ShellScript {
                echo("shebang")
            }.hasShebang).isFalse()
        }
    }

    @Nested
    inner class Shebang {

        @Test
        fun `should return shebang`() {
            expectThat(ShellScript("""
                #!/bin/bash
                echo 'test'
            """.trimIndent()))
                .get { shebang }
                .isNotNull()
                .isEqualTo(CommandLine("/bin/bash"))
        }

        @Test
        fun `should return shebang with with arguments`() {
            expectThat(ShellScript("""
                #!/bin/bash arg1 '-arg2'
                echo 'test'
            """.trimIndent()))
                .get { shebang }
                .isNotNull()
                .isEqualTo(CommandLine("/bin/bash", "arg1", "-arg2"))
        }

        @Test
        fun `should return null on missing shebang in first line`() {
            expectThat(ShellScript("""
                echo 'test'
                #!/bin/bash arg1 -arg2
            """.trimIndent()))
                .get { shebang }
                .isNull()
        }
    }


    @Nested
    inner class Name {

        private val testBanner = "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m" +
            "$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░" +
            "$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m " +
            "$e[96mTEST$e[39m"

        private val differentBanner = "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m" +
            "$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░" +
            "$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m " +
            "$e[96mDIFFERENT$e[39m"

        @Test
        fun `should not echo name`() {
            val sh = ShellScript("test", "exit 0")
            expectThat(sh.toString()).isEqualTo("""
                exit 0
    
            """.trimIndent())
        }

        @Test
        fun `should echo name if specified`() {
            val sh = ShellScript("test", "exit 0")
            expectThat(sh.toString(echoName = true)).toStringIsEqualTo("""
                echo '$testBanner'
                exit 0
    
            """.trimIndent())
        }

        @Test
        fun `should use different name if specified`() {
            val sh = ShellScript("test", "exit 0")
            expectThat(sh.toString(true, "different")) {
                contains("DIFFERENT")
                not { contains("TEST") }
            }
        }

        @Test
        fun `should echo name after shebang in first line`() {
            val sh = ShellScript("test", "#!/bin/sh\nexit 0")
            expectThat(sh.toString(echoName = true)).toStringIsEqualTo("""
                #!/bin/sh
                echo '$testBanner'
                exit 0
    
            """.trimIndent())
        }

        @Test
        fun `should echo name in first line on missing shebang in first line`() {
            val sh = ShellScript("test", "exit 0\n#!/bin/sh")
            expectThat(sh.toString(echoName = true)).toStringIsEqualTo("""
                echo '$testBanner'
                exit 0
                #!/bin/sh
    
            """.trimIndent())
        }

        @Test
        fun `should echo name in first line on empty script`() {
            val sh = ShellScript("test", null)
            expectThat(sh.toString(echoName = true)).toStringIsEqualTo("""
                echo '$testBanner'
    
    
            """.trimIndent())
        }
    }

    @Nested
    inner class Content {

        @Test
        fun `should provide content`() {
            expectThat(shellScript(null).content).matchesCurlyPattern("""
                #!/bin/sh
                {{}}
                'echo' 'Hello World!'
                'echo' 'Bye!'
                'exit' '42'
            """.trimIndent())
        }
    }

    @Nested
    inner class UsingScriptContext {

        @Nested
        inner class Shebang {

            @Test
            fun `should add using property`() {
                expectThat(ShellScript {
                    shebang
                    echo("shebang")
                }).containsExactly("#!/bin/sh", "'echo' 'shebang'")
            }

            @Test
            fun `should add using function`() {
                expectThat(ShellScript {
                    shebang()
                    echo("shebang")
                }).containsExactly("#!/bin/sh", "'echo' 'shebang'")
            }

            @Test
            fun `should add custom interpreter`() {
                expectThat(ShellScript {
                    shebang("/bin/bash")
                    echo("shebang")
                }).containsExactly("#!/bin/bash", "'echo' 'shebang'")
            }

            @Test
            fun `should add custom path interpreter`() {
                expectThat(ShellScript {
                    shebang("/bin/bash".asPath())
                    echo("shebang")
                }).containsExactly("#!/bin/bash", "'echo' 'shebang'")
            }

            @Test
            fun `should add custom interpreter with arguments`() {
                expectThat(ShellScript {
                    shebang("/bin/bash", "arg1", "-arg2")
                    echo("shebang")
                }).containsExactly("#!/bin/bash arg1 -arg2", "'echo' 'shebang'")
            }

            @Test
            fun `should add custom path interpreter with arguments`() {
                expectThat(ShellScript {
                    shebang("/bin/bash".asPath(), "arg1", "-arg2")
                    echo("shebang")
                }).containsExactly("#!/bin/bash arg1 -arg2", "'echo' 'shebang'")
            }

            @Test
            fun `should add anywhere`() {
                expectThat(ShellScript {
                    shebang
                    echo("shebang")
                    shebang("/bin/bash")
                }).containsExactly("#!/bin/sh", "'echo' 'shebang'", "#!/bin/bash")
            }
        }

        @Nested
        inner class WithLine {

            @TestFactory
            fun `should or`() = testEach(
                ShellScript { echo("A") or echo("B"); echo("C") },
                ShellScript { echo("A") or "'echo' 'B'"; echo("C") },
            ) {
                expecting { it } that { containsExactly("'echo' 'A' || 'echo' 'B'", "'echo' 'C'") }
            }

            @TestFactory
            fun `should or at end`() = testEach(
                ShellScript { echo("A"); echo("B") or echo("C") },
                ShellScript { echo("A"); echo("B") or "'echo' 'C'" },
            ) {
                expecting { it } that { containsExactly("'echo' 'A'", "'echo' 'B' || 'echo' 'C'") }
            }

            @TestFactory
            fun `should and`() = testEach(
                ShellScript { echo("A") and echo("B"); echo("C") },
                ShellScript { echo("A") and "'echo' 'B'"; echo("C") },
            ) {
                expecting { it } that { containsExactly("'echo' 'A' && 'echo' 'B'", "'echo' 'C'") }
            }

            @TestFactory
            fun `should and at end`() = testEach(
                ShellScript { echo("A"); echo("B") and echo("C") },
                ShellScript { echo("A"); echo("B") and "'echo' 'C'" },
            ) {
                expecting { it } that { containsExactly("'echo' 'A'", "'echo' 'B' && 'echo' 'C'") }
            }

            @Test
            fun `should redirect to file`() {
                expectThat(ShellScript { echo("A") redirectTo "file".asPath(); echo("B") })
                    .containsExactly("'echo' 'A' > 'file'", "'echo' 'B'")
            }

            @Test
            fun `should redirect to file at end`() {
                expectThat(ShellScript { echo("A"); echo("B") redirectTo "file".asPath() })
                    .containsExactly("'echo' 'A'", "'echo' 'B' > 'file'")
            }
        }

        @Nested
        inner class ChangeDirectory {

            @Test
            fun `should build change directory command`() {
                expectThat(ShellScript { changeDirectory("a/b/c".asPath()) }).containsExactly("'cd' 'a/b/c'")
            }

            @Test
            fun `should build change directory command or exit`() {
                expectThat(ShellScript { changeDirectoryOrExit("a/b/c".asPath()) }).containsExactly("'cd' 'a/b/c' || 'exit' '1'")
            }
        }

        @TestFactory
        fun `should echo`() = testEach<Pair<String, ScriptInit>>(
            "'echo'" to { echo() },
            "'echo' 'Hello!'" to { echo("Hello!") },
            "'echo' 'Hello World!'" to { echo("Hello World!") },
            "'echo' 'Hello' 'World!'" to { echo("Hello", "World!") },
        ) { (expected, init) ->
            expecting { ShellScript { init() } } that { string.lines().first().isEqualTo(expected) }
        }

        @Slow @Nested
        inner class Poll {

            @Test
            fun `should proceed if connection succeeds`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val passedTime = http(8900) {
                    measureTime {
                        ShellScript { poll(URI("http://localhost:8900")) }.exec.logging()
                    }
                }
                expectThat(passedTime).isLessThan(3.seconds)
            }

            @Test
            fun `should retry until succeeds`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val passedTime = measureTime {
                    thread {
                        2.seconds.sleep()
                        http(8901) { 5.seconds.sleep() }
                    }
                    ShellScript { poll(URI("http://localhost:8901"), interval = 3.seconds) }.exec.logging()
                }
                expectThat(passedTime).isGreaterThan(3.seconds)
            }

            @Test
            fun `should not print`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = http(8902) {
                    ShellScript { poll(URI("http://localhost:8902"), interval = 1.seconds) }.exec.logging()
                }
                expectThat(process).io.isEmpty()
            }

            @Test
            fun `should print if specified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = http(8903) {
                    ShellScript { poll(URI("http://localhost:8903"), interval = 3.seconds, verbose = true) }.exec.logging()
                }
                expectThat(process).io
                    .contains(Output typed "Polling http://localhost:8903...")
                    .contains(Output typed "Polled http://localhost:8903 successfully.")
            }

            @Test
            fun `should exit on timeout`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val passedTime = measureTime {
                    ShellScript {
                        poll(URI("http://localhost:8904"), timeout = 1.seconds)
                    }.exec.logging()
                }
                expectThat(passedTime).isLessThan(5.seconds)
            }

            @Test
            fun `should exit with 124 on timeout`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = ShellScript {
                    poll(URI("http://localhost:8905"), timeout = 1.seconds)
                }.exec.logging()
                expectThat(process.exitCode).isEqualTo(124)
            }

            @Test
            fun `should throw on invalid interval`() {
                expectThrows<IllegalArgumentException> { ShellScript { poll(URI("http://localhost"), interval = .5.seconds) } }
            }

            @Test
            fun `should throw on invalid attempt timeout`() {
                expectThrows<IllegalArgumentException> { ShellScript { poll(URI("http://localhost"), attemptTimeout = .5.seconds) } }
            }

            @Test
            fun `should throw on invalid timeout`() {
                expectThrows<IllegalArgumentException> { ShellScript { poll(URI("http://localhost"), timeout = .5.seconds) } }
            }
        }

        @Nested
        inner class FileOperations {

            @Test
            fun `should provide file operations by string`() {
                expectThat(ShellScript {
                    file("file.txt") {
                        appendLine("content")
                    }
                }).toStringMatchesCurlyPattern("""
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
                }).toStringMatchesCurlyPattern("""
                    cat <<HERE-{} >>"file.txt"
                    content
                    HERE-{}
    
                """.trimIndent())
            }
        }

        @Nested
        inner class Embed {

            private fun getEmbeddedShellScript() = ShellScript("embedded script 📝") {
                shebang("/bin/bash")
                !"mkdir 'dir'"
                !"cd 'dir'"
                !"sleep 1"
                !"echo 'test' > 'file.txt'"
            }

            private fun ScriptContext.shellScript(echoName: Boolean): String {
                shebang
                !"echo 'about to run embedded script'"
                embed(getEmbeddedShellScript(), echoName)
                !"echo 'finished to run embedded script'"
                !"echo $(pwd)"
                return ""
            }

            @Test
            fun `should embed shell script`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(ShellScript { shellScript(false) }).toStringMatchesCurlyPattern("""
                    #!/bin/sh
                    echo 'about to run embedded script'
                    '/bin/bash' '-c' 'mkdir '"'"'dir'"'"'
                    cd '"'"'dir'"'"'
                    sleep 1
                    echo '"'"'test'"'"' > '"'"'file.txt'"'"'
                    '
                    echo 'finished to run embedded script'
                    echo $(pwd)
                """.trimIndent())
            }

            @Test
            fun `should embed shell script with name if specified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(ShellScript { shellScript(true) }).toStringMatchesCurlyPattern("""
                    #!/bin/sh
                    echo 'about to run embedded script'
                    '/bin/bash' '-c' 'echo '"'"'${Banner.banner("embedded script 📝")}'"'"'
                    mkdir '"'"'dir'"'"'
                    cd '"'"'dir'"'"'
                    sleep 1
                    echo '"'"'test'"'"' > '"'"'file.txt'"'"'
                    '
                    echo 'finished to run embedded script'
                    echo $(pwd)
                """.trimIndent())
            }

            @Smoke @Test
            fun `should preserve functionality`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val exec = ShellScript {
                    changeDirectoryOrExit(this@withTempDir)
                    shellScript(true)
                }.exec.logging()

                expect {
                    that(exec.exitCodeOrNull).isEqualTo(0)
                    that(exec.io.ansiRemoved.lines().filter { "terminated successfully at" !in it }.joinLinesToString())
                        .matchesCurlyPattern("""
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

            @Suppress("LongLine")
            @Test
            fun `should build valid docker run`() {
                expectThat(ShellScript {
                    shebang
                    !DockerRunCommandLine(
                        image = DockerImage { "image" / "name" },
                        options = Options(
                            name = DockerContainer.from("container-name"),
                            mounts = MountOptions {
                                Path.of("/a/b") mountAt "/c/d"
                                Path.of("/e/f/../g") mountAt "//h"
                            },
                        ),
                        executable = CommandLine("-arg1", "--argument", "2"),
                    )
                }).toStringIsEqualTo("""
                    #!/bin/sh
                    'docker' 'run' '--name' 'container-name' '--rm' '--interactive' '--mount' 'type=bind,source=/a/b,target=/c/d' '--mount' 'type=bind,source=/e/f/../g,target=/h' 'image/name' '-arg1' '--argument' '2'
                    
                """.trimIndent())
            }

            @Test
            fun `should build valid docker stop`() {
                expectThat(ShellScript {
                    shebang
                    !DockerStopCommandLine("busybox", "guestfish", time = 42.seconds)
                }).toStringIsEqualTo("""
                    #!/bin/sh
                    'docker' 'stop' '--time' '42' 'busybox' 'guestfish'
        
                """.trimIndent())
            }
        }

        @Nested
        inner class Shutdown {

            @Test
            fun `should build shutdown command`() {
                expectThat(ShellScript { shutdown() }).containsExactly("'shutdown' '-h' 'now'")
            }

            @Test
            fun `should build shutdown command without halt flag if specified`() {
                expectThat(ShellScript { shutdown(halt = false) }).containsExactly("'shutdown' 'now'")
            }

            @Test
            fun `should build shutdown command with specified time`() {
                expectThat(ShellScript { shutdown(time = "+2") }).containsExactly("'shutdown' '-h' '+2'")
            }

            @Test
            fun `should build shutdown command with specified message`() {
                expectThat(ShellScript { shutdown(message = "shutting down now") }).containsExactly("'shutdown' '-h' 'now' 'shutting down now'")
            }
        }

        @Nested
        inner class Exit {

            @Test
            fun `should build exit command`() {
                val sh = ShellScript {
                    exit(42u)
                }
                expectThat(sh).containsExactly("'exit' '42'")
            }
        }

        @Nested
        inner class Comment {

            @Test
            fun `should build comments`() {
                val sh = ShellScript {
                    comment("test")
                    "exit 0"
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
                    "exit 0"

                }).containsExactly("# line 1", "# line 2", "exit 0")
            }
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
            fun `should not remove itself by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val script = ShellScript().toFile(resolve("script.sh"))
                ShellScript { !script.pathString }.exec.logging()
                expectThat(resolve("script.sh")).exists()
            }

            @Test
            fun `should remove itself`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val script = ShellScript { deleteSelf() }.toFile(resolve("script.sh"))
                ShellScript { !script.pathString }.exec.logging()
                expectThat(resolve("script.sh")).not { exists() }
            }
        }
    }

    @Nested
    inner class CompanionObject {

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

fun <R> http(
    port: Int = 8000,
    responseText: String = HtmlFixture.text,
    block: SpanScope.() -> R,
): R {
    var result: Result<R>? = null
    runSpanning("server") {
        val engine = embeddedServer(Netty, port = port) {
            routing {
                get("/") {
                    call.respondText(responseText)
                }
            }
        }.start(wait = false)
        result = kotlin.runCatching { block() }
        engine.stop(0L, 0L)
    }
    return result?.getOrThrow() ?: error("error running nginx")
}
