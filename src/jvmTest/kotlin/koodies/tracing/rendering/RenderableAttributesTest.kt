package koodies.tracing.rendering

import koodies.exec.IOAttributes
import koodies.test.test
import koodies.tracing.Key
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.startsWith

class RenderableAttributesTest {

    private val renderableAttributes = RenderableAttributes.of(
        RenderingAttributes.DESCRIPTION renderingOnly "rendering only description",
        IOAttributes.TEXT to "/root",
        Key.stringKey("irrelevant-key") to "irrelevant value",
    )

    @TestFactory
    fun `should check for attributes`() = test(renderableAttributes) {
        expecting("resolving match") { contains(RenderingAttributes.DESCRIPTION.renderingKey) } that { isTrue() }
        expecting("resolving rendering only") { contains(RenderingAttributes.DESCRIPTION) } that { isTrue() }
        expecting("not resolving not-existent rendering only") { contains(IOAttributes.TEXT.renderingKey) } that { isFalse() }
    }

    @TestFactory
    fun `should get attributes`() = test(renderableAttributes) {
        expecting("resolving match") { get(RenderingAttributes.DESCRIPTION.renderingKey) } that {
            isNotNull().get { render(null, null) }.isEqualTo("rendering only description")
        }
        expecting("resolving rendering only") { get(RenderingAttributes.DESCRIPTION) } that {
            isNotNull().get { render(null, null) }.isEqualTo("rendering only description")
        }
        expecting("not resolving not-existent rendering only") { get(IOAttributes.TEXT.renderingKey) } that { isNull() }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should start with class name`() {
            expectThat(renderableAttributes.toString())
                .startsWith("RenderableAttributes")
        }

        @Test
        fun `should contain all attributes`() {
            expectThat(renderableAttributes.toString())
                .contains("description.render = rendering only description")
                .contains("koodies.exec.io.text = /root")
                .contains("irrelevant-key = irrelevant value")
        }
    }
}
