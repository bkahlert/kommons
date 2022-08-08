package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.tracing.Key
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes.Keys.rendering
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.common.Attributes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.startsWith

class RenderingAttributesTest {

    private val renderingAttributes = Attributes.of(
        RenderingAttributes.DESCRIPTION, "custom description",
        RenderingAttributes.RENDERER, "custom renderer",
        Key.stringKey("irrelevant-key"), "irrelevant value",
    ).rendering

    @Test
    fun `should read attributes`() = testAll {
        renderingAttributes.description shouldBe "custom description"
        renderingAttributes.renderer shouldBe "custom renderer"
    }

    @Nested
    inner class ToString {

        @Test
        fun `should start with class name`() {
            expectThat(renderingAttributes.toString())
                .startsWith("RenderingAttributes")
        }

        @Test
        fun `should contain all attributes`() {
            expectThat(renderingAttributes.toString())
                .contains("""description: "custom description"""")
                .contains("""kommons.renderer: "custom renderer"""")
                .contains("""irrelevant-key: "irrelevant value"""")
        }
    }
}
