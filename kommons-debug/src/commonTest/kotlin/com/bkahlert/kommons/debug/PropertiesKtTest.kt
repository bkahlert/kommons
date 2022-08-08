package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlin.test.fail

class PropertiesTest {

    @Test fun properties() = testAll {
        nativeObject().properties shouldBe mapOf(
            "property" to "Function-property",
        )
        BaseClass().properties shouldBe mapOf(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
        )
        Singleton.properties shouldBe buildMap {
            put("baseProperty", "base-property")
            put("openBaseProperty", 42)
            put("protectedOpenBaseProperty", "protected-open-base-property")
            if (Platform.Current == Browser || Platform.Current == NodeJS) put("privateBaseProperty", "private-base-property")
            put("singletonProperty", "singleton-property")
            put("privateSingletonProperty", "private-singleton-property")
        }
        AnonymousSingleton.properties shouldBe mapOf(
            "anonymousSingletonProperty" to "anonymous-singleton-property",
            "privateAnonymousSingletonProperty" to "private-anonymous-singleton-property",
        )
        ListImplementingSingleton.properties shouldBe buildMap {
            put("baseProperty", "base-property")
            put("openBaseProperty", 42)
            put("protectedOpenBaseProperty", "protected-open-base-property")
            when (Platform.Current) {
                Browser, NodeJS -> put("privateBaseProperty", "private-base-property")
                JVM -> put("size", 2)
                else -> fail("untested platform")
            }
            put("singletonProperty", "singleton-property")
            put("privateSingletonProperty", "private-singleton-property")
        }
        ListImplementingAnonymousSingleton.properties shouldBe buildMap {
            if (Platform.Current == JVM) put("size", 2)
            put("anonymousSingletonProperty", "anonymous-singleton-property")
            put("privateAnonymousSingletonProperty", "private-anonymous-singleton-property")
        }
        MapImplementingSingleton.properties shouldBe buildMap {
            put("baseProperty", "base-property")
            put("openBaseProperty", 42)
            put("protectedOpenBaseProperty", "protected-open-base-property")
            when (Platform.Current) {
                Browser, NodeJS -> put("privateBaseProperty", "private-base-property")
                JVM -> {
                    put("size", 2)
                    put("keys", MapImplementingSingleton.keys)
                    put("values", MapImplementingSingleton.values)
                    put("entries", MapImplementingSingleton.entries)
                }

                else -> fail("untested platform")
            }
            put("singletonProperty", "singleton-property")
            put("privateSingletonProperty", "private-singleton-property")
        }
        MapImplementingAnonymousSingleton.properties shouldBe buildMap {
            if (Platform.Current == JVM) {
                put("size", 2)
                put("keys", MapImplementingSingleton.keys)
                put("values", MapImplementingSingleton.values)
                put("entries", MapImplementingSingleton.entries)
            }
            put("anonymousSingletonProperty", "anonymous-singleton-property")
            put("privateAnonymousSingletonProperty", "private-anonymous-singleton-property")
        }
        OrdinaryClass().properties shouldBe buildMap {
            put("baseProperty", "base-property")
            put("openBaseProperty", 42)
            put("protectedOpenBaseProperty", "protected-open-base-property")
            if (Platform.Current == Browser || Platform.Current == NodeJS) put("privateBaseProperty", "private-base-property")
            put("ordinaryProperty", "ordinary-property")
            put("privateOrdinaryProperty", "private-ordinary-property")
        }
        DataClass().properties shouldBe buildMap {
            put("baseProperty", "base-property")
            put("openBaseProperty", 37)
            put("protectedOpenBaseProperty", "overridden-protected-open-base-property")
            if (Platform.Current == Browser || Platform.Current == NodeJS) put("privateBaseProperty", "private-base-property")
            put("dataProperty", "data-property")
            put("privateDataProperty", "private-data-property")
        }
        if (Platform.Current != Browser && Platform.Current != NodeJS) {
            ThrowingClass().properties should {
                it["throwingProperty"].shouldBeInstanceOf<PropertyAccessError>()
                it["privateThrowingProperty"].shouldBeInstanceOf<PropertyAccessError>()
            }
        }
        val customObject = ClassWithDefaultToString(null)
        customObject.properties shouldBe mapOf(
            "foo" to null,
            "bar" to "baz",
        )
        ClassWithDefaultToString(customObject).properties shouldBe mapOf(
            "foo" to customObject,
            "bar" to "baz",
        )
    }

    @Test fun get_or_else() = testAll {
        val instance = DataClass()
        listOf(
            instance::baseProperty,
            DataClass::openBaseProperty
        ).associate { prop -> prop.name to prop.getOrElse(instance) { it } } shouldBe mapOf(
            "baseProperty" to "base-property",
            "openBaseProperty" to 37,
        )
    }

    @Test fun get_or_else_0() = testAll {
        DataClass().kProperties0().associate { prop -> prop.name to prop.getOrElse { it } } shouldBe mapOf(
            "baseProperty" to "base-property",
            "openBaseProperty" to 37,
            "protectedOpenBaseProperty" to "overridden-protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "dataProperty" to "data-property",
            "privateDataProperty" to "private-data-property",
        )

        ThrowingClass().kProperties0().associate { prop -> prop.name to prop.getOrElse { it } } shouldBe mapOf(
            "privateThrowingProperty" to RuntimeException("error reading private property"),
            "throwingProperty" to RuntimeException("error reading property"),
        )
    }

    @Test fun get_or_else_1() = testAll {
        DataClass.kProperties1().associate { prop -> prop.name to prop.getOrElse(DataClass()) { it } } shouldBe mapOf(
            "baseProperty" to "base-property",
            "openBaseProperty" to 37,
            "protectedOpenBaseProperty" to "overridden-protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "dataProperty" to "data-property",
            "privateDataProperty" to "private-data-property",
        )

        ThrowingClass.kProperties1().associate { prop -> prop.name to prop.getOrElse(ThrowingClass()) { it } } shouldBe mapOf(
            "privateThrowingProperty" to RuntimeException("error reading private property"),
            "throwingProperty" to RuntimeException("error reading property"),
        )
    }
}
