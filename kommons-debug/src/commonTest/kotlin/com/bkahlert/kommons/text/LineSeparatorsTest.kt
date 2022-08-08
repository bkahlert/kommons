package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators.CR
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.Default
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.LS
import com.bkahlert.kommons.text.LineSeparators.NEL
import com.bkahlert.kommons.text.LineSeparators.PS
import com.bkahlert.kommons.text.LineSeparators.chunkedLineSequence
import com.bkahlert.kommons.text.LineSeparators.chunkedLines
import com.bkahlert.kommons.text.LineSeparators.endsWithLineSeparator
import com.bkahlert.kommons.text.LineSeparators.getFirstLineSeparatorLength
import com.bkahlert.kommons.text.LineSeparators.getFirstLineSeparatorOrNull
import com.bkahlert.kommons.text.LineSeparators.getLeadingLineSeparatorOrNull
import com.bkahlert.kommons.text.LineSeparators.getMostFrequentLineSeparatorOrDefault
import com.bkahlert.kommons.text.LineSeparators.getTrailingLineSeparatorOrNull
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.LineSeparators.isSingleLine
import com.bkahlert.kommons.text.LineSeparators.lineSequence
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.LineSeparators.removeLeadingLineSeparator
import com.bkahlert.kommons.text.LineSeparators.removeTrailingLineSeparator
import com.bkahlert.kommons.text.LineSeparators.startsWithLineSeparator
import com.bkahlert.kommons.text.LineSeparators.unifyLineSeparators
import com.bkahlert.kommons.text.LineSeparators.withLeadingLineSeparator
import com.bkahlert.kommons.text.LineSeparators.withTrailingLineSeparator
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class LineSeparatorsTest {

    @Test fun constants() = testAll {
        CRLF shouldBe "\r\n"
        LF shouldBe "\n"
        CR shouldBe "\r"
        NEL shouldBe "\u0085"
        PS shouldBe "\u2029"
        LS shouldBe "\u2028"
    }

    @Test fun default() = testAll {
        LineSeparators.Common.forAny { it shouldBe Default }
    }

    @Test fun list() = testAll {
        LineSeparators.shouldContainExactly(CRLF, LF, CR)
    }

    @Test fun common() = testAll {
        LineSeparators.Common.shouldContainExactly(CRLF, LF, CR)
    }

    @Test fun unicode() = testAll {
        LineSeparators.Unicode.shouldContainExactly(CRLF, LF, CR, NEL, PS, LS)
    }

    @Test fun uncommon() = testAll {
        LineSeparators.Uncommon.shouldContainExactly(NEL, PS, LS)
    }

    @Test fun common_regex() = testAll {
        stringWithAllLineSeparators.replace(LineSeparators.CommonRegex, "-") shouldBe "foo-foo-foo-foo${NEL}foo${PS}foo${LS}foo"
    }

    @Test fun unicode_regex() = testAll {
        stringWithAllLineSeparators.replace(LineSeparators.UnicodeRegex, "-") shouldBe "foo-foo-foo-foo-foo-foo-foo"
    }

    @Test fun uncommon_regex() = testAll {
        stringWithAllLineSeparators.replace(LineSeparators.UncommonRegex, "-") shouldBe "foo${CRLF}foo${LF}foo${CR}foo-foo-foo-foo"
    }


    @Test fun get_first_line_separator_or_null() = testAll {
        LineSeparators.Unicode.forAll {
            "foo".cs.getFirstLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe null
            "foo${it}$stringWithAllLineSeparators".cs.getFirstLineSeparatorOrNull(* LineSeparators.Unicode) shouldBe it

            "foo".getFirstLineSeparatorOrNull(* LineSeparators.Unicode) shouldBe null
            "foo${it}$stringWithAllLineSeparators".getFirstLineSeparatorOrNull(* LineSeparators.Unicode) shouldBe it
        }
        LineSeparators.Uncommon.forAll {
            "foo${it}$stringWithAllLineSeparators".cs.getFirstLineSeparatorOrNull() shouldBe CRLF
            "foo${it}$stringWithAllLineSeparators".getFirstLineSeparatorOrNull() shouldBe CRLF
        }
    }

    @Test fun get_first_line_separator_length() = testAll {
        "foo".cs.getFirstLineSeparatorLength() shouldBe 0
        "foo".getFirstLineSeparatorLength() shouldBe 0

        "foo${CRLF}$stringWithAllLineSeparators".cs.getFirstLineSeparatorLength(*LineSeparators.Unicode) shouldBe 2
        "foo${CRLF}$stringWithAllLineSeparators".getFirstLineSeparatorLength(*LineSeparators.Unicode) shouldBe 2
        LineSeparators.Unicode.drop(1).forAll {
            "foo${it}$stringWithAllLineSeparators".cs.getFirstLineSeparatorLength(*LineSeparators.Unicode) shouldBe 1
            "foo${it}$stringWithAllLineSeparators".getFirstLineSeparatorLength(*LineSeparators.Unicode) shouldBe 1
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}$stringWithAllLineSeparators".cs.getFirstLineSeparatorLength() shouldBe 2
            "foo${it}$stringWithAllLineSeparators".getFirstLineSeparatorLength() shouldBe 2
        }
    }

    @Test fun get_most_frequent_line_separator_or_default() = testAll {
        "".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe Default
        "foo".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe Default
        LineSeparators.Unicode.asIterable().zipWithNext().forAll { (self, other) ->
            self.cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "${self}foo".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar${other}baz".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar${other}baz${other}".cs.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe other
        }

        "".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe Default
        "foo".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe Default
        LineSeparators.Unicode.asIterable().zipWithNext().forAll { (self, other) ->
            self.getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "${self}foo".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar${other}baz".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe self
            "foo${self}bar${other}baz${other}".getMostFrequentLineSeparatorOrDefault(*LineSeparators.Unicode) shouldBe other
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}foo${it}foo${CR}foo".cs.getMostFrequentLineSeparatorOrDefault() shouldBe CR
            "foo${it}foo${it}foo${CR}foo".getMostFrequentLineSeparatorOrDefault() shouldBe CR
        }
    }

    @Test fun unify_line_separators() = testAll {
        stringWithAllLineSeparators.cs.unifyLineSeparators(LineSeparators.Unicode) shouldBe stringWithDefaultLineSeparators
        stringWithAllLineSeparators.cs.unifyLineSeparators(CRLF, LineSeparators.Unicode) shouldBe stringWithWindowsLineSeparators
        stringWithAllLineSeparators.unifyLineSeparators(LineSeparators.Unicode) shouldBe stringWithDefaultLineSeparators
        stringWithAllLineSeparators.unifyLineSeparators(CRLF, LineSeparators.Unicode) shouldBe stringWithWindowsLineSeparators

        stringWithAllLineSeparators.cs.unifyLineSeparators() shouldBe "foo${Default}foo${Default}foo${Default}foo${NEL}foo${PS}foo${LS}foo"
        stringWithAllLineSeparators.cs.unifyLineSeparators(CRLF) shouldBe "foo${CRLF}foo${CRLF}foo${CRLF}foo${NEL}foo${PS}foo${LS}foo"
        stringWithAllLineSeparators.unifyLineSeparators() shouldBe "foo${Default}foo${Default}foo${Default}foo${NEL}foo${PS}foo${LS}foo"
        stringWithAllLineSeparators.unifyLineSeparators(CRLF) shouldBe "foo${CRLF}foo${CRLF}foo${CRLF}foo${NEL}foo${PS}foo${LS}foo"
    }


    @Test fun line_sequence() = testAll {
        LineSeparators.Unicode.forAny {
            (null as String?)?.cs.lineSequence(*LineSeparators.Unicode).shouldBeEmpty()
            "".cs.lineSequence(*LineSeparators.Unicode).shouldContainExactly("")
            "foo".cs.lineSequence(*LineSeparators.Unicode).shouldContainExactly("foo")
            "foo${it}bar".cs.lineSequence(*LineSeparators.Unicode).shouldContainExactly("foo", "bar")
            "foo${it}bar$it".cs.lineSequence(*LineSeparators.Unicode).shouldContainExactly("foo", "bar", "")

            (null as String?).lineSequence(*LineSeparators.Unicode, keepDelimiters = true).shouldBeEmpty()
            "".cs.lineSequence(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("")
            "foo".cs.lineSequence(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo")
            "foo${it}bar".cs.lineSequence(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo$it", "bar")
            "foo${it}bar$it".cs.lineSequence(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo$it", "bar$it", "")
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}bar".cs.lineSequence().shouldContainExactly("foo${it}bar")
        }
    }

    @Test fun lines() = testAll {
        LineSeparators.Unicode.forAny {
            (null as String?)?.cs.lines(*LineSeparators.Unicode).shouldBeEmpty()
            "".cs.lines(*LineSeparators.Unicode).shouldContainExactly("")
            "foo".cs.lines(*LineSeparators.Unicode).shouldContainExactly("foo")
            "foo${it}bar".cs.lines(*LineSeparators.Unicode).shouldContainExactly("foo", "bar")
            "foo${it}bar$it".cs.lines(*LineSeparators.Unicode).shouldContainExactly("foo", "bar", "")

            (null as String?).lines(*LineSeparators.Unicode, keepDelimiters = true).shouldBeEmpty()
            "".cs.lines(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("")
            "foo".cs.lines(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo")
            "foo${it}bar".cs.lines(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo$it", "bar")
            "foo${it}bar$it".cs.lines(*LineSeparators.Unicode, keepDelimiters = true).shouldContainExactly("foo$it", "bar$it", "")
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}bar".cs.lines().shouldContainExactly("foo${it}bar")
        }
    }

    @Test fun chunked_line_sequence() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            (null as String?)?.cs.chunkedLineSequence(3, *LineSeparators.Unicode) { ">$it<" }.shouldBeEmpty()
            "".cs.chunkedLineSequence(3, *LineSeparators.Unicode) { ">$it<" }.shouldContainExactly("><")
            "foo".cs.chunkedLineSequence(3, *LineSeparators.Unicode) { ">$it<" }.shouldContainExactly(">foo<")
            "12x̅4567${sep}89${sep}".cs.chunkedLineSequence(3, *LineSeparators.Unicode) { ">$it<" }
                .shouldContainExactly(">12x<", ">̅45<", ">67<", ">89<", "><")
        }
        LineSeparators.Uncommon.forAll { sep ->
            "12x̅4567${sep}89${sep}".cs.chunkedLineSequence(3) { ">$it<" }
                .shouldContainExactly(">12x<", ">̅45<", ">67$sep<", ">89$sep<")
        }
    }

    @Test fun chunked_lines() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            (null as String?)?.cs.chunkedLines(3, *LineSeparators.Unicode) { ">$it<" }.shouldBeEmpty()
            "".cs.chunkedLines(3, *LineSeparators.Unicode) { ">$it<" }.shouldContainExactly("><")
            "foo".cs.chunkedLines(3, *LineSeparators.Unicode) { ">$it<" }.shouldContainExactly(">foo<")
            "12x̅4567${sep}89${sep}".cs.chunkedLines(3, *LineSeparators.Unicode) { ">$it<" }
                .shouldContainExactly(">12x<", ">̅45<", ">67<", ">89<", "><")
        }
        LineSeparators.Uncommon.forAll { sep ->
            "12x̅4567${sep}89${sep}".cs.chunkedLines(3) { ">$it<" }
                .shouldContainExactly(">12x<", ">̅45<", ">67$sep<", ">89$sep<")
        }
    }

    @Test fun map_lines() = testAll {
        LineSeparators.Unicode.forAny { sep ->
            "".cs.mapLines(*LineSeparators.Unicode) { ">$it<" } shouldBe "><"
            "foo".cs.mapLines(*LineSeparators.Unicode) { ">$it<" } shouldBe ">foo<"
            "foo${sep}bar".cs.mapLines(*LineSeparators.Unicode) { ">$it<" } shouldBe ">foo<${sep}>bar<"
            "foo${sep}bar$sep".cs.mapLines(*LineSeparators.Unicode) { ">$it<" } shouldBe ">foo<${sep}>bar<${sep}><"
        }

        LineSeparators.Uncommon.forAll { sep ->
            "foo${sep}bar".cs.mapLines { ">$it<" } shouldBe ">foo${sep}bar<"
        }
    }

    @Test fun is_multiline() = testAll {
        (null as String?)?.cs.isMultiline() shouldBe false
        "".cs.isMultiline() shouldBe false
        "foo".cs.isMultiline() shouldBe false
        LineSeparators.Unicode.forAll {
            it.cs.isMultiline(*LineSeparators.Unicode) shouldBe true
            "${it}foo".cs.isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}".cs.isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}bar".cs.isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}bar${it}baz".cs.isMultiline(*LineSeparators.Unicode) shouldBe true
        }
        LineSeparators.Uncommon.forAll {
            it.cs.isMultiline() shouldBe false
            "${it}foo".cs.isMultiline() shouldBe false
            "foo${it}".cs.isMultiline() shouldBe false
            "foo${it}bar".cs.isMultiline() shouldBe false
            "foo${it}bar${it}baz".cs.isMultiline() shouldBe false
        }

        (null as String?).isMultiline() shouldBe false
        "".isMultiline() shouldBe false
        "foo".isMultiline() shouldBe false
        LineSeparators.Unicode.forAll {
            it.isMultiline(*LineSeparators.Unicode) shouldBe true
            "${it}foo".isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}".isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}bar".isMultiline(*LineSeparators.Unicode) shouldBe true
            "foo${it}bar${it}baz".isMultiline(*LineSeparators.Unicode) shouldBe true
        }
        LineSeparators.Uncommon.forAll {
            it.isMultiline() shouldBe false
            "${it}foo".isMultiline() shouldBe false
            "foo${it}".isMultiline() shouldBe false
            "foo${it}bar".isMultiline() shouldBe false
            "foo${it}bar${it}baz".isMultiline() shouldBe false
        }
    }

    @Test fun is_single_line() = testAll {
        (null as String?)?.cs.isSingleLine() shouldBe false
        "".cs.isSingleLine() shouldBe true
        "foo".cs.isSingleLine() shouldBe true
        LineSeparators.Unicode.forAll {
            it.cs.isSingleLine(*LineSeparators.Unicode) shouldBe false
            "${it}foo".cs.isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}".cs.isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}bar".cs.isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}bar${it}baz".cs.isSingleLine(*LineSeparators.Unicode) shouldBe false
        }
        LineSeparators.Uncommon.forAll {
            it.cs.isSingleLine() shouldBe true
            "${it}foo".cs.isSingleLine() shouldBe true
            "foo${it}".cs.isSingleLine() shouldBe true
            "foo${it}bar".cs.isSingleLine() shouldBe true
            "foo${it}bar${it}baz".cs.isSingleLine() shouldBe true
        }

        (null as String?).isSingleLine() shouldBe false
        "".isSingleLine() shouldBe true
        "foo".isSingleLine() shouldBe true
        LineSeparators.Unicode.forAll {
            it.isSingleLine(*LineSeparators.Unicode) shouldBe false
            "${it}foo".isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}".isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}bar".isSingleLine(*LineSeparators.Unicode) shouldBe false
            "foo${it}bar${it}baz".isSingleLine(*LineSeparators.Unicode) shouldBe false
        }
        LineSeparators.Uncommon.forAll {
            it.isSingleLine() shouldBe true
            "${it}foo".isSingleLine() shouldBe true
            "foo${it}".isSingleLine() shouldBe true
            "foo${it}bar".isSingleLine() shouldBe true
            "foo${it}bar${it}baz".isSingleLine() shouldBe true
        }
    }


    @Test fun get_leading_line_separator_or_null() = testAll {
        LineSeparators.Unicode.forAll {
            "foo".cs.getLeadingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe null
            "${it}foo".cs.getLeadingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe it

            "foo".getLeadingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe null
            "${it}foo".getLeadingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe it
        }

        LineSeparators.Uncommon.forAll {
            "${it}foo".cs.getLeadingLineSeparatorOrNull() shouldBe null
            "${it}foo".getLeadingLineSeparatorOrNull() shouldBe null
        }
    }

    @Test fun starts_with_line_separator() = testAll {
        LineSeparators.Unicode.forAll {
            "foo".cs.startsWithLineSeparator(*LineSeparators.Unicode) shouldBe false
            "${it}foo".cs.startsWithLineSeparator(*LineSeparators.Unicode) shouldBe true

            "foo".startsWithLineSeparator(*LineSeparators.Unicode) shouldBe false
            "${it}foo".startsWithLineSeparator(*LineSeparators.Unicode) shouldBe true
        }

        LineSeparators.Uncommon.forAll {
            "${it}foo".cs.startsWithLineSeparator() shouldBe false
            "${it}foo".startsWithLineSeparator() shouldBe false
        }
    }

    @Test fun remove_leading_line_separator() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            "foo".cs should { it.removeLeadingLineSeparator(* LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "${sep}foo".cs should { it.removeLeadingLineSeparator(* LineSeparators.Unicode) shouldBe "foo" }

            "foo" should { it.removeLeadingLineSeparator(* LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "${sep}foo" should { it.removeLeadingLineSeparator(* LineSeparators.Unicode) shouldBe "foo" }
        }

        LineSeparators.Uncommon.forAll { sep ->
            "${sep}foo".cs should { it.removeLeadingLineSeparator() shouldBeSameInstanceAs it }
            "${sep}foo" should { it.removeLeadingLineSeparator() shouldBeSameInstanceAs it }
        }
    }

    @Test fun with_leading_line_separator() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            "foo".cs.withLeadingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBe "${Default}foo"
            "foo".cs.withLeadingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "${CRLF}foo"
            "${sep}foo".cs should { it.withLeadingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "${sep}foo".cs.withLeadingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode).toString() shouldBe "${CRLF}foo"

            "foo".withLeadingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBe "${Default}foo"
            "foo".withLeadingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "${CRLF}foo"
            "${sep}foo" should { it.withLeadingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "${sep}foo".withLeadingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "${CRLF}foo"
        }
        LineSeparators.Uncommon.forAll { sep ->
            "${sep}foo".cs.withLeadingLineSeparator().toString() shouldBe "${Default}${sep}foo"
            "${sep}foo".cs.withLeadingLineSeparator(CRLF).toString() shouldBe "${CRLF}${sep}foo"

            "${sep}foo".withLeadingLineSeparator() shouldBe "${Default}${sep}foo"
            "${sep}foo".withLeadingLineSeparator(CRLF) shouldBe "${CRLF}${sep}foo"
        }
    }


    @Test fun get_trailing_line_separator_or_null() = testAll {
        LineSeparators.Unicode.forAll {
            "foo".cs.getTrailingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe null
            "foo${it}".cs.getTrailingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe it

            "foo".getTrailingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe null
            "foo${it}".getTrailingLineSeparatorOrNull(*LineSeparators.Unicode) shouldBe it
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}".cs.getTrailingLineSeparatorOrNull() shouldBe null
            "foo${it}".getTrailingLineSeparatorOrNull() shouldBe null
        }
    }

    @Test fun ends_with_line_separator() = testAll {
        LineSeparators.Unicode.forAll {
            "foo".cs.endsWithLineSeparator(*LineSeparators.Unicode) shouldBe false
            "foo${it}".cs.endsWithLineSeparator(*LineSeparators.Unicode) shouldBe true

            "foo".endsWithLineSeparator(*LineSeparators.Unicode) shouldBe false
            "foo${it}".endsWithLineSeparator(*LineSeparators.Unicode) shouldBe true
        }

        LineSeparators.Uncommon.forAll {
            "foo${it}".cs.endsWithLineSeparator() shouldBe false
            "foo${it}".endsWithLineSeparator() shouldBe false
        }
    }

    @Test fun remove_trailing_line_separator() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            "foo".cs should { it.removeTrailingLineSeparator(* LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "foo${sep}".cs should { it.removeTrailingLineSeparator(* LineSeparators.Unicode) shouldBe "foo" }

            "foo" should { it.removeTrailingLineSeparator(* LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "foo${sep}" should { it.removeTrailingLineSeparator(* LineSeparators.Unicode) shouldBe "foo" }
        }

        LineSeparators.Uncommon.forAll { sep ->
            "foo${sep}".cs should { it.removeTrailingLineSeparator() shouldBeSameInstanceAs it }
            "foo${sep}" should { it.removeTrailingLineSeparator() shouldBeSameInstanceAs it }
        }
    }

    @Test fun with_trailing_line_separator() = testAll {
        LineSeparators.Unicode.forAll { sep ->
            "foo".cs.withTrailingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBe "foo${Default}"
            "foo".cs.withTrailingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "foo${CRLF}"
            "foo${sep}".cs should { it.withTrailingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "foo${sep}".cs.withTrailingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode).toString() shouldBe "foo${CRLF}"

            "foo".withTrailingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBe "foo${Default}"
            "foo".withTrailingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "foo${CRLF}"
            "foo${sep}" should { it.withTrailingLineSeparator(lineSeparators = LineSeparators.Unicode) shouldBeSameInstanceAs it }
            "foo${sep}".withTrailingLineSeparator(CRLF, lineSeparators = LineSeparators.Unicode) shouldBe "foo${CRLF}"
        }
        LineSeparators.Uncommon.forAll { sep ->
            "foo${sep}".cs.withTrailingLineSeparator().toString() shouldBe "foo${sep}${Default}"
            "foo${sep}".cs.withTrailingLineSeparator(CRLF).toString() shouldBe "foo${sep}${CRLF}"

            "foo${sep}".withTrailingLineSeparator() shouldBe "foo${sep}${Default}"
            "foo${sep}".withTrailingLineSeparator(CRLF) shouldBe "foo${sep}${CRLF}"
        }
    }
}

internal val stringWithAllLineSeparators: String = LineSeparators.Unicode.joinToString("foo", "foo", "foo")
internal val stringWithDefaultLineSeparators = "foo${Default}foo${Default}foo${Default}foo${Default}foo${Default}foo${Default}foo"
internal const val stringWithWindowsLineSeparators = "foo${CRLF}foo${CRLF}foo${CRLF}foo${CRLF}foo${CRLF}foo${CRLF}foo"
