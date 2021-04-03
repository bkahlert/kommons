package koodies.docker

import koodies.debug.trace
import koodies.test.string
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.endsWithRandomSuffix
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.length
import java.nio.file.Path

@Execution(SAME_THREAD)
class DockerContainerNameTest {

    @TestFactory
    fun `should throw on illegal name`() = ILLEGAL_NAMES.testEach {
        expectThrowing { DockerContainer(it) }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should accept legal name`() = LEGAL_NAMES.testEach {
        expect { DockerContainer(it) }.that { toStringIsEqualTo(it) }
    }

    @Nested
    inner class Sanitization {

        @TestFactory
        fun `should sanitize illegal name`() = ILLEGAL_NAMES.testEach {
            expect { DockerContainer.from(it) }.that { string.length.isGreaterThanOrEqualTo(8) }
        }

        @TestFactory
        fun `should sanitize legal name`() = LEGAL_NAMES.testEach {
            expect { DockerContainer.from(it) }.that { toStringIsEqualTo(it) }
        }

        @TestFactory
        fun `should append random suffix if specified`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
            expect { DockerContainer.from(it, randomSuffix = true) }.that {
                string.endsWithRandomSuffix()
            }
        }

        @TestFactory
        fun `should not sanitize legal name if specified`() = LEGAL_NAMES.testEach {
            expect { DockerContainer.from(it, randomSuffix = false) }.that { toStringIsEqualTo(it) }
        }

        @Test
        fun `should create name from path and append random suffix`() {
            val path = Path.of("~/.imgcstmzr.test/test/RaspberryPiLite/2020-11-29T21-46-47--9N3k/2020-08-20-raspios-buster-armhf-lite.img")
            val subject = DockerContainer.from(path).trace
            expectThat(subject) {
                string.length.isEqualTo("2020-08-20-raspios-buster-armhf-lite.img".length + 6)
            }
        }

        @Test
        fun `should fill to short name`() {
            expectThat(DockerContainer.from("abc")).string.length.isEqualTo(8)
        }

        @Test
        fun `should truncate to long name`() {
            expectThat(DockerContainer.from("X".repeat(130))).toStringIsEqualTo("X".repeat(128))
        }
    }

    companion object {
        val ILLEGAL_NAMES = listOf("", "---", ".container")
        val LEGAL_NAMES = listOf("dockerDocker", "docker-container", "container.1234")
    }
}

val Assertion.Builder<DockerContainer>.sanitizedName
    get() = get("sanitized name %s") { sanitized }
