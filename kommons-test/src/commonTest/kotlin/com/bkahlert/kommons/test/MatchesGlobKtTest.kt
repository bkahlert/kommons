@file:Suppress("SpellCheckingInspection")

package com.bkahlert.kommons.test

import com.bkahlert.kommons.text.LineSeparators
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MatchesGlobKtTest {

    @Test fun should_match_glob() {
        shouldNotThrowAny {
            multilineGlobMatchInput shouldMatchGlob """
                foo
                  .**()
            """.trimIndent()
        }
    }

    @Test fun should_match_glob__failure() {
        shouldThrow<AssertionError> {
            multilineGlobMatchInput shouldMatchGlob """
                foo
                  .*()
            """.trimIndent()
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should match the following glob pattern (wildcard: *, multiline wildcard: **, line separators: CRLF (\r\n), LF (\n), CR (\r)):
            ""${'"'}
            foo
              .*()
            ""${'"'}
        """.trimIndent()
    }


    @Test fun should_not_match_glob() {
        shouldNotThrowAny {
            multilineGlobMatchInput shouldNotMatchGlob """
                foo
                  .*()
            """.trimIndent()
        }
    }

    @Test fun should_not_match_glob__failure() {
        shouldThrow<AssertionError> {
            multilineGlobMatchInput shouldNotMatchGlob """
                foo
                  .**()
            """.trimIndent()
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should not match the following glob pattern (wildcard: *, multiline wildcard: **, line separators: CRLF (\r\n), LF (\n), CR (\r)):
            ""${'"'}
            foo
              .**()
            ""${'"'}
        """.trimIndent()
    }


    @Test fun match_glob() {
        shouldNotThrowAny {
            multilineGlobMatchInput should matchGlob(
                """
                foo
                {}.{{}}()
            """.trimIndent(),
                wildcard = "{}",
                multilineWildcard = "{{}}",
                *LineSeparators.Unicode, "🫠",
            )
        }
    }

    @Test fun match_glob__failure() {
        @Suppress("LongLine")
        shouldThrow<AssertionError> {
            multilineGlobMatchInput should matchGlob(
                """
                foo
                {}.{}()
            """.trimIndent(),
                wildcard = "{}",
                multilineWildcard = "{{}}",
                *LineSeparators.Unicode, "🫠",
            )
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should match the following glob pattern (wildcard: {}, multiline wildcard: {{}}, line separators: CRLF (\r\n), LF (\n), CR (\r), NEL (\u0085), PS (\u2029), LS (\u2028), Unknown (0xf09faba0)):
            ""${'"'}
            foo
            {}.{}()
            ""${'"'}
        """.trimIndent()
    }

    @Test fun match_glob__singleLine_failure() {
        @Suppress("LongLine")
        shouldThrow<AssertionError> {
            "foo.bar()" should matchGlob("bar.{}")
        }.message shouldBe """
            "foo.bar()"
            should match the following glob pattern (wildcard: *, multiline wildcard: **, line separators: CRLF (\r\n), LF (\n), CR (\r)):
            "bar.{}"
        """.trimIndent()
    }


    @Test fun should_match_curly() {
        shouldNotThrowAny {
            multilineGlobMatchInput shouldMatchCurly """
                foo
                  .{{}}()
            """.trimIndent()
        }
    }

    @Test fun should_match_curly__failure() {
        shouldThrow<AssertionError> {
            multilineGlobMatchInput shouldMatchCurly """
                foo
                  .{}()
            """.trimIndent()
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should match the following curly pattern (line separators: CRLF (\r\n), LF (\n), CR (\r)):
            ""${'"'}
            foo
              .{}()
            ""${'"'}
        """.trimIndent()
    }


    @Test fun should_not_match_curly() {
        shouldNotThrowAny {
            multilineGlobMatchInput shouldNotMatchCurly """
                foo
                  .{}()
            """.trimIndent()
        }
    }

    @Test fun should_not_match_curly__failure() {
        shouldThrow<AssertionError> {
            multilineGlobMatchInput shouldNotMatchCurly """
                foo
                  .{{}}()
            """.trimIndent()
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should not match the following curly pattern (line separators: CRLF (\r\n), LF (\n), CR (\r)):
            ""${'"'}
            foo
              .{{}}()
            ""${'"'}
        """.trimIndent()
    }


    @Test fun match_curly() {
        shouldNotThrowAny {
            multilineGlobMatchInput should matchCurly(
                """
                foo
                {}.{{}}()
            """.trimIndent(),
                *LineSeparators.Unicode, "🫠",
            )
        }
    }

    @Test fun match_curly__failure() {
        @Suppress("LongLine")
        shouldThrow<AssertionError> {
            multilineGlobMatchInput should matchCurly(
                """
                foo
                {}.{}()
            """.trimIndent(),
                *LineSeparators.Unicode, "🫠",
            )
        }.message shouldBe """
            ""${'"'}
            foo
              .bar()
              .baz()
            ""${'"'}
            should match the following curly pattern (line separators: CRLF (\r\n), LF (\n), CR (\r), NEL (\u0085), PS (\u2029), LS (\u2028), Unknown (0xf09faba0)):
            ""${'"'}
            foo
            {}.{}()
            ""${'"'}
        """.trimIndent()
    }

    @Test fun match_curly__singleLine_failure() {
        @Suppress("LongLine")
        shouldThrow<AssertionError> {
            "foo.bar()" should matchCurly("bar.{}")
        }.message shouldBe """
            "foo.bar()"
            should match the following curly pattern (line separators: CRLF (\r\n), LF (\n), CR (\r)):
            "bar.{}"
        """.trimIndent()
    }
}


internal val multilineGlobMatchInput = """
foo
  .bar()
  .baz()
""".trimIndent()
