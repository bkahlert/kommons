package koodies.builder

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PseudoBuilderTest {

    private class CustomPseudoBuilder : PseudoBuilder<String>

    private val builder = CustomPseudoBuilder()

    @Test
    fun `should return compute result as build result`() {
        expectThat(builder.build { "abc" }).isEqualTo("abc")
    }

    @Test
    fun `should return value as build result`() {
        expectThat(builder.using("abc")).isEqualTo("abc")
    }

    @Test
    fun `should build and transform`() {
        expectThat(builder.build({ "abc" }) { this + this }).isEqualTo("abcabc")
    }
}
