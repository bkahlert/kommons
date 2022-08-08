package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.debug.Compression.Always
import com.bkahlert.kommons.debug.Compression.Never
import com.bkahlert.kommons.debug.CustomToString.Ignore
import com.bkahlert.kommons.debug.CustomToString.IgnoreForPlainCollectionsAndMaps
import com.bkahlert.kommons.debug.Typing.SimplyTyped
import com.bkahlert.kommons.debug.Typing.Untyped
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.fail

class RenderTest {

    @Test fun render_null() = testAll {
        null.render() shouldBe "null"
    }

    @Suppress("SpellCheckingInspection")
    @Test fun render_string() = testAll {
        renderString("") { compression = Always } shouldBe "\"\""
        renderString(" ") { compression = Always } shouldBe "\" \""
        renderString("string") { compression = Always } shouldBe "\"string\""
        renderString("line 1\nline 2") { compression = Always } shouldBe "\"line 1\\nline 2\""

        renderString("") { compression = Never } shouldBe "\"\""
        renderString(" ") { compression = Never } shouldBe "\" \""
        renderString("string") { compression = Never } shouldBe "\"string\""
        renderString("line 1\nline 2") { compression = Never } shouldBe """
            "${"\""}"
            line 1
            line 2
            "${"\""}"
        """.trimIndent()

        renderString("") shouldBe "\"\""
        renderString(" ") shouldBe "\" \""
        renderString("string") shouldBe "\"string\""
        renderString("line 1\nline 2") shouldBe """
            "${"\""}"
            line 1
            line 2
            "${"\""}"
        """.trimIndent()
    }

    @Test fun render_char_sequence() = testAll {
        val charSequenceWithNoCustomToString = object : CharSequence by "string" {}
        val charSequenceWithCustomToString = object : CharSequence by "string" {
            override fun toString(): String = "custom"
        }
        renderString(charSequenceWithNoCustomToString) shouldBe "\"string\""
        renderString(charSequenceWithCustomToString) shouldBe "\"string\""
    }

    @Test fun render_primitive() = testAll {
        renderPrimitive(PrimitiveTypes.boolean) shouldBe "true"
        renderPrimitive(PrimitiveTypes.char) shouldBe "*"
        renderPrimitive(PrimitiveTypes.float) shouldBe "42.1"
        renderPrimitive(PrimitiveTypes.double) shouldBe "42.12"

        renderPrimitive(PrimitiveTypes.uByte) shouldBe "39／0x27"
        renderPrimitive(PrimitiveTypes.uShort) shouldBe "40／0x28"
        renderPrimitive(PrimitiveTypes.uInt) shouldBe "41／0x29"
        renderPrimitive(PrimitiveTypes.uLong) shouldBe "42／0x2a"

        renderPrimitive(PrimitiveTypes.byte) shouldBe "39／0x27"
        renderPrimitive(PrimitiveTypes.short) shouldBe "40／0x28"
        renderPrimitive(PrimitiveTypes.int) shouldBe "41／0x29"
        renderPrimitive(PrimitiveTypes.long) shouldBe "42／0x2a"

        renderPrimitive("string") shouldBe "⁉️"
    }

    @Test fun render_primitive_array() = testAll {
        renderPrimitiveArray(PrimitiveTypes.booleanArray) shouldBe "[true, false, false]"
        renderPrimitiveArray(PrimitiveTypes.charArray) shouldBe "[a, r, r, a, y]"
        renderPrimitiveArray(PrimitiveTypes.floatArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        renderPrimitiveArray(PrimitiveTypes.doubleArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"

        renderPrimitiveArray(PrimitiveTypes.uByteArray) shouldBe "0x6172726179"
        renderPrimitiveArray(PrimitiveTypes.uShortArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        renderPrimitiveArray(PrimitiveTypes.uIntArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        renderPrimitiveArray(PrimitiveTypes.uLongArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"

        renderPrimitiveArray(PrimitiveTypes.byteArray) shouldBe "0x6172726179"
        renderPrimitiveArray(PrimitiveTypes.shortArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        renderPrimitiveArray(PrimitiveTypes.intArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        renderPrimitiveArray(PrimitiveTypes.longArray) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
    }

    @Suppress("SpellCheckingInspection")
    @Test fun render_array() = testAll {
        renderArray(arrayOf<String>()) { compression = Always } shouldBe "[]"
        renderArray(arrayOf("string")) { compression = Always } shouldBe "[ \"string\" ]"
        renderArray(arrayOf("string", null)) { compression = Always } shouldBe "[ \"string\", null ]"
        renderArray(arrayOf("string", "line 1\nline 2")) { compression = Always } shouldBe "[ \"string\", \"line 1\\nline 2\" ]"

        renderArray(arrayOf<String>()) { compression = Never } shouldBe "[]"
        renderArray(arrayOf("string")) { compression = Never } shouldBe """
            [
                "string"
            ]
        """.trimIndent()
        renderArray(arrayOf("string", null)) { compression = Never } shouldBe """
            [
                "string",
                null
            ]
        """.trimIndent()
        renderArray(arrayOf("string", "line 1\nline 2")) { compression = Never } shouldBe """
            [
                "string",
                "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()

        renderArray(arrayOf<String>()) shouldBe "[]"
        renderArray(arrayOf("string")) shouldBe "[ \"string\" ]"
        renderArray(arrayOf("string", null)) shouldBe "[ \"string\", null ]"
        renderArray(arrayOf("string", "line 1 -------------------------\nline 2 -------------------------")) shouldBe """
            [
                "string",
                "${"\""}"
                line 1 -------------------------
                line 2 -------------------------
                "${"\""}"
            ]
        """.trimIndent()

        renderArray(arrayOf(ClassWithDefaultToString())) { typing = SimplyTyped } shouldBe """
            [
                !ClassWithDefaultToString {
                    foo: null,
                    bar: !String "baz"
                }
            ]
        """.trimIndent()
    }

    @Suppress("SpellCheckingInspection")
    @Test fun render_collection() = testAll {
        renderCollection(listOf<String>()) { compression = Always } shouldBe "[]"
        renderCollection(listOf("string")) { compression = Always } shouldBe "[ \"string\" ]"
        renderCollection(listOf("string", null)) { compression = Always } shouldBe "[ \"string\", null ]"
        renderCollection(listOf("string", "line 1\nline 2")) { compression = Always } shouldBe "[ \"string\", \"line 1\\nline 2\" ]"

        renderCollection(listOf<String>()) { compression = Never } shouldBe "[]"
        renderCollection(listOf("string")) { compression = Never } shouldBe """
            [
                "string"
            ]
        """.trimIndent()
        renderCollection(listOf("string", null)) { compression = Never } shouldBe """
            [
                "string",
                null
            ]
        """.trimIndent()
        renderCollection(listOf("string", "line 1\nline 2")) { compression = Never } shouldBe """
            [
                "string",
                "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()

        renderCollection(listOf<String>()) shouldBe "[]"
        renderCollection(listOf("string")) shouldBe "[ \"string\" ]"
        renderCollection(listOf("string", null)) shouldBe "[ \"string\", null ]"
        renderCollection(listOf("string", "line 1 -------------------------\nline 2 -------------------------")) shouldBe """
            [
                "string",
                "${"\""}"
                line 1 -------------------------
                line 2 -------------------------
                "${"\""}"
            ]
        """.trimIndent()

        renderCollection(listOf(ClassWithDefaultToString())) { typing = SimplyTyped } shouldBe """
            [
                !ClassWithDefaultToString {
                    foo: null,
                    bar: !String "baz"
                }
            ]
        """.trimIndent()
    }

    @Suppress("SpellCheckingInspection")
    @Test fun render_map() = testAll {
        renderObject(emptyMap<String, Any?>()) { compression = Always } shouldBe "{}"
        renderObject(mapOf("foo" to "string")) { compression = Always } shouldBe "{ foo: \"string\" }"
        renderObject(mapOf("foo" to "string", "bar" to null)) { compression = Always } shouldBe "{ foo: \"string\", bar: null }"
        renderObject(mapOf("foo" to "string", "bar" to "line 1\nline 2")) { compression = Always } shouldBe "{ foo: \"string\", bar: \"line 1\\nline 2\" }"

        renderObject(emptyMap<String, Any?>()) { compression = Never } shouldBe "{}"
        renderObject(mapOf("foo" to "string")) { compression = Never } shouldBe """
            {
                foo: "string"
            }
        """.trimIndent()
        renderObject(mapOf("foo" to "string", "bar" to null)) { compression = Never } shouldBe """
            {
                foo: "string",
                bar: null
            }
        """.trimIndent()
        renderObject(mapOf("foo" to "string", "bar" to "line 1\nline 2")) { compression = Never } shouldBe """
            {
                foo: "string",
                bar: "${"\""}"
                     line 1
                     line 2
                     "${"\""}"
            }
        """.trimIndent()

        renderObject(emptyMap<String, Any?>()) shouldBe "{}"
        renderObject(mapOf("foo" to "string")) shouldBe "{ foo: \"string\" }"
        renderObject(mapOf("foo" to "string", "bar" to null)) shouldBe "{ foo: \"string\", bar: null }"
        renderObject(mapOf("foo" to "string", "bar" to "line 1 -------------------------\nline 2 -------------------------")) shouldBe """
            {
                foo: "string",
                bar: "${"\""}"
                     line 1 -------------------------
                     line 2 -------------------------
                     "${"\""}"
            }
        """.trimIndent()

        renderObject(mapOf("foo" to ClassWithDefaultToString())) { typing = SimplyTyped } shouldBe """
            {
                foo: !ClassWithDefaultToString {
                         foo: null,
                         bar: !String "baz"
                     }
            }
        """.trimIndent()
    }

    @Test fun render_map_with_any_key() = testAll {
        renderObject(mapOf(DataClass() to "foo", null to "bar")) { compression = Always } shouldBe """
            { DataClass(dataProperty=data-property, openBaseProperty=37): "foo", null: "bar" }
        """.trimIndent()
        renderObject(mapOf(DataClass() to "foo", null to "bar")) { compression = Never } shouldBe """
            {
                DataClass(dataProperty=data-property, openBaseProperty=37): "foo",
                null: "bar"
            }
        """.trimIndent()
        renderObject(mapOf(DataClass() to "foo", null to "bar")) shouldBe """
            {
                DataClass(dataProperty=data-property, openBaseProperty=37): "foo",
                null: "bar"
            }
        """.trimIndent()

        renderObject(mapOf(ClassWithDefaultToString() to "foo")) { typing = SimplyTyped } shouldBe """
            {
                !ClassWithDefaultToString { foo: null, bar: !String "baz" }: !String "foo"
            }
        """.trimIndent()
    }

    @Suppress("SpellCheckingInspection")
    @Test fun render_object() = testAll {
        renderObject(ClassWithDefaultToString()) { compression = Always } shouldBe "{ foo: null, bar: \"baz\" }"
        renderObject(ClassWithDefaultToString("string")) { compression = Always } shouldBe "{ foo: \"string\", bar: \"baz\" }"
        renderObject(ClassWithDefaultToString("line 1\nline 2")) { compression = Always } shouldBe "{ foo: \"line 1\\nline 2\", bar: \"baz\" }"
        renderObject(ClassWithDefaultToString(listOf("string", null))) { compression = Always } shouldBe "{ foo: [ \"string\", null ], bar: \"baz\" }"
        renderObject(ClassWithDefaultToString(ClassWithDefaultToString())) {
            compression = Always
        } shouldBe "{ foo: { foo: null, bar: \"baz\" }, bar: \"baz\" }"

        renderObject(ClassWithDefaultToString()) { compression = Never } shouldBe """
            {
                foo: null,
                bar: "baz"
            }
        """.trimIndent()
        renderObject(ClassWithDefaultToString("string")) { compression = Never } shouldBe """
            {
                foo: "string",
                bar: "baz"
            }
        """.trimIndent()
        renderObject(ClassWithDefaultToString("line 1\nline 2")) { compression = Never } shouldBe """
            {
                foo: "${"\""}"
                     line 1
                     line 2
                     "${"\""}",
                bar: "baz"
            }
        """.trimIndent()
        renderObject(ClassWithDefaultToString(listOf("string", null))) { compression = Never } shouldBe """
            {
                foo: [
                         "string",
                         null
                     ],
                bar: "baz"
            }
        """.trimIndent()
        renderObject(ClassWithDefaultToString(ClassWithDefaultToString())) { compression = Never } shouldBe """
            {
                foo: {
                         foo: null,
                         bar: "baz"
                     },
                bar: "baz"
            }
        """.trimIndent()

        renderObject(ClassWithDefaultToString()) shouldBe "{ foo: null, bar: \"baz\" }"
        renderObject(ClassWithDefaultToString("string")) shouldBe "{ foo: \"string\", bar: \"baz\" }"
        renderObject(ClassWithDefaultToString("line 1 -------------------------\nline 2 -------------------------")) shouldBe """
            {
                foo: "${"\""}"
                     line 1 -------------------------
                     line 2 -------------------------
                     "${"\""}",
                bar: "baz"
            }
        """.trimIndent()
        renderObject(ClassWithDefaultToString(listOf("string", null))) shouldBe "{ foo: [ \"string\", null ], bar: \"baz\" }"
        renderObject(ClassWithDefaultToString(ClassWithDefaultToString())) shouldBe "{ foo: { foo: null, bar: \"baz\" }, bar: \"baz\" }"

        renderObject(ClassWithDefaultToString(ClassWithDefaultToString())) { typing = SimplyTyped } shouldBe """
            {
                foo: !ClassWithDefaultToString {
                         foo: null,
                         bar: !String "baz"
                     },
                bar: !String "baz"
            }
        """.trimIndent()
    }

    // render

    @Suppress("SpellCheckingInspection")
    @Test fun render() = testAll {

        val simplyTypedAlwaysCompressingSettings = RenderingSettings.build { typing = SimplyTyped; compression = Always }
        null.render(simplyTypedAlwaysCompressingSettings) shouldBe "null"
        "line 1\nline 2".render(simplyTypedAlwaysCompressingSettings) shouldBe "!String \"line 1\\nline 2\""
        PrimitiveTypes.double.render(simplyTypedAlwaysCompressingSettings) shouldBe "!Double 42.12"
        PrimitiveTypes.doubleArray.render(simplyTypedAlwaysCompressingSettings) shouldBe "!DoubleArray [97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        arrayOf("string", "line 1\nline 2").render(simplyTypedAlwaysCompressingSettings) shouldBe "!Array [ !String \"string\", !String \"line 1\\nline 2\" ]"
        listOf("string", "line 1\nline 2").render(simplyTypedAlwaysCompressingSettings) shouldBe "!List [ !String \"string\", !String \"line 1\\nline 2\" ]"
        mapOf(
            "foo" to "string",
            "bar" to "line 1\nline 2"
        ).render(simplyTypedAlwaysCompressingSettings) shouldBe "!Map { foo: !String \"string\", bar: !String \"line 1\\nline 2\" }"
        mapOf(
            DataClass() to "foo",
            null to "bar"
        ).render(simplyTypedAlwaysCompressingSettings) shouldBe "!Map { !DataClass DataClass(dataProperty=data-property, openBaseProperty=37): !String \"foo\", null: !String \"bar\" }"
        ClassWithDefaultToString(ClassWithDefaultToString()).render(simplyTypedAlwaysCompressingSettings) shouldBe "!ClassWithDefaultToString { foo: !ClassWithDefaultToString { foo: null, bar: !String \"baz\" }, bar: !String \"baz\" }"

        val simplyTypedNeverCompressingSettings = RenderingSettings.build { typing = SimplyTyped; compression = Never }
        null.render(simplyTypedNeverCompressingSettings) shouldBe "null"
        "line 1\nline 2".render(simplyTypedNeverCompressingSettings) shouldBe """
            !String "${"\""}"
            line 1
            line 2
            "${"\""}"
        """.trimIndent()
        PrimitiveTypes.double.render(simplyTypedNeverCompressingSettings) shouldBe "!Double 42.12"
        PrimitiveTypes.doubleArray.render(simplyTypedNeverCompressingSettings) shouldBe "!DoubleArray [97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        arrayOf("string", "line 1\nline 2").render(simplyTypedNeverCompressingSettings) shouldBe """
            !Array [
                !String "string",
                !String "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()
        listOf("string", "line 1\nline 2").render(simplyTypedNeverCompressingSettings) shouldBe """
            !List [
                !String "string",
                !String "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()
        mapOf("foo" to "string", "bar" to "line 1\nline 2").render(simplyTypedNeverCompressingSettings) shouldBe """
            !Map {
                foo: !String "string",
                bar: !String "${"\""}"
                     line 1
                     line 2
                     "${"\""}"
            }
        """.trimIndent()
        mapOf(DataClass() to "foo", null to "bar").render(simplyTypedNeverCompressingSettings) shouldBe """
            !Map {
                !DataClass DataClass(dataProperty=data-property, openBaseProperty=37): !String "foo",
                null: !String "bar"
            }
        """.trimIndent()
        ClassWithDefaultToString(ClassWithDefaultToString()).render(simplyTypedNeverCompressingSettings) shouldBe """
            !ClassWithDefaultToString {
                foo: !ClassWithDefaultToString {
                         foo: null,
                         bar: !String "baz"
                     },
                bar: !String "baz"
            }
        """.trimIndent()

        val untypedAlwaysCompressingSettings = RenderingSettings.build { typing = Untyped; compression = Always }
        null.render(untypedAlwaysCompressingSettings) shouldBe "null"
        "line 1\nline 2".render(untypedAlwaysCompressingSettings) shouldBe "\"line 1\\nline 2\""
        PrimitiveTypes.double.render(untypedAlwaysCompressingSettings) shouldBe "42.12"
        PrimitiveTypes.doubleArray.render(untypedAlwaysCompressingSettings) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        arrayOf("string", "line 1\nline 2").render(untypedAlwaysCompressingSettings) shouldBe "[ \"string\", \"line 1\\nline 2\" ]"
        listOf("string", "line 1\nline 2").render(untypedAlwaysCompressingSettings) shouldBe "[ \"string\", \"line 1\\nline 2\" ]"
        mapOf("foo" to "string", "bar" to "line 1\nline 2").render(untypedAlwaysCompressingSettings) shouldBe "{ foo: \"string\", bar: \"line 1\\nline 2\" }"
        mapOf(DataClass() to "foo", null to "bar").render(untypedAlwaysCompressingSettings) shouldBe """
            { DataClass(dataProperty=data-property, openBaseProperty=37): "foo", null: "bar" }
        """.trimIndent()
        ClassWithDefaultToString(ClassWithDefaultToString()).render(untypedAlwaysCompressingSettings) shouldBe "{ foo: { foo: null, bar: \"baz\" }, bar: \"baz\" }"

        val untypedNeverCompressingSettings = RenderingSettings.build { typing = Untyped; compression = Never }
        null.render(untypedNeverCompressingSettings) shouldBe "null"
        "line 1\nline 2".render(untypedNeverCompressingSettings) shouldBe """
            "${"\""}"
            line 1
            line 2
            "${"\""}"
        """.trimIndent()
        PrimitiveTypes.double.render(untypedNeverCompressingSettings) shouldBe "42.12"
        PrimitiveTypes.doubleArray.render(untypedNeverCompressingSettings) shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        arrayOf("string", "line 1\nline 2").render(untypedNeverCompressingSettings) shouldBe """
            [
                "string",
                "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()
        listOf("string", "line 1\nline 2").render(untypedNeverCompressingSettings) shouldBe """
            [
                "string",
                "${"\""}"
                line 1
                line 2
                "${"\""}"
            ]
        """.trimIndent()
        mapOf("foo" to "string", "bar" to "line 1\nline 2").render(untypedNeverCompressingSettings) shouldBe """
            {
                foo: "string",
                bar: "${"\""}"
                     line 1
                     line 2
                     "${"\""}"
            }
        """.trimIndent()
        mapOf(DataClass() to "foo", null to "bar").render(untypedNeverCompressingSettings) shouldBe """
            {
                DataClass(dataProperty=data-property, openBaseProperty=37): "foo",
                null: "bar"
            }
        """.trimIndent()
        ClassWithDefaultToString(ClassWithDefaultToString()).render(untypedNeverCompressingSettings) shouldBe """
            {
                foo: {
                         foo: null,
                         bar: "baz"
                     },
                bar: "baz"
            }
        """.trimIndent()

        null.render() shouldBe "null"
        "line 1\nline 2".render() shouldBe """
            "${"\""}"
            line 1
            line 2
            "${"\""}"
        """.trimIndent()
        PrimitiveTypes.double.render() shouldBe "42.12"
        PrimitiveTypes.doubleArray.render() shouldBe "[97／0x61, 114／0x72, 114／0x72, 97／0x61, 121／0x79]"
        arrayOf("string", "line 1\nline 2").render() shouldBe "[ \"string\", \"line 1\\nline 2\" ]"
        listOf("string", "line 1\nline 2").render() shouldBe "[ \"string\", \"line 1\\nline 2\" ]"
        mapOf("foo" to "string", "bar" to "line 1\nline 2").render() shouldBe "{ foo: \"string\", bar: \"line 1\\nline 2\" }"
        mapOf(DataClass() to "foo", null to "bar").render() shouldBe """
            {
                DataClass(dataProperty=data-property, openBaseProperty=37): "foo",
                null: "bar"
            }
        """.trimIndent()
        ClassWithDefaultToString(ClassWithDefaultToString()).render() shouldBe "{ foo: { foo: null, bar: \"baz\" }, bar: \"baz\" }"
    }

    @Test fun render_object_with_rendering_to_string() = testAll {
        ClassWithRenderingToString().render() shouldBe "{ foo: null }"
    }

    @Test fun render_option_custom_to_string() = testAll {
        val plainCollectionsAndMapsToStringIgnoringSettings = RenderingSettings.build { customToString = IgnoreForPlainCollectionsAndMaps }
        val toStringIgnoringSettings = RenderingSettings.build { customToString = Ignore }

        CollectionTypes.list.render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe CollectionTypes.list.render(toStringIgnoringSettings)
        ListImplementingSingleton.render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe ListImplementingSingleton.render(toStringIgnoringSettings)
        ClassTypes.map.render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe ClassTypes.map.render(toStringIgnoringSettings)
        MapImplementingSingleton.render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe MapImplementingSingleton.render(toStringIgnoringSettings)

        ClassWithDefaultToString().render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe ClassWithDefaultToString().render(toStringIgnoringSettings)

        ClassWithCustomToString().render(plainCollectionsAndMapsToStringIgnoringSettings) shouldBe "custom toString"
        ClassWithCustomToString().render(toStringIgnoringSettings) shouldBe "{ foo: null }"
    }

    @Test fun render_with_filter() = testAll {
        ClassWithDefaultToString(ClassTypes.triple).render() shouldBe """
            { foo: (39, 40, 41), bar: "baz" }
        """.trimIndent()
        ClassWithDefaultToString(ClassTypes.triple).render { filterProperties { obj, _ -> obj != ClassTypes.triple } } shouldBe """
            { bar: "baz" }
        """.trimIndent()
        ClassWithDefaultToString(ClassTypes.triple).render { filterProperties { _, prop -> prop != "bar" } } shouldBe """
            { foo: (39, 40, 41) }
        """.trimIndent()
    }

    @Test fun render_circular_reference() = testAll {
        SelfReferencingClass().render() shouldContain "selfProperty: <SelfReferencingClass@-?\\d+>".toRegex()
    }

    @Test fun render_function() = testAll {
        ({ }).render() shouldBe when (Platform.Current) {
            Browser, NodeJS -> """
                function () {
                      return Unit_getInstance();
                    }
            """.trimIndent()

            JVM -> """
                () -> kotlin.Unit
            """.trimIndent()

            else -> fail("untested platform")
        }
    }
}
