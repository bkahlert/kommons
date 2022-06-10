package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.string
import com.bkahlert.kommons.test.testsOld
import com.bkahlert.kommons.text.Whitespaces.EM_QUAD
import com.bkahlert.kommons.text.Whitespaces.EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.EN_QUAD
import com.bkahlert.kommons.text.Whitespaces.EN_SPACE
import com.bkahlert.kommons.text.Whitespaces.FIGURE_SPACE_FO
import com.bkahlert.kommons.text.Whitespaces.FOUR_PER_EM_SPACE
import com.bkahlert.kommons.text.Whitespaces.HAIR_SPACE
import com.bkahlert.kommons.text.Whitespaces.IDEOGRAPHIC_SPACE
import com.bkahlert.kommons.text.Whitespaces.MEDIUM_MATHEMATICAL_SPACE
import com.bkahlert.kommons.text.Whitespaces.NARROW_NO_BREAK_SPACE_FO
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class WhitespacesTest {

    @Test
    fun `should iterate all whitespaces in order`() {
        expectThat(Whitespaces.joinToString(" ") { "($it)" }).isEqualTo("($SPACE) ($NO_BREAK_SPACE) ($OGHAM_SPACE_MARK) ($EN_QUAD) ($EM_QUAD) ($EN_SPACE) ($EM_SPACE) ($THREE_PER_EM_SPACE) ($FOUR_PER_EM_SPACE) ($SIX_PER_EM_SPACE) ($FIGURE_SPACE_FO) ($PUNCTUATION_SPACE) ($THIN_SPACE) ($HAIR_SPACE) ($NARROW_NO_BREAK_SPACE_FO) ($MEDIUM_MATHEMATICAL_SPACE) ($IDEOGRAPHIC_SPACE)")
    }

    @Nested
    inner class Dict {

        @Test
        fun `should iterate all line break names in order`() {
            expectThat(Whitespaces.Dict.values.joinToString(" ") { "($it)" }).isEqualTo(
                "(SPACE) " +
                    "(NO BREAK SPACE) " +
                    "(OGHAM SPACE MARK) " +
                    "(EN QUAD) " +
                    "(EM QUAD) " +
                    "(EN SPACE) " +
                    "(EM SPACE) " +
                    "(THREE PER EM SPACE) " +
                    "(FOUR PER EM SPACE) " +
                    "(SIX PER EM SPACE) " +
                    "(FIGURE SPACE FO) " +
                    "(PUNCTUATION SPACE) " +
                    "(THIN SPACE) " +
                    "(HAIR SPACE) " +
                    "(NARROW NO BREAK SPACE FO) " +
                    "(MEDIUM MATHEMATICAL SPACE) " +
                    "(IDEOGRAPHIC SPACE)"
            )
        }

        @Test
        fun `should iterate all line breaks in order`() {
            expectThat(Whitespaces.Dict.keys.joinToString(" ") { "($it)" }).isEqualTo("($SPACE) ($NO_BREAK_SPACE) ($OGHAM_SPACE_MARK) ($EN_QUAD) ($EM_QUAD) ($EN_SPACE) ($EM_SPACE) ($THREE_PER_EM_SPACE) ($FOUR_PER_EM_SPACE) ($SIX_PER_EM_SPACE) ($FIGURE_SPACE_FO) ($PUNCTUATION_SPACE) ($THIN_SPACE) ($HAIR_SPACE) ($NARROW_NO_BREAK_SPACE_FO) ($MEDIUM_MATHEMATICAL_SPACE) ($IDEOGRAPHIC_SPACE)")
        }
    }

    @TestFactory
    fun `each whitespace`() = testsOld {
        Whitespaces.Dict.forEach { (whitespace, name) ->
            name all {
                expecting("has length") { whitespace.length } that { isEqualTo(1) }
                expecting("equal to itself") { whitespace.asCodePoint() } that {
                    isNotNull().string.isEqualTo(whitespace)
                }

                expecting("trailing if at end") { "line$whitespace".trailingWhitespaces } that { isEqualTo(whitespace) }
                expecting("not be trailing if not at end") { "line${whitespace}X".trailingWhitespaces } that { isEmpty() }

                expecting("trailing group if left") { "line$whitespace ".trailingWhitespaces } that { isEqualTo(whitespace + SPACE) }
                expecting("trailing group if right") { "line $whitespace".trailingWhitespaces } that { isEqualTo(SPACE + whitespace) }

                expecting("true if trailing") { "line$whitespace".hasTrailingWhitespaces } that { isTrue() }
                expecting("false if not trailing") { "line${whitespace}X".hasTrailingWhitespaces } that { isFalse() }

                expecting("be removed if part of trailing whitespaces") { "line$whitespace".withoutTrailingWhitespaces } that { isEqualTo("line") }
                expecting("not be removed if not part") { "line${whitespace}X".withoutTrailingWhitespaces } that { isEqualTo("line${whitespace}X") }
                expecting("be replaced by single space") { unify("abc${whitespace}def") } that { isEqualTo("abc${SPACE}def") }
            }
        }
    }
}
