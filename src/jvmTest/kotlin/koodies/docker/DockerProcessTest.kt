package koodies.docker

import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.synchronized
import koodies.test.Slow
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.matchesCurlyPattern
import koodies.text.LineSeparators
import koodies.text.containsAny
import koodies.time.poll
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerProcessTest {

    @DockerRequiring @Test
    fun `should start docker`(uniqueId: UniqueId) {
        val dockerProcess = Docker.busybox(uniqueId.simple, "echo test", processor = noopProcessor())

        kotlin.runCatching {
            poll { dockerProcess.ioLog.logged.any { it.type == OUT && it.unformatted == "test" } }
                .every(100.milliseconds).forAtMost(8.seconds) {
                    if (dockerProcess.alive) fail("Did not log \"test\" output within 8 seconds.")
                    fail("Process terminated without logging: ${dockerProcess.ioLog.dump()}.")
                }
        }.onFailure { dockerProcess.kill() }.getOrThrow()
    }

    @DockerRequiring @Test
    fun `should override toString`(uniqueId: UniqueId) {
        val dockerProcess = Docker.busybox(uniqueId.simple, "echo test", processor = noopProcessor())
        expectThat(dockerProcess.toString())
            .matchesCurlyPattern("DockerProcess[name={}.should_override_toString, Process[{}]")
            .not { containsAny(*LineSeparators.toTypedArray()) }
        dockerProcess.kill()
    }

    @Nested
    inner class Lifecycle {

        @DockerRequiring @Test
        fun `should start docker and pass arguments`(uniqueId: UniqueId) {
            val dockerProcess = Docker.busybox(uniqueId.simple, "echo test", processor = noopProcessor())

            kotlin.runCatching {
                poll { dockerProcess.ioLog.logged.any { it.type == OUT && it.unformatted == "test" } }
                    .every(100.milliseconds).forAtMost(8.seconds) {
                        if (dockerProcess.alive) fail("Did not log \"test\" output within 8 seconds.")
                        fail("Process terminated without logging: ${dockerProcess.ioLog.dump()}.")
                    }
            }.onFailure { dockerProcess.kill() }.getOrThrow()
        }

        @DockerRequiring @Test
        fun `should start docker and process input`(uniqueId: UniqueId) {
            val dockerProcess = Docker.busybox(uniqueId.simple, "echo test", processor = noopProcessor())

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")
                poll { dockerProcess.ioLog.logged.any { it.type == OUT && it.unformatted == "test" } }
                    .every(100.milliseconds).forAtMost(8.seconds) { fail("Did not log self-induced \"test\" output within 8 seconds.") }
                dockerProcess.stop()
            }.onFailure { dockerProcess.kill() }.getOrThrow()
        }

        @DockerRequiring @Test
        fun `should start docker and process output`(uniqueId: UniqueId) {
            val dockerProcess = Docker.busybox(
                uniqueId.simple,
                """while true; do""",
                """echo "looping"""",
                """sleep 1""",
                """done""",
                processor = noopProcessor())

            kotlin.runCatching {
                poll { dockerProcess.ioLog.logged.any { it.type == OUT } }
                    .every(100.milliseconds).forAtMost(8.seconds) { fail("Did not log any output within 8 seconds.") }
            }.onFailure { dockerProcess.kill() }.getOrThrow()
        }

        @DockerRequiring @Smoke @Test
        fun `should start docker and process output produced by own input`(uniqueId: UniqueId) {
            val logged = mutableListOf<String>().synchronized()
            val dockerProcess =
                Docker.busybox(uniqueId.simple) { io ->
                    logged.add(io.unformatted)
                    if (io.type == OUT) {
                        if (logged.contains("test 4 6")) stop()
                        val message = "echo '${io.unformatted} ${io.unformatted.length}'"
                        enter(message)
                    }
                }

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")
                poll {
                    dockerProcess.ioLog.logged.mapNotNull { if (it.type == OUT) it.unformatted else null }.containsAll(listOf("test", "test 4", "test 4 6"))
                }
                    .every(100.milliseconds)
                    .forAtMost(30.seconds) { fail("Did not log self-produced \"test\", \"test 4\" and \"test 4 6\" output within 30 seconds.") }
            }.onFailure { dockerProcess.kill() }.getOrThrow()
        }

        @Nested
        inner class IsRunning {

            @DockerRequiring @Test
            fun `should return false on not yet started container container`(uniqueId: UniqueId) {
                val dockerProcess = Docker.busybox(
                    uniqueId.simple,
                    """while true; do""",
                    """echo "looping"""",
                    """sleep 1""",
                    """done""",
                    processor = noopProcessor())

                kotlin.runCatching {
                    expectThat(dockerProcess.alive).isFalse()
                }.onFailure { dockerProcess.kill() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should return true on running container`(uniqueId: UniqueId) {
                val dockerProcess = Docker.busybox(
                    uniqueId.simple,
                    """while true; do""",
                    """echo "looping"""",
                    """sleep 1""",
                    """done""",
                    processor = noopProcessor())

                kotlin.runCatching {
                    poll { dockerProcess.alive }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("$dockerProcess not start container within 5 seconds.") }
                    expectThat(dockerProcess.alive).isTrue()
                }.onFailure { dockerProcess.kill() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should return false on completed container`(uniqueId: UniqueId) {
                val dockerProcess = Docker.busybox(
                    uniqueId.simple,
                    """while true; do""",
                    """echo "looping"""",
                    """sleep 1""",
                    """done""",
                    processor = noopProcessor())

                kotlin.runCatching {
                    poll { dockerProcess.alive }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }

                    dockerProcess.stop()

                    poll { !dockerProcess.alive }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not stop container within 5 seconds.") }
                    expectThat(dockerProcess.alive).isFalse()
                }.onFailure { dockerProcess.kill() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should stop started container`(uniqueId: UniqueId) {
                val dockerProcess = Docker.busybox(
                    uniqueId.simple,
                    """while true; do""",
                    """echo "looping"""",
                    """sleep 1""",
                    """done""",
                    processor = noopProcessor())

                kotlin.runCatching {
                    poll { dockerProcess.alive }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }

                    dockerProcess.stop()

                    poll { !dockerProcess.alive }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not stop container within 5 seconds.") }
                    expectThat(dockerProcess.alive).isFalse()
                }.onFailure { dockerProcess.kill() }.getOrThrow()
            }
        }

        @Slow @DockerRequiring @Test
        fun `should remove docker container after completion`(uniqueId: UniqueId) {
            val dockerProcess = Docker.busybox(
                uniqueId.simple,
                """while true; do""",
                """echo "looping"""",
                """sleep 1""",
                """done""",
                processor = noopProcessor())

            kotlin.runCatching {
                poll { Docker.exists(dockerProcess.name) }
                    .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }
                expectThat(Docker.exists(dockerProcess.name)).isTrue()

                dockerProcess.stop()

                poll { !Docker.exists(dockerProcess.name) }
                    .every(100.milliseconds).forAtMost(15.seconds) { fail("Did not stop container within 15 seconds.") }
                expectThat(Docker.exists(dockerProcess.name)).isFalse()
            }.onFailure { dockerProcess.kill() }.getOrThrow()
        }
    }

    @Slow @DockerRequiring @Test
    fun `should not produce incorrect empty lines`(uniqueId: UniqueId) {
        val output = mutableListOf<IO>().synchronized()
        val dockerProcess = Docker.busybox(
            uniqueId.simple,
            """while true; do""",
            """echo "looping"""",
            """sleep 1""",
            """done""",
        ) {
            output.add(it)
        }

        kotlin.runCatching {
            20.seconds.sleep()
            dockerProcess.stop()
            expectThat(output).get { size }.isGreaterThan(20)
            expectThat(output.filter { it.isBlank() }.size).isLessThan(output.size / 4)
        }.onFailure { dockerProcess.kill() }.getOrThrow()
    }
}
