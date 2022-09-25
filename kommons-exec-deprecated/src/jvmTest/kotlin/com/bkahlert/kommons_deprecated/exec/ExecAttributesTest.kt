package com.bkahlert.kommons_deprecated.exec

import com.bkahlert.kommons_deprecated.exec.ExecAttributes.Companion.exec
import com.bkahlert.kommons_deprecated.test.testOld
import com.bkahlert.kommons_deprecated.tracing.Key
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.common.Attributes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

class ExecAttributesTest {

    private val execAttributes = Attributes.of(
        ExecAttributes.NAME, "print environment variable HOME",
        ExecAttributes.EXECUTABLE, "printenv HOME",
        Key.stringKey("irrelevant-key"), "irrelevant value",
    ).exec

    @TestFactory
    fun `should read attributes`() = testOld(execAttributes) {
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
            execAttributes.toString() shouldBe """
                ExecAttributes {
                    irrelevant-key: "irrelevant value",
                    kommons.exec.executable: "printenv HOME",
                    kommons.exec.name: "print environment variable HOME"
                }
            """.trimIndent()
        }
    }
}
