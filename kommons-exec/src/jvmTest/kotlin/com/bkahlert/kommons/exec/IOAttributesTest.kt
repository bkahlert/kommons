package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.IOAttributes.Companion.io
import com.bkahlert.kommons.test.testOld
import com.bkahlert.kommons.tracing.Key
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.common.Attributes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

class IOAttributesTest {

    private val ioAttributes = Attributes.of(
        IOAttributes.TYPE, "output",
        IOAttributes.TEXT, "/root",
        Key.stringKey("irrelevant-key"), "irrelevant value",
    ).io

    @TestFactory
    fun `should read attributes`() = testOld(ioAttributes) {
        expecting { type } that { isEqualTo("output") }
        expecting { text } that { isEqualTo("/root") }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should start with class name`() {
            expectThat(ioAttributes.toString())
                .startsWith("IOAttributes")
        }

        @Test
        fun `should contain all attributes`() {
            ioAttributes.toString() shouldBe """
                IOAttributes {
                    irrelevant-key: "irrelevant value",
                    kommons.exec.io.text: "/root",
                    kommons.exec.io.type: "output"
                }
            """.trimIndent()
        }
    }
}
