package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CharSequenceDelegateExtensionsTest {

    @Test fun assume_fixed_with_string() = testAll {
        val foo = "foo"
        val fo: String = foo.dropLast(1)
        fo shouldBe "fo"
    }

    @Test fun assume_fixed_with_char_sequence() = testAll {
        val foo: CharSequence = object : CharSequence by "foo" {}
        val fo: CharSequence = foo.dropLast(1)
        fo.toString() shouldBe "fo"
    }

    @Test fun assume_fixed_with_string_builder() = testAll {
        val foo: StringBuilder = StringBuilder().apply { append("foo") }
        val fo: CharSequence = foo.dropLast(1)

        foo.toString() shouldBe "foo"
        fo.toString() shouldBe "fo"

        foo.insert(0, ">")
        foo.toString() shouldBe ">foo"
        fo.toString() shouldBe "fo"
    }

    @Test fun test_delegate() = testAll {
        val (update, foo) = TestCharSequence("foo")
        val fo: CharSequence = CharSequenceDelegate(foo).dropLast(1)

        foo.toString() shouldBe "foo"
        fo.toString() shouldBe "fo"

        update(">foo")
        foo.toString() shouldBe ">foo"
        fo.toString() shouldBe ">f"
    }
}

fun TestCharSequence(initial: CharSequence): Pair<(CharSequence) -> Unit, CharSequence> {
    val sb = StringBuilder(initial)
    val update: (CharSequence) -> Unit = { sb.clear().append(it) }
    return update to CharSequenceDelegate(sb)
}
