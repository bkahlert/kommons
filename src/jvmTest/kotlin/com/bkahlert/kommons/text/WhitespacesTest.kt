package com.bkahlert.kommons.text

import com.bkahlert.kommons.asCodePointSequence
import com.bkahlert.kommons.string
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.text.Whitespaces.EM_QUAD
import com.bkahlert.kommons.text.Whitespaces.EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.EN_QUAD
import com.bkahlert.kommons.text.Whitespaces.EN_SPACE
import com.bkahlert.kommons.text.Whitespaces.FIGURE_SPACE
import com.bkahlert.kommons.text.Whitespaces.FOUR_PER_EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.HAIR_SPACE
import com.bkahlert.kommons.text.Whitespaces.IDEOGRAPHIC_SPACE
import com.bkahlert.kommons.text.Whitespaces.MEDIUM_MATHEMATICAL_SPACE
import com.bkahlert.kommons.text.Whitespaces.NARROW_NO_BREAK_SPACE
import com.bkahlert.kommons.text.Whitespaces.NO_BREAK_SPACE
import com.bkahlert.kommons.text.Whitespaces.OGHAM_SPACE_MARK
import com.bkahlert.kommons.text.Whitespaces.PUNCTUATION_SPACE
import com.bkahlert.kommons.text.Whitespaces.SIX_PER_EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.SPACE
import com.bkahlert.kommons.text.Whitespaces.THIN_SPACE
import com.bkahlert.kommons.text.Whitespaces.THREE_PER_EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.hasTrailingWhitespaces
import com.bkahlert.kommons.text.Whitespaces.trailingWhitespaces
import com.bkahlert.kommons.text.Whitespaces.unify
import com.bkahlert.kommons.text.Whitespaces.withoutTrailingWhitespaces
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldHaveLength
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class WhitespacesTest {

    @Test fun iterate() = test {
        Whitespaces.toList().shouldContainExactly(
            SPACE,
            NO_BREAK_SPACE,
            OGHAM_SPACE_MARK,
            EN_QUAD,
            EM_QUAD,
            EN_SPACE,
            EM_SPACE,
            THREE_PER_EM_SPACE,
            FOUR_PER_EM_SPACE,
            SIX_PER_EM_SPACE,
            FIGURE_SPACE,
            PUNCTUATION_SPACE,
            THIN_SPACE,
            HAIR_SPACE,
            NARROW_NO_BREAK_SPACE,
            MEDIUM_MATHEMATICAL_SPACE,
            IDEOGRAPHIC_SPACE
        )
    }

    @Test fun dict() = test {
        Whitespaces.Dict.keys.toList().shouldContainExactly(
            SPACE,
            NO_BREAK_SPACE,
            OGHAM_SPACE_MARK,
            EN_QUAD,
            EM_QUAD,
            EN_SPACE,
            EM_SPACE,
            THREE_PER_EM_SPACE,
            FOUR_PER_EM_SPACE,
            SIX_PER_EM_SPACE,
            FIGURE_SPACE,
            PUNCTUATION_SPACE,
            THIN_SPACE,
            HAIR_SPACE,
            NARROW_NO_BREAK_SPACE,
            MEDIUM_MATHEMATICAL_SPACE,
            IDEOGRAPHIC_SPACE
        )
        Whitespaces.Dict.values.toList().shouldContainExactly(
            "SPACE",
            "NO-BREAK SPACE",
            "OGHAM SPACE MARK",
            "EN QUAD",
            "EM QUAD",
            "EN SPACE",
            "EM SPACE",
            "THREE-PER-EM SPACE",
            "FOUR-PER-EM SPACE",
            "SIX-PER-EM SPACE",
            "FIGURE SPACE",
            "PUNCTUATION SPACE",
            "THIN SPACE",
            "HAIR SPACE",
            "NARROW NO-BREAK SPACE",
            "MEDIUM MATHEMATICAL SPACE",
            "IDEOGRAPHIC SPACE",
        )
    }

    @TestFactory
    fun `each whitespace`() = Whitespaces.Dict.testEach { (whitespace, name) ->
        withClue(name) {
            withClue("has length") { whitespace shouldHaveLength 1 }
            withClue("equal to itself") { whitespace.asCodePointSequence().singleOrNull()?.string shouldBe whitespace }

            withClue("trailing if at end") { "line$whitespace".trailingWhitespaces shouldBe whitespace }
            withClue("not be trailing if not at end") { "line${whitespace}X".trailingWhitespaces.shouldBeEmpty() }

            withClue("trailing group if left") { "line$whitespace ".trailingWhitespaces shouldBe whitespace + SPACE }
            withClue("trailing group if right") { "line $whitespace".trailingWhitespaces shouldBe SPACE + whitespace }

            withClue("true if trailing") { "line$whitespace".hasTrailingWhitespaces shouldBe true }
            withClue("false if not trailing") { "line${whitespace}X".hasTrailingWhitespaces shouldBe false }

            withClue("be removed if part of trailing whitespaces") { "line$whitespace".withoutTrailingWhitespaces shouldBe "line" }
            withClue("not be removed if not part") { "line${whitespace}X".withoutTrailingWhitespaces shouldBe "line${whitespace}X" }
            withClue("be replaced by single space") { unify("abc${whitespace}def") shouldBe "abc${SPACE}def" }
        }
    }
}
