package com.bkahlert.kommons.text

import kotlin.test.Test
import kotlin.test.assertEquals

class DelegatingCharSequenceExtensionsTest {

    @Test
    fun assume_fixed_with_string() {
        val foo = "foo"
        val fo: String = foo.dropLast(1)
        assertEquals("fo", fo)
    }

    @Test
    fun assume_fixed_with_char_sequence() {
        val foo: CharSequence = object : CharSequence by "foo" {}
        val fo: CharSequence = foo.dropLast(1)
        assertEquals("fo", fo.toString())
    }

    @Test
    fun assume_fixed_with_string_builder() {
        val foo: StringBuilder = StringBuilder().apply { append("foo") }
        val fo: CharSequence = foo.dropLast(1)

        assertEquals("foo", foo.toString())
        assertEquals("fo", fo.toString())

        foo.insert(0, ">")
        assertEquals(">foo", foo.toString())
        assertEquals("fo", fo.toString())
    }

    @Test
    fun test_delegating() {
        val foo: StringBuilder = StringBuilder().apply { append("foo") }
        val fo: CharSequence = DelegatingCharSequence(foo).dropLast(1)

        assertEquals("foo", foo.toString())
        assertEquals("fo", fo.toString())

        foo.insert(0, ">")
        assertEquals(">foo", foo.toString())
        assertEquals(">f", fo.toString())
    }
}
