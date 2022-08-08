package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.js.json
import kotlin.test.Test

class MiscTest {

    @Test fun iterable_to_json() = testAll {
        emptyList<Pair<String, Any?>>().toJson().entries shouldBe json().entries
        listOf("notNull" to 42, "null" to null, "map" to mapOf("foo" to "bar")).toJson().entries shouldBe json(
            "notNull" to 42,
            "null" to null,
            "map" to mapOf("foo" to "bar"),
        ).entries
    }

    @Test fun map_to_json() = testAll {
        emptyMap<String, Any?>().toJson().entries shouldBe json().entries
        mapOf("notNull" to 42, "null" to null, "map" to mapOf("foo" to "bar")).toJson().entries shouldBe json(
            "notNull" to 42,
            "null" to null,
            "map" to mapOf("foo" to "bar"),
        ).entries
    }

    @Test fun iterable_to_json_array() = testAll {
        emptyList<Any?>().toJsonArray().entries shouldBe json().entries
        listOf(42, null, mapOf("foo" to "bar")).toJsonArray() should {
            it.size shouldBe 3
            it[0] shouldBe 42
            it[1].entries shouldBe emptyArray()
            it[2].entries shouldBe arrayOf(arrayOf("foo", "bar"))
        }
    }

    @Test fun map_to_json_array() = testAll {
        emptyArray<Any?>().toJsonArray().entries shouldBe json().entries
        arrayOf(42, null, mapOf("foo" to "bar")).toJsonArray() should {
            it.size shouldBe 3
            it[0] shouldBe 42
            it[1].entries shouldBe emptyArray()
            it[2].entries shouldBe arrayOf(arrayOf("foo", "bar"))
        }
    }

    @Test fun object_stringify_extension() = testAll {
        null.stringify() shouldBe "null"
        "string".stringify() shouldBe """
            "string"
        """.trimIndent()
        arrayOf("string", 42).stringify() shouldBe """
            [
              "string",
              42
            ]
        """.trimIndent()
        listOf("string", 42).stringify() shouldBe """
            [
              "string",
              42
            ]
        """.trimIndent()
        arrayOf("string" to "value", "digit" to 42).stringify() shouldBe """
            [
              {
                "first": "string",
                "second": "value"
              },
              {
                "first": "digit",
                "second": 42
              }
            ]
        """.trimIndent()
        mapOf("string" to "value", "digit" to 42).stringify() shouldBe """
            {
              "string": "value",
              "digit": 42
            }
        """.trimIndent()
        nativeObject().stringify() shouldBe """
            {
              "property": "Function-property"
            }
            """.trimIndent()
        BaseClass().stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 42,
              "protectedOpenBaseProperty": "protected-open-base-property",
              "privateBaseProperty": "private-base-property"
            }
            """.trimIndent()
        Singleton.stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 42,
              "protectedOpenBaseProperty": "protected-open-base-property",
              "privateBaseProperty": "private-base-property",
              "singletonProperty": "singleton-property",
              "privateSingletonProperty": "private-singleton-property"
            }
            """.trimIndent()
        AnonymousSingleton.stringify() shouldBe """
            {
              "anonymousSingletonProperty": "anonymous-singleton-property",
              "privateAnonymousSingletonProperty": "private-anonymous-singleton-property"
            }
            """.trimIndent()
        ListImplementingSingleton.stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 42,
              "protectedOpenBaseProperty": "protected-open-base-property",
              "privateBaseProperty": "private-base-property",
              "singletonProperty": "singleton-property",
              "privateSingletonProperty": "private-singleton-property"
            }
            """.trimIndent()
        ListImplementingAnonymousSingleton.stringify() shouldBe """
            {
              "anonymousSingletonProperty": "anonymous-singleton-property",
              "privateAnonymousSingletonProperty": "private-anonymous-singleton-property"
            }
            """.trimIndent()
        MapImplementingSingleton.stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 42,
              "protectedOpenBaseProperty": "protected-open-base-property",
              "privateBaseProperty": "private-base-property",
              "singletonProperty": "singleton-property",
              "privateSingletonProperty": "private-singleton-property"
            }
            """.trimIndent()
        MapImplementingAnonymousSingleton.stringify() shouldBe """
            {
              "anonymousSingletonProperty": "anonymous-singleton-property",
              "privateAnonymousSingletonProperty": "private-anonymous-singleton-property"
            }
            """.trimIndent()
        OrdinaryClass().stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 42,
              "protectedOpenBaseProperty": "protected-open-base-property",
              "privateBaseProperty": "private-base-property",
              "ordinaryProperty": "ordinary-property",
              "privateOrdinaryProperty": "private-ordinary-property"
            }
            """.trimIndent()
        DataClass().stringify() shouldBe """
            {
              "baseProperty": "base-property",
              "openBaseProperty": 37,
              "protectedOpenBaseProperty": "overridden-protected-open-base-property",
              "privateBaseProperty": "private-base-property",
              "dataProperty": "data-property",
              "privateDataProperty": "private-data-property"
            }
            """.trimIndent()
    }

    @Test fun string_parse_extension() = testAll {
        null.stringify().parseJson().entries shouldBe json().entries
        "string".stringify().parseJson().entries shouldBe "string".entries
        arrayOf("string", 42).stringify().parseJson().entries shouldBe json(
            "0" to "string",
            "1" to 42,
        ).entries
        listOf("string", 42).stringify().parseJson().entries shouldBe json(
            "0" to "string",
            "1" to 42,
        ).entries
        arrayOf("string" to "value", "digit" to 42).stringify().parseJson().stringify() shouldBe arrayOf(
            "string" to "value",
            "digit" to 42,
        ).stringify()
        mapOf("string" to "value", "digit" to 42).stringify().parseJson().entries shouldBe json(
            "string" to "value",
            "digit" to 42
        ).entries
        nativeObject().stringify().parseJson().entries shouldBe json(
            "property" to "Function-property",
        ).entries
        BaseClass().stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
        ).entries
        Singleton.stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "singletonProperty" to "singleton-property",
            "privateSingletonProperty" to "private-singleton-property"
        ).entries
        AnonymousSingleton.stringify().parseJson().entries shouldBe json(
            "anonymousSingletonProperty" to "anonymous-singleton-property",
            "privateAnonymousSingletonProperty" to "private-anonymous-singleton-property"
        ).entries
        ListImplementingSingleton.stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "singletonProperty" to "singleton-property",
            "privateSingletonProperty" to "private-singleton-property"
        ).entries
        ListImplementingAnonymousSingleton.stringify().parseJson().entries shouldBe json(
            "anonymousSingletonProperty" to "anonymous-singleton-property",
            "privateAnonymousSingletonProperty" to "private-anonymous-singleton-property"
        ).entries
        MapImplementingSingleton.stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "singletonProperty" to "singleton-property",
            "privateSingletonProperty" to "private-singleton-property"
        ).entries
        MapImplementingAnonymousSingleton.stringify().parseJson().entries shouldBe json(
            "anonymousSingletonProperty" to "anonymous-singleton-property",
            "privateAnonymousSingletonProperty" to "private-anonymous-singleton-property"
        ).entries
        OrdinaryClass().stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 42,
            "protectedOpenBaseProperty" to "protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "ordinaryProperty" to "ordinary-property",
            "privateOrdinaryProperty" to "private-ordinary-property",
        ).entries
        DataClass().stringify().parseJson().entries shouldBe json(
            "baseProperty" to "base-property",
            "openBaseProperty" to 37,
            "protectedOpenBaseProperty" to "overridden-protected-open-base-property",
            "privateBaseProperty" to "private-base-property",
            "dataProperty" to "data-property",
            "privateDataProperty" to "private-data-property",
        ).entries
    }

    @Test fun any_to_json_extension() = testAll {
        null.toJson().entries shouldBe null.stringify().parseJson().entries
        "string".toJson().entries shouldBe "string".stringify().parseJson().entries
        arrayOf("string", 42).toJson().entries shouldBe arrayOf("string", 42).stringify().parseJson().entries
        listOf("string", 42).toJson().entries shouldBe listOf("string", 42).stringify().parseJson().entries
        arrayOf("string" to "value", "digit" to 42).toJson().stringify() shouldBe arrayOf("string" to "value", "digit" to 42).stringify().parseJson()
            .stringify()
        mapOf("string" to "value", "digit" to 42).toJson().entries shouldBe mapOf("string" to "value", "digit" to 42).stringify().parseJson().entries
        nativeObject().toJson().entries shouldBe nativeObject().stringify().parseJson().entries
        BaseClass().toJson().entries shouldBe BaseClass().stringify().parseJson().entries
        Singleton.toJson().entries shouldBe Singleton.stringify().parseJson().entries
        AnonymousSingleton.toJson().entries shouldBe AnonymousSingleton.stringify().parseJson().entries
        ListImplementingSingleton.toJson().entries shouldBe ListImplementingSingleton.stringify().parseJson().entries
        ListImplementingAnonymousSingleton.toJson().entries shouldBe ListImplementingAnonymousSingleton.stringify().parseJson().entries
//        MapImplementingSingleton.toJson().entries shouldBe MapImplementingSingleton.stringify().parse().entries
//        MapImplementingAnonymousSingleton.toJson().entries shouldBe MapImplementingAnonymousSingleton.stringify().parse().entries
        OrdinaryClass().toJson().entries shouldBe OrdinaryClass().stringify().parseJson().entries
        DataClass().toJson().entries shouldBe DataClass().stringify().parseJson().entries
    }
}
