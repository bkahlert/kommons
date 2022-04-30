package com.bkahlert.kommons

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassesTest {

    @Test
    fun asString_properties_empty() {
        assertEquals("Foo", nullLine.run { asString() })
    }

    @Test
    fun asString_properties_nullLine() {
        assertEquals("Foo { bar = null ⦀ baz = 42 }", nullLine.run { asString(::bar, ::baz) })
    }

    @Test
    fun asString_properties_singleLine() {
        assertEquals("Foo { bar = line ⦀ baz = 42 }", singleLine.run { asString(::bar, ::baz) })
    }

    @Test
    fun asString_properties_multiLine() {
        assertEquals("""
            Foo {
                    bar = line1
                          line2
                    baz = 42
                }
        """.trimIndent(), multiLine.run { asString(::bar, ::baz) })
    }

    @Test
    fun asString_builder_empty() {
        assertEquals("Foo", nullLine.run { asString {} })
    }

    @Test
    fun asString_builder_nullLine() {
        assertEquals("Foo", nullLine.run { asString {} })
    }

    @Test
    fun asString_builder_singleLine() {
        assertEquals("Foo { bar = line ⦀ baz = 42 }", singleLine.run { asString { ::bar.name to bar; "baz" to baz } })
    }

    @Test
    fun asString_builder_multiline() {
        assertEquals("""
            Foo {
                    bar = line1
                          line2
                    baz = 42
                }
        """.trimIndent(), multiLine.run { asString { ::bar.name to bar; "baz" to baz } })
    }
}

private data class Foo(
    val bar: String? = null,
    val baz: Int = 42,
)

private val nullLine: Foo = Foo(null)
private val singleLine: Foo = Foo("line")
private val multiLine: Foo = Foo("line1\nline2")
