package com.bkahlert.kommons.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CharSequencesTest {

    @Test
    fun concatToString_of_string() {
        val string = "foo"
        assertEquals("foo", string.concatToString())
    }

    @Test
    fun concatToString_of_char_sequence() {
        val charSequence = object : CharSequence by "foo" {}
        assertEquals("foo", charSequence.concatToString())
        assertNotEquals("foo", charSequence.toString())
    }
}
