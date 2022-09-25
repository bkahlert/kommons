package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons_deprecated.exec.IOAttributes
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons_deprecated.tracing.Key
import io.kotest.assertions.withClue
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.startsWith

class RenderableAttributesTest {

    private val renderableAttributes = RenderableAttributes.of(
        RenderingAttributes.DESCRIPTION renderingOnly "rendering only description",
        IOAttributes.TEXT to "/root",
        Key.stringKey("irrelevant-key") to "irrelevant value",
    )

    @Test fun `should check for attributes`() = testAll {
        withClue("resolving match") { renderableAttributes.shouldContainKey(RenderingAttributes.DESCRIPTION.renderingKey) }
        withClue("resolving rendering only") { renderableAttributes.shouldContainKey(RenderingAttributes.DESCRIPTION) }
        withClue("not resolving not-existent rendering only") { renderableAttributes.shouldNotContainKey(IOAttributes.TEXT.renderingKey) }
    }

    @Test fun `should get attributes`() = testAll {
        withClue("resolving match") {
            renderableAttributes.get(RenderingAttributes.DESCRIPTION.renderingKey).shouldNotBeNull()
                .render(null, null) shouldBe "rendering only description"
        }
        withClue("resolving rendering only") {
            renderableAttributes.get(RenderingAttributes.DESCRIPTION).shouldNotBeNull()
                .render(null, null) shouldBe "rendering only description"
        }
        withClue("not resolving not-existent rendering only") {
            renderableAttributes.get(IOAttributes.TEXT.renderingKey).shouldBeNull()
        }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should start with class name`() {
            expectThat(renderableAttributes.toString())
                .startsWith("RenderingKeyPreferringAttributes")
        }

        @Test
        fun `should contain all attributes`() {
            expectThat(renderableAttributes.toString())
                .contains("""description.render: "rendering only description"""")
                .contains("""kommons.exec.io.text: "/root"""")
                .contains("""irrelevant-key: "irrelevant value"""")
        }
    }
}
