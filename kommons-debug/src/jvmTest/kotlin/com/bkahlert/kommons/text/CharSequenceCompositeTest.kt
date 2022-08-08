package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

class CharSequenceCompositeTest {

    @Test fun length() = testAll {
        charSequenceComposite0 shouldHaveLength 0
        charSequenceComposite1Empty shouldHaveLength 0
        charSequenceComposite1 shouldHaveLength 3
        charSequenceComposite2 shouldHaveLength 6
        charSequenceComposite3 shouldHaveLength 9

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it shouldHaveLength 10
            update("ts")
            it shouldHaveLength 8
        }
    }

    @Test fun get() = testAll {
        shouldThrow<IndexOutOfBoundsException> { charSequenceComposite0[0] }.message shouldBe "index out of range: 0"
        shouldThrow<IndexOutOfBoundsException> { charSequenceComposite1Empty[0] }.message shouldBe "index out of range: 0"
        charSequenceComposite1 should {
            shouldThrow<IndexOutOfBoundsException> { it[-1] }.message shouldBe "index out of range: -1"
            it[0] shouldBe 'f'
            it[1] shouldBe 'o'
            it[2] shouldBe 'o'
            shouldThrow<IndexOutOfBoundsException> { it[3] }.message shouldBe "index out of range: 3"
        }
        charSequenceComposite2 should {
            shouldThrow<IndexOutOfBoundsException> { it[-1] }.message shouldBe "index out of range: -1"
            it[0] shouldBe 'f'
            it[1] shouldBe 'o'
            it[2] shouldBe 'o'
            it[3] shouldBe 'b'
            it[4] shouldBe 'a'
            it[5] shouldBe 'r'
            shouldThrow<IndexOutOfBoundsException> { it[6] }.message shouldBe "index out of range: 6"
        }
        charSequenceComposite3 should {
            shouldThrow<IndexOutOfBoundsException> { it[-1] }.message shouldBe "index out of range: -1"
            it[0] shouldBe 'f'
            it[1] shouldBe 'o'
            it[2] shouldBe 'o'
            it[3] shouldBe 'b'
            it[4] shouldBe 'a'
            it[5] shouldBe 'r'
            it[6] shouldBe 'b'
            it[7] shouldBe 'a'
            it[8] shouldBe 'z'
            shouldThrow<IndexOutOfBoundsException> { it[9] }.message shouldBe "index out of range: 9"
        }

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it[6] shouldBe 't'
            it[7] shouldBe 'b'
            update("ts")
            it[6] shouldBe 'a'
            it[7] shouldBe 'z'
        }
    }

    @Suppress("SpellCheckingInspection")
    @Test fun subSequence() = testAll {
        listOf(charSequenceComposite0, charSequenceComposite1Empty).forAll {
            it.subSequence(0, 0) shouldBe ""
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 1) }
        }
        charSequenceComposite1 should {
            it.subSequence(0, 3).toString() shouldBe "foo"
            it.subSequence(1, 2).toString() shouldBe "o"
            it.subSequence(0, 3) shouldBeSameInstanceAs it
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 4) }
        }
        charSequenceComposite2 should {
            it.subSequence(0, 6).toString() shouldBe "foobar"
            it.subSequence(1, 5).toString() shouldBe "ooba"
            it.subSequence(0, 6) shouldBeSameInstanceAs it
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 7) }
        }
        charSequenceComposite3 should {
            it.subSequence(0, 9).toString() shouldBe "foobarbaz"
            it.subSequence(1, 5).toString() shouldBe "ooba"
            it.subSequence(4, 8).toString() shouldBe "arba"
            it.subSequence(0, 9) shouldBeSameInstanceAs it
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 10) }
        }

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it.subSequence(0, 10).toString() shouldBe "footestbaz"
            it.subSequence(1, 6).toString() shouldBe "ootes"
            it.subSequence(4, 9).toString() shouldBe "estba"
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 11) }
            update("bar")
            it.subSequence(0, 9).toString() shouldBe "foobarbaz"
            it.subSequence(1, 5).toString() shouldBe "ooba"
            it.subSequence(4, 8).toString() shouldBe "arba"
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { it.subSequence(0, 10) }
        }
    }

    @Test fun instantiation() = testAll {
        CharSequenceComposite("foo", "bar") shouldBe CharSequenceComposite(listOf("foo", "bar"))
    }

    @Suppress("SpellCheckingInspection")
    @Test fun to_string() = testAll {
        charSequenceComposite0.toString() shouldBe ""
        charSequenceComposite1Empty.toString() shouldBe ""
        charSequenceComposite1.toString() shouldBe "foo"
        charSequenceComposite2.toString() shouldBe "foobar"
        charSequenceComposite3.toString() shouldBe "foobarbaz"

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it.toString() shouldBe "footestbaz"
            update("bar")
            it.toString() shouldBe "foobarbaz"
        }
    }

    @Test fun equality() = testAll {
        charSequenceComposite0 shouldNotBe CharSequenceDelegate("")
        charSequenceComposite0 shouldBe charSequenceComposite1Empty

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it shouldNotBe charSequenceComposite3
            update("bar")
            it shouldBe charSequenceComposite3
        }
    }

    @Test fun hash_code() = testAll {
        charSequenceComposite0.hashCode() shouldBe charSequenceComposite1Empty.hashCode()

        val (update, testCharSequence) = TestCharSequence("test")
        charSequenceComposite3(testCharSequence) should {
            it.hashCode() shouldNotBe charSequenceComposite3.hashCode()
            update("bar")
            it.hashCode() shouldBe charSequenceComposite3.hashCode()
        }
    }
}

internal val charSequenceComposite0 = CharSequenceComposite()
internal val charSequenceComposite1Empty = CharSequenceComposite("")
internal val charSequenceComposite1 = CharSequenceComposite("foo")
internal val charSequenceComposite2 = CharSequenceComposite("foo", "bar")
internal val charSequenceComposite3 = CharSequenceComposite("foo", "bar", "baz")
internal fun charSequenceComposite3(charSequence: CharSequence) = CharSequenceComposite("foo", charSequence, "baz")
