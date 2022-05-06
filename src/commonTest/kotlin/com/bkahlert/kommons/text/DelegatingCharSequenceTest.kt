package com.bkahlert.kommons.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("ReplaceCallWithBinaryOperator")
class DelegatingCharSequenceTest {

    @Test
    fun of_string() {
        val string = "foo"
        val foo = DelegatingCharSequence(string)
        assertFoo(foo)
        assertTrue { foo.toString() === "foo" }
    }

    @Test
    fun of_string_builder() {
        val stringBuilder = StringBuilder("foo")
        val foo = DelegatingCharSequence(stringBuilder)
        assertFoo(foo)
        assertTrue { foo.toString() !== "foo" }
    }

    @Test
    fun of_delegate() {
        val stringBuilder = StringBuilder("foo")
        val delegate = DelegatingCharSequence(stringBuilder)
        val foo = DelegatingCharSequence(delegate)
        assertFoo(foo)
        assertTrue { foo.toString() !== "foo" }

        stringBuilder.append("-bar")
        assertEquals(7, foo.length)
        assertEquals('f', foo[0])
        assertEquals('o', foo[1])
        assertEquals("o-b", foo.subSequence(2, 5).toString())
        assertEquals("foo-bar", foo.toString())
    }

    @Test
    fun of_delegate_subSequence() {
        val stringBuilder = StringBuilder("-foo-")
        val delegate = DelegatingCharSequence(stringBuilder)
        val foo = delegate.subSequence(1, 4)
        assertFoo(foo)
        assertTrue { foo.toString() !== "foo" }

        stringBuilder.insert(0, ">")
        assertEquals(3, foo.length)
        assertEquals('-', foo[0])
        assertEquals('f', foo[1])
        assertEquals("o", foo.subSequence(2, 3).toString())
        assertEquals("-fo", foo.toString())
    }
}

private fun assertFoo(foo: CharSequence) {
    assertEquals(3, foo.length)
    assertTrue { kotlin.runCatching { foo[-1] }.isFailure }
    assertEquals('f', foo[0])
    assertEquals('o', foo[1])
    assertEquals('o', foo[2])
    assertTrue { kotlin.runCatching { foo[3] }.isFailure }
    assertEquals("foo", foo.subSequence(0, 3).toString())
    assertEquals("fo", foo.subSequence(0, 2).toString())
    assertEquals("o", foo.subSequence(1, 2).toString())
    assertEquals("", foo.subSequence(2, 2).toString())
}
