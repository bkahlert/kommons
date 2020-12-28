package koodies.docker

import koodies.debug.asEmoji
import koodies.test.Slow
import koodies.time.poll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.milliseconds
import kotlin.time.seconds

@Isolated
@DockerRequiring
@Execution(SAME_THREAD)
class DockerPsTest {

    private val containers = mutableListOf<DockerProcess>()

    @BeforeAll
    fun setUp() {
        listOf(
            "shared-prefix-boot-and-run",
            "shared-prefix-boot-and-run-program-in-user-session",
        ).mapTo(containers) {
            Docker.busybox(it,
                "while true; do",
                "    sleep 1",
                "done").execute()
                .also { poll { it.alive }.every(100.milliseconds).forAtMost(15.seconds) { fail("Docker containers did not start") } }
        }
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should correctly read docker ps output`() = mapOf(
        "shared-prefix-boot" to false,
        "shared-prefix-boot-and-run" to true,
        "shared-prefix-boot-and-run-program" to false,
        "shared-prefix-boot-and-run-program-in-user-session" to true,
    ).flatMap { (name, expectedIsRunning) ->
        listOf(
            dynamicTest("${expectedIsRunning.asEmoji} $name is running?") {
                expectThat(Docker.isContainerRunning(name)).isEqualTo(expectedIsRunning)
            },
            dynamicTest("${expectedIsRunning.asEmoji} $name exists?") {
                expectThat(Docker.exists(name)).isEqualTo(expectedIsRunning)
            },
        )
    }

    @Slow
    @AfterAll
    fun tearDown() {
        containers.forEach { it.kill() }
    }
}
