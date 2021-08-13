package koodies.exec

import io.opentelemetry.api.common.Attributes
import koodies.exec.ExecAttributes.Companion.exec
import koodies.test.test
import koodies.tracing.Key
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

class ExecAttributesTest {

    private val execAttributes = Attributes.of(
        ExecAttributes.NAME, "print environment variable HOME",
        ExecAttributes.EXECUTABLE, "printenv HOME",
        Key.stringKey("irrelevant-key"), "irrelevant value",
    ).exec

    @TestFactory
    fun `should read attributes`() = test(execAttributes) {
        expecting { name } that { isEqualTo("print environment variable HOME") }
        expecting { executable } that { isEqualTo("printenv HOME") }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should start with class name`() {
            expectThat(execAttributes.toString())
                .startsWith("ExecAttributes")
        }

        @Test
        fun `should contain all attributes`() {
            expectThat(execAttributes.toString())
                .contains("koodies.exec.name = print environment variable HOME")
                .contains("koodies.exec.executable = printenv HOME")
                .contains("irrelevant-key = irrelevant value")
        }
    }
}
