package koodies.docker

import koodies.docker.DockerContainer.Status
import koodies.docker.DockerContainer.Status.Existent.Running
import koodies.docker.DockerContainer.Status.NotExistent
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_CONTAINER
import koodies.logging.LoggingContext.Companion.GLOBAL
import koodies.logging.expectLogged
import koodies.test.string
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.endsWithRandomSuffix
import koodies.text.randomString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import strikt.assertions.length
import java.nio.file.Path

@Execution(SAME_THREAD)
class DockerContainerTest {

    @Nested
    inner class ConstructorInstantiation {

        @TestFactory
        fun `should throw on illegal name`() = ILLEGAL_NAMES.testEach {
            val x = DockerContainer { "ds".sanitized }
            expectThrowing { DockerContainer(it) }.that { isFailure().isA<IllegalArgumentException>() }
        }

        @TestFactory
        fun `should accept legal name`() = LEGAL_NAMES.testEach {
            expect { DockerContainer(it) }.that { toStringIsEqualTo(it) }
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal on same name`() {
            expectThat(DockerContainer("docker-container-1")).isEqualTo(DockerContainer("docker-container-1"))
        }

        @Test
        fun `should be unequal on different name`() {
            expectThat(DockerContainer("docker-container-1")).isNotEqualTo(DockerContainer("docker-container-2"))
        }
    }

    @Nested
    inner class Sanitization {

        @Test
        fun `should append random suffix to container`() {
            val container = DockerContainer("docker-container")
            expectThat(DockerContainer { container.withRandomSuffix }).isA<DockerContainer>().string.endsWithRandomSuffix()
        }

        @Nested
        inner class StringBased {

            @TestFactory
            fun `should sanitize illegal name`() = ILLEGAL_NAMES.testEach {
                expect { DockerContainer.from(it) }.that { string.length.isGreaterThanOrEqualTo(8) }
                expect { DockerContainer { it.sanitized } }.that { string.length.isGreaterThanOrEqualTo(8) }
            }

            @TestFactory
            fun `should sanitize legal name`() = LEGAL_NAMES.testEach {
                expect { DockerContainer.from(it) }.that { toStringIsEqualTo(it) }
                expect { DockerContainer { it.sanitized } }.that { toStringIsEqualTo(it) }
            }

            @TestFactory
            fun `should not append random suffix by default`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expect { DockerContainer.from(it) }.that {
                    string.not { endsWithRandomSuffix() }
                }
            }

            @TestFactory
            fun `should append random suffix if specified`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expect { DockerContainer.from(it, randomSuffix = true) }.that { string.endsWithRandomSuffix() }
                expect { DockerContainer { it.withRandomSuffix } }.that { string.endsWithRandomSuffix() }
            }
        }

        @Nested
        inner class PathBased {

            @Test
            fun `should sanitize illegal path`() {
                val path = Path.of("~file202")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { toStringIsEqualTo("Xfile202") }
                expectThat(DockerContainer { path.sanitized }) { toStringIsEqualTo("Xfile202") }
            }

            @Test
            fun `should sanitize legal path`() {
                val path = Path.of("2020-08-20-raspios-buster-armhf-lite.img")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { toStringIsEqualTo("2020-08-20-raspios-buster-armhf-lite.img") }
                expectThat(DockerContainer { path.sanitized }) { toStringIsEqualTo("2020-08-20-raspios-buster-armhf-lite.img") }
            }

            @Test
            fun `should only use filename`() {
                val path = Path.of("dir/2020-08-20-raspios-buster-armhf-lite.img")
                expectThat(DockerContainer.from(path)) { string.not { contains("dir") } }
                expectThat(DockerContainer { path.withRandomSuffix }) { string.not { contains("dir") } }
            }

            @Test
            fun `should append random suffix by default`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-raspios-buster-armhf-lite.img"))) { string.endsWithRandomSuffix() }
                expectThat(DockerContainer { Path.of("2020-08-20-raspios-buster-armhf-lite.img").withRandomSuffix }) { string.endsWithRandomSuffix() }
            }

            @TestFactory
            fun `should not append random suffix if specified`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-raspios-buster-armhf-lite.img"), randomSuffix = false)) {
                    string.not { endsWithRandomSuffix() }
                }
            }
        }

        @Test
        fun `should fill to short name`() {
            expectThat(DockerContainer.from("abc")).string.length.isEqualTo(8)
            expectThat(DockerContainer { "abc".sanitized }).string.length.isEqualTo(8)
        }

        @Test
        fun `should truncate to long name`() {
            expectThat(DockerContainer.from("X".repeat(130))).toStringIsEqualTo("X".repeat(128))
            expectThat(DockerContainer { "X".repeat(130).sanitized }).toStringIsEqualTo("X".repeat(128))
        }
    }

    @Nested
    inner class GetStatus {

        @Test
        fun `should get status of non-existent`() {
            val container = DockerContainer(randomString())
            expectThat(container).hasStatus<NotExistent>()
            GLOBAL.expectLogged.contains("Checking status of $container")
        }

        @Test
        fun `should get status`() {
            use(DOCKER_TEST_CONTAINER) {
                expectThat(it.container).hasStatus<Running> { get { details }.isNotEmpty() }
                GLOBAL.expectLogged.contains("Checking status of ${it.container}")
            }
        }
    }

    @Nested
    inner class ListContainers {

        @Test
        fun `should list containers and log`() {
            val containers = (1..3).map { DOCKER_TEST_CONTAINER.start(true).container }
            expectThat(DockerContainer.toList()).contains(containers)
            GLOBAL.expectLogged.contains("Listing all containers")
        }
    }

    companion object {
        val ILLEGAL_NAMES = listOf("", "---", ".container")
        val LEGAL_NAMES = listOf("dockerDocker", "docker-container", "container.1234")
    }
}

public inline fun <reified T : Status> Builder<DockerContainer>.hasStatus(): Builder<DockerContainer> =
    compose("status") {
        get { status }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

public inline fun <reified T : Status> Builder<DockerContainer>.hasStatus(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<DockerContainer> =
    compose("status") {
        get { status }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }


//public inline fun <reified T:Status> Assertion.Builder<DockerContainer>.hasStatus(
//    expectedState: T,
//    detailsAssertion: Assertion.Builder<String>.() -> Unit,
//): Builder<DockerContainer> =
//    compose("status") {
//        with(it.status) {
//            get { state }.isEqualTo(expectedState)
//            get { details }.detailsAssertion()
//        }
//    }.then { if (allPassed) pass() else fail() }
//
