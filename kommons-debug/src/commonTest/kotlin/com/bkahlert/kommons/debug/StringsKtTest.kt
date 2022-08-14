package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringsKtTest {

    @Test fun as_string() = testAll {
        OrdinaryClass().asString() shouldBe when (Platform.Current) {
            Browser, NodeJS -> """
                OrdinaryClass {
                    baseProperty: "base-property",
                    openBaseProperty: 42／0x2a,
                    protectedOpenBaseProperty: "protected-open-base-property",
                    privateBaseProperty: "private-base-property",
                    ordinaryProperty: "ordinary-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()

            else -> """
                OrdinaryClass {
                    protectedOpenBaseProperty: "protected-open-base-property",
                    openBaseProperty: 42／0x2a,
                    baseProperty: "base-property",
                    privateOrdinaryProperty: "private-ordinary-property",
                    ordinaryProperty: "ordinary-property"
                }
            """.trimIndent()
        }
        if (Platform.Current != Browser && Platform.Current != NodeJS) {
            ThrowingClass().asString() shouldBe """
            ThrowingClass {
                throwingProperty: <error:java.lang.RuntimeException: error reading property>,
                privateThrowingProperty: <error:java.lang.RuntimeException: error reading private property>
            }
        """.trimIndent()
        }

        OrdinaryClass().asString(OrdinaryClass::ordinaryProperty) shouldBe """
            OrdinaryClass { ordinaryProperty: "ordinary-property" }
        """.trimIndent()

        OrdinaryClass().asString(exclude = listOf(OrdinaryClass::ordinaryProperty)) shouldBe when (Platform.Current) {
            Browser, NodeJS -> """
                OrdinaryClass {
                    baseProperty: "base-property",
                    openBaseProperty: 42／0x2a,
                    protectedOpenBaseProperty: "protected-open-base-property",
                    privateBaseProperty: "private-base-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()

            else -> """
                OrdinaryClass {
                    protectedOpenBaseProperty: "protected-open-base-property",
                    openBaseProperty: 42／0x2a,
                    baseProperty: "base-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()
        }

        ClassWithDefaultToString().asString() shouldBe """ClassWithDefaultToString { bar: "baz" }"""
        ClassWithDefaultToString().asString(excludeNullValues = true) shouldBe """ClassWithDefaultToString { bar: "baz" }"""
        ClassWithDefaultToString().asString(excludeNullValues = false) shouldBe """ClassWithDefaultToString { foo: null, bar: "baz" }"""

        ClassWithDefaultToString().let {
            it.asString {
                put(it::bar, "baz")
                put("baz", ClassWithCustomToString())
            }
        } shouldBe """ClassWithDefaultToString { bar: "baz", baz: custom toString }"""
    }
}
