package com.bkahlert.kommons.uri

import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import kotlin.test.Test

class ParametersTest {

    @Test fun build_with_parameters() {
        Parameters.build(Parameters.build(Parameters.Empty) {
            appendAll("foo", emptyList())
        }) {
            append("bar", "baz")
        } shouldBe Fixture
    }

    @Test fun build_without_parameters() {
        Parameters.build(Parameters.Empty) {
            append("bar", "baz")
        } shouldBe Parameters.build(Parameters.Empty) {
            append("bar", "baz")
        }
    }

    @Test fun append() {
        Parameters.build(Parameters.Empty, fun ParametersBuilder.() {
            append("foo")
        }) shouldBe Parameters.build(Parameters.Empty, fun ParametersBuilder.() {
            appendAll("foo", emptyList())
        })
    }

    @Test fun form_url_encode() {
        Fixture.formUrlEncode() shouldBe "foo&bar=baz"
        Fixture.formUrlEncode(keepEmptyValues = true) shouldBe "foo&bar=baz"
        Fixture.formUrlEncode(keepEmptyValues = false) shouldBe "bar=baz"
    }

    companion object {
        val Fixture: Parameters = Parameters.build(Parameters.Empty, fun ParametersBuilder.() {
            appendAll("foo", emptyList())
            appendAll("bar", listOf("baz"))
        })
    }
}
