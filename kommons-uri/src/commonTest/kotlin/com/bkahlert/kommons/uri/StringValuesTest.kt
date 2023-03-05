package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import io.ktor.util.StringValues
import io.ktor.util.StringValuesBuilder
import kotlin.test.Test

class StringValuesTest {

    @Test fun build_with_string_values() {
        StringValues.build(StringValues.build(Parameters.Empty) {
            appendAll("foo", emptyList())
        }) {
            append("bar", "baz")
        } shouldBe Fixture
    }

    @Test fun build_without_string_values() {
        StringValues.build(Parameters.Empty) {
            append("bar", "baz")
        } shouldBe StringValues.build(Parameters.Empty) {
            append("bar", "baz")
        }
    }

    @Test fun append() {
        StringValues.build(StringValues.Empty, fun StringValuesBuilder.() {
            append("foo")
        }) shouldBe StringValues.build(StringValues.Empty, fun StringValuesBuilder.() {
            appendAll("foo", emptyList())
        })
    }

    @Test fun form_url_encode() {
        Fixture.formUrlEncode() shouldBe "foo&bar=baz"
        Fixture.formUrlEncode(keepEmptyValues = true) shouldBe "foo&bar=baz"
        Fixture.formUrlEncode(keepEmptyValues = false) shouldBe "bar=baz"
    }

    @Test fun delegate() = testAll {
        val foo by Fixture
        val bar by Fixture
        val baz by Fixture
        foo.shouldBeEmpty()
        bar.shouldContainExactly("baz")
        baz.shouldBeNull()
    }

    companion object {
        val Fixture: StringValues = StringValues.build(StringValues.Empty, fun StringValuesBuilder.() {
            appendAll("foo", emptyList())
            appendAll("bar", listOf("baz"))
        })
    }
}
