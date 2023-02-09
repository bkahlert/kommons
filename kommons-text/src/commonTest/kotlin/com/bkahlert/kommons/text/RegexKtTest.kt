package com.bkahlert.kommons.text

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators.CR
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.LS
import com.bkahlert.kommons.text.LineSeparators.NEL
import com.bkahlert.kommons.text.LineSeparators.PS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@Suppress(
    "RegExpRepeatedSpace",
    "RegExpSingleCharAlternation",
    "RegExpOctalEscape",
    "RegExpDuplicateAlternationBranch",
    "RegExpEscapedMetaCharacter",
    "RegExpEmptyAlternationBranch",
    "RegExpAnonymousGroup",
    "RegExpUnexpectedAnchor",
    "RegExpDuplicateCharacterInClass",
    "RegExpRedundantNestedCharacterClass",
    "RegExpUnnecessaryNonCapturingGroup",
    "RegExpSuspiciousBackref",
    "RegExpSimplifiable",
    "RegExpRedundantClassElement",
)
class RegexKtTest {

    @Test fun is_group() = testAll {
        Regex("").isGroup shouldBe false
        Regex(" ").isGroup shouldBe false
        Regex("   ").isGroup shouldBe false

        Regex("^[0-9]*foo()$").isGroup shouldBe false
        Regex("()foo").isGroup shouldBe false

        Regex("()()").isGroup shouldBe false
        Regex("(foo)(bar)").isGroup shouldBe false
        Regex("(())()").isGroup shouldBe false
        Regex("(foo(bar)baz)(bar)").isGroup shouldBe false

        Regex("()").isGroup shouldBe true
        Regex("(foo)").isGroup shouldBe true
        Regex("(())").isGroup shouldBe true
        Regex("(foo())").isGroup shouldBe true
        Regex("(()foo)").isGroup shouldBe true
        Regex("(?:)").isGroup shouldBe true
        Regex("(?:foo)").isGroup shouldBe true
        Regex("(?:())").isGroup shouldBe true
        Regex("(?:foo())").isGroup shouldBe true
        Regex("(?:()foo)").isGroup shouldBe true
        Regex("(?<name>)").isGroup shouldBe true
        Regex("(?<name>foo)").isGroup shouldBe true
        Regex("(?<name>())").isGroup shouldBe true
        Regex("(?<name>foo())").isGroup shouldBe true
        Regex("(?<name>()foo)").isGroup shouldBe true

        Regex("(\\()").isGroup shouldBe true
        Regex("(\\(foo)").isGroup shouldBe true
        Regex("(\\)\\(\\))").isGroup shouldBe true
        Regex("()\\)").isGroup shouldBe false
        Regex("\\((())").isGroup shouldBe false
    }

    @Test fun is_named_group() = testAll {
        Regex("()").isNamedGroup shouldBe false
        Regex("(foo)").isNamedGroup shouldBe false
        Regex("(())").isNamedGroup shouldBe false
        Regex("(foo())").isNamedGroup shouldBe false
        Regex("(()foo)").isNamedGroup shouldBe false
        Regex("(?:)").isNamedGroup shouldBe false
        Regex("(?:foo)").isNamedGroup shouldBe false
        Regex("(?:())").isNamedGroup shouldBe false
        Regex("(?:foo())").isNamedGroup shouldBe false
        Regex("(?:()foo)").isNamedGroup shouldBe false
        Regex("(?<name>)").isNamedGroup shouldBe true
        Regex("(?<name>foo)").isNamedGroup shouldBe true
        Regex("(?<name>())").isNamedGroup shouldBe true
        Regex("(?<name>foo())").isNamedGroup shouldBe true
        Regex("(?<name>()foo)").isNamedGroup shouldBe true
    }

    @Test fun is_anonymous_group() = testAll {
        Regex("()").isAnonymousGroup shouldBe false
        Regex("(foo)").isAnonymousGroup shouldBe false
        Regex("(())").isAnonymousGroup shouldBe false
        Regex("(foo())").isAnonymousGroup shouldBe false
        Regex("(()foo)").isAnonymousGroup shouldBe false
        Regex("(?:)").isAnonymousGroup shouldBe true
        Regex("(?:foo)").isAnonymousGroup shouldBe true
        Regex("(?:())").isAnonymousGroup shouldBe true
        Regex("(?:foo())").isAnonymousGroup shouldBe true
        Regex("(?:()foo)").isAnonymousGroup shouldBe true
        Regex("(?<name>)").isAnonymousGroup shouldBe false
        Regex("(?<name>foo)").isAnonymousGroup shouldBe false
        Regex("(?<name>())").isAnonymousGroup shouldBe false
        Regex("(?<name>foo())").isAnonymousGroup shouldBe false
        Regex("(?<name>()foo)").isAnonymousGroup shouldBe false
    }

    @Test fun is_indexed_group() = testAll {
        Regex("()").isIndexedGroup shouldBe true
        Regex("(foo)").isIndexedGroup shouldBe true
        Regex("(())").isIndexedGroup shouldBe true
        Regex("(foo())").isIndexedGroup shouldBe true
        Regex("(()foo)").isIndexedGroup shouldBe true
        Regex("(?:)").isIndexedGroup shouldBe false
        Regex("(?:foo)").isIndexedGroup shouldBe false
        Regex("(?:())").isIndexedGroup shouldBe false
        Regex("(?:foo())").isIndexedGroup shouldBe false
        Regex("(?:()foo)").isIndexedGroup shouldBe false
        Regex("(?<name>)").isIndexedGroup shouldBe false
        Regex("(?<name>foo)").isIndexedGroup shouldBe false
        Regex("(?<name>())").isIndexedGroup shouldBe false
        Regex("(?<name>foo())").isIndexedGroup shouldBe false
        Regex("(?<name>()foo)").isIndexedGroup shouldBe false
    }

    @Test fun group_contents() = testAll {
        Regex("").groupContents shouldBe Regex("")
        Regex(" ").groupContents shouldBe Regex(" ")
        Regex("   ").groupContents shouldBe Regex("   ")

        Regex("^[0-9]*foo()$").groupContents shouldBe Regex("^[0-9]*foo()$")
        Regex("()foo").groupContents shouldBe Regex("()foo")

        Regex("()()").groupContents shouldBe Regex("()()")
        Regex("(foo)(bar)").groupContents shouldBe Regex("(foo)(bar)")
        Regex("(())()").groupContents shouldBe Regex("(())()")
        Regex("(foo(bar)baz)(bar)").groupContents shouldBe Regex("(foo(bar)baz)(bar)")

        Regex("()").groupContents shouldBe Regex("")
        Regex("(foo)").groupContents shouldBe Regex("foo")
        Regex("(())").groupContents shouldBe Regex("()")
        Regex("(foo())").groupContents shouldBe Regex("foo()")
        Regex("(()foo)").groupContents shouldBe Regex("()foo")
        Regex("(?:)").groupContents shouldBe Regex("")
        Regex("(?:foo)").groupContents shouldBe Regex("foo")
        Regex("(?:())").groupContents shouldBe Regex("()")
        Regex("(?:foo())").groupContents shouldBe Regex("foo()")
        Regex("(?:()foo)").groupContents shouldBe Regex("()foo")
        Regex("(?<name>)").groupContents shouldBe Regex("")
        Regex("(?<name>foo)").groupContents shouldBe Regex("foo")
        Regex("(?<name>())").groupContents shouldBe Regex("()")
        Regex("(?<name>foo())").groupContents shouldBe Regex("foo()")
        Regex("(?<name>()foo)").groupContents shouldBe Regex("()foo")

        Regex("(\\()").groupContents shouldBe Regex("\\(")
        Regex("(\\(foo)").groupContents shouldBe Regex("\\(foo")
        Regex("(\\)\\(\\))").groupContents shouldBe Regex("\\)\\(\\)")
        Regex("()\\)").groupContents shouldBe Regex("()\\)")
        Regex("\\((())").groupContents shouldBe Regex("\\((())")
    }


    @Test fun plus() = testAll {
        Regex("foo") + Regex("bar") shouldBe Regex("foobar")
        Regex("foo") + "bar" shouldBe Regex("foobar")
    }

    @Test fun or() = testAll {
        Regex("foo") or Regex("bar") shouldBe Regex("foo|bar")
        Regex("foo") or "bar" shouldBe Regex("foo|bar")
    }

    @Test fun from_literal_alternates() = testAll {
        Regex.fromLiteralAlternates() shouldBe Regex("")
        Regex.fromLiteralAlternates("foo") shouldBe Regex(Regex.escape("foo"))
        Regex.fromLiteralAlternates("foo", "bar") shouldBe Regex("${Regex.escape("foo")}|${Regex.escape("bar")}")

        Regex.fromLiteralAlternates(emptyList()) shouldBe Regex("")
        Regex.fromLiteralAlternates(listOf("foo")) shouldBe Regex(Regex.escape("foo"))
        Regex.fromLiteralAlternates(listOf("foo", "bar")) shouldBe Regex("${Regex.escape("foo")}|${Regex.escape("bar")}")
    }

    @Test fun from_glob() = testAll {
        val input = """
            foo.bar()
            bar[0]++
            baz did throw a RuntimeException
                at SomeFile.kt:42
        """.trimIndent()

        Regex.fromGlob(
            """
                foo.*
                bar[0]++
                baz did **
            """.trimIndent()
        ) should { regex ->
            withClue("matched by glob pattern") {
                regex.matches(input) shouldBe true
            }

            withClue("line breaks matched by any common line break") {
                LineSeparators.Common.forAll { sep ->
                    regex.matches(input.replace("\n", sep)) shouldBe true
                }
            }
            withClue("line breaks matched by no uncommon line break") {
                LineSeparators.Uncommon.forAll { sep ->
                    regex.matches(input.replace("\n", sep)) shouldBe false
                }
            }
        }

        Regex.fromGlob(
            """
                foo.*
                bar[0]++
                baz did **
            """.trimIndent(),
            lineSeparators = LineSeparators.Unicode
        ) should { regex ->
            regex.matches(input) shouldBe true

            withClue("line breaks matched by any unicode line break") {
                LineSeparators.Unicode.forAll { sep ->
                    regex.matches(input.replace("\n", sep)) shouldBe true
                }
            }
        }

        Regex.fromGlob(
            """
                foo.*
                bar[0]++
                baz did *
            """.trimIndent(),
        ) should { regex ->
            withClue("line breaks not matched by simple wildcard") {
                regex.matches(input) shouldBe false
            }
        }

        Regex.fromGlob(
            """
                foo.{}
                bar[0]++
                baz did {{}}
            """.trimIndent(),
            wildcard = "{}",
            multilineWildcard = "{{}}",
        ) should { regex ->
            withClue("matched by glob pattern with custom wildcards") {
                regex.matches(input) shouldBe true
            }
        }
    }

    @Test fun matches_glob() = testAll {
        "foo.bar()".matchesGlob("foo.*") shouldBe true
        "foo.bar()".matchesGlob("foo.{}", wildcard = "{}") shouldBe true

        multilineGlobMatchInput.matchesGlob(
            """
            foo
              .**()
            """.trimIndent()
        ) shouldBe true

        multilineGlobMatchInput.matchesGlob(
            """
            foo
              .{{}}()
            """.trimIndent(),
            multilineWildcard = "{{}}",
        ) shouldBe true

        multilineGlobMatchInput.matchesGlob(
            """
            foo${NEL}  .**()
            """.trimIndent(),
            lineSeparators = LineSeparators.Unicode,
        ) shouldBe true

        multilineGlobMatchInput.matchesGlob(
            """
            foo
              .*()
            """.trimIndent()
        ) shouldBe false
    }

    @Test fun matches_curly() = testAll {
        "foo.bar()".matchesCurly("foo.{}") shouldBe true

        multilineGlobMatchInput.matchesCurly(
            """
            foo
              .{{}}()
            """.trimIndent()
        ) shouldBe true

        multilineGlobMatchInput.matchesCurly(
            """
            foo${NEL}  .{{}}()
            """.trimIndent(),
            lineSeparators = LineSeparators.Unicode,
        ) shouldBe true

        multilineGlobMatchInput.matchesCurly(
            """
            foo
              .{}()
            """.trimIndent()
        ) shouldBe false
    }


    @Test fun group() = testAll {
        Regex("foo").group("other") shouldBe Regex("(?<other>foo)")
        Regex("(foo)").group("other") shouldBe Regex("(?<other>foo)")
        Regex("(?:foo)").group("other") shouldBe Regex("(?<other>foo)")
        Regex("(?<name>foo)").group("other") shouldBe Regex("(?<other>(?<name>foo))")
        shouldThrow<IllegalArgumentException> { Regex("(?<name>foo)").group("in-valid") }

        Regex("foo").group() shouldBe Regex("(?:foo)")
        Regex("(foo)").group() shouldBe Regex("(foo)")
        Regex("(?:foo)").group() shouldBe Regex("(?:foo)")
        Regex("(?<name>foo)").group() shouldBe Regex("(?<name>foo)")
    }

    @Test fun grouped() = testAll {
        Regex("foo").group() shouldBe Regex("(?:foo)")
        Regex("(foo)").group() shouldBe Regex("(foo)")
        Regex("(?:foo)").group() shouldBe Regex("(?:foo)")
        Regex("(?<name>foo)").group() shouldBe Regex("(?<name>foo)")
    }

    @Test fun optional() = testAll {
        Regex("foo").optional() shouldBe Regex("(?:foo)?")
        Regex("(foo)").optional() shouldBe Regex("(foo)?")
        Regex("(?:foo)").optional() shouldBe Regex("(?:foo)?")
        Regex("(?<name>foo)").optional() shouldBe Regex("(?<name>foo)?")
    }

    @Test fun repeat_any() = testAll {
        Regex("foo").repeatAny() shouldBe Regex("(?:foo)*")
        Regex("(foo)").repeatAny() shouldBe Regex("(foo)*")
        Regex("(?:foo)").repeatAny() shouldBe Regex("(?:foo)*")
        Regex("(?<name>foo)").repeatAny() shouldBe Regex("(?<name>foo)*")
    }

    @Test fun repeat_at_least_once() = testAll {
        Regex("foo").repeatAtLeastOnce() shouldBe Regex("(?:foo)+")
        Regex("(foo)").repeatAtLeastOnce() shouldBe Regex("(foo)+")
        Regex("(?:foo)").repeatAtLeastOnce() shouldBe Regex("(?:foo)+")
        Regex("(?<name>foo)").repeatAtLeastOnce() shouldBe Regex("(?<name>foo)+")
    }


    @Test fun repeat() = testAll {
        Regex("foo").repeat(2, 5) shouldBe Regex("(?:foo){2,5}")
        Regex("(foo)").repeat(2, 5) shouldBe Regex("(foo){2,5}")
        Regex("(?:foo)").repeat(2, 5) shouldBe Regex("(?:foo){2,5}")
        Regex("(?<name>foo)").repeat(2, 5) shouldBe Regex("(?<name>foo){2,5}")
    }


    @Test fun named_groups() = testAll {
        val string = "foo bar baz"

        // built-in
        Regex("(ba.)").findAll(string).mapNotNull { it.groups[1] }.map { it.value }.shouldContainExactly("bar", "baz")
        Regex("(?<name>ba.)").findAll(string).mapNotNull { it.groups[1] }.map { it.value }.shouldContainExactly("bar", "baz")

        // extensions
        Regex("(ba.)").findAll(string).mapNotNull { it.groupValue(1) }.shouldContainExactly("bar", "baz")
        Regex("(?<name>ba.)").findAll(string).mapNotNull { it.groupValue(1) }.shouldContainExactly("bar", "baz")

        // name extensions
        shouldThrow<IllegalArgumentException> { Regex("(ba.)").findAll(string).mapNotNull { it.groups["name"] }.toList() }
        Regex("(?<name>ba.)").findAll(string).mapNotNull { it.groups["name"] }.map { it.value }.shouldContainExactly("bar", "baz")
        shouldThrow<IllegalArgumentException> { Regex("(ba.)").findAll(string).mapNotNull { it.groupValue("name") }.toList() }
        Regex("(?<name>ba.)").findAll(string).mapNotNull { it.groupValue("name") }.shouldContainExactly("bar", "baz")
    }


    @Test fun find_all_values() = testAll {
        val string = "foo bar baz"

        Regex("(ba.)").findAllValues(string).shouldContainExactly("bar", "baz")
        Regex("(?<name>ba.)").findAllValues(string).shouldContainExactly("bar", "baz")

        Regex("(ba.)").findAllValues(string, 6).shouldContainExactly("baz")
        Regex("(?<name>ba.)").findAllValues(string, 6).shouldContainExactly("baz")
    }


    @Test fun any_character_regex() = testAll {
        stringWithAllLineSeparators.replace(Regex.AnyCharacterRegex, "-") shouldBe "----------------------------"
        stringWithAllLineSeparators.replace(Regex("."), "-") shouldBe when (Platform.Current) {
            Browser, NodeJS -> "---${CRLF}---${LF}---${CR}-------${PS}---${LS}---"
            else -> "---${CRLF}---${LF}---${CR}---${NEL}---${PS}---${LS}---"
        }
    }

    @Test fun url_regex() = testAll {
        Regex.UrlRegex.findAllValues(
            """
               http://example.net
               https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state
               ftp://edu.gov/download/latest-shit
               file:///some/triple-slash/uri/path/to/file.sh
               mailto:someone@somewhere
               abc://example.net
               ${GifImageFixture.dataUri}
               crap
           """.trimIndent()
        ).toList().shouldContainExactly(
            "http://example.net",
            "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
            "ftp://edu.gov/download/latest-shit",
            "file:///some/triple-slash/uri/path/to/file.sh",
        )
    }


    @Test fun uri_regex() = testAll {
        Regex.UriRegex.findAllValues(
            """
               http://example.net
               https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state
               ftp://edu.gov/download/latest-shit
               file:///some/triple-slash/uri/path/to/file.sh
               mailto:someone@somewhere
               abc://example.net
               ${GifImageFixture.dataUri}
               crap
           """.trimIndent()
        ).toList().shouldContainExactly(
            "http://example.net",
            "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
            "ftp://edu.gov/download/latest-shit",
            "file:///some/triple-slash/uri/path/to/file.sh",
            "mailto:someone@somewhere",
            "abc://example.net",
            GifImageFixture.dataUri,
        )
    }
}

internal val multilineGlobMatchInput = """
foo
  .bar()
  .baz()
""".trimIndent()
