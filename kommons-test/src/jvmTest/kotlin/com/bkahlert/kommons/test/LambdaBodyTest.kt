package com.bkahlert.kommons.test

import io.kotest.assertions.asClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class LambdaBodyTest {

    @Test fun to_string() = testAll {
        LambdaBody("body").toString() shouldBe "body"
        LambdaBody(
            """
            body 1
            body 2
            """.trimIndent()
        ).toString() shouldBe "body 1\nbody 2"
    }

    @Test fun body() = testAll {
        LambdaBody("body").body shouldBe "body"
        LambdaBody(
            """
            body 1
            body 2
        """.trimIndent()
        ).body shouldBe "body 1\nbody 2"
    }

    @Test fun outer_body() = testAll {
        LambdaBody("body").outerBody shouldBe "{ body }"
        LambdaBody(
            """
            body 1
            body 2
            """.trimIndent()
        ).outerBody shouldBe """
            {
                body 1
                body 2
            }
        """.trimIndent()

        LambdaBody("body").outerBody("foo") shouldBe "foo { body }"
        LambdaBody(
            """
            body 1
            body 2
        """.trimIndent()
        ).outerBody("foo") shouldBe """
            foo {
                body 1
                body 2
            }
        """.trimIndent()
    }

    @Test fun guess_name() = testAll {
        "foo".asClue { LambdaBody.guessName(it) shouldBe null }
        "foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "   foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        " \n  foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "foo(\"arg\") {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "   foo(\"arg\") {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        " \n  foo(\"arg\") {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "foo<Type> {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "   foo<Type> {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        " \n  foo<Type> {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "bar(foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "  bar( foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        "bar( \n  foo {".asClue { LambdaBody.guessName(it) shouldBe "foo" }
        " {".asClue { LambdaBody.guessName(it) shouldBe null }
        "    {".asClue { LambdaBody.guessName(it) shouldBe null }
        " \n   {".asClue { LambdaBody.guessName(it) shouldBe null }
    }

    @Test fun parse_or_null__single_line() = validSingleLineLambdaStrings.testAll { code ->
        LambdaBody.parseOrNull("foo", code) shouldBe LambdaBody("body")
    }

    @Test fun parse_or_null__multi_line() = validMultiLineLambdaStrings.testAll { code ->
        LambdaBody.parseOrNull("foo", code) shouldBe LambdaBody(
            """
            body 1
            body 2
            """.trimIndent()
        )
    }

    @Test fun parse_or_null__wrapped_single_line() = validSingleLineLambdaStrings.testAll { code ->
        wrapped(code).forAll { wrappedCode ->
            LambdaBody.parseOrNull("foo", wrappedCode) shouldBe LambdaBody("body")
        }
    }

    @Test fun parse_or_null__wrapped_multi_line() = validMultiLineLambdaStrings.testAll { code ->
        wrapped(code).forAll { wrappedCode ->
            LambdaBody.parseOrNull("foo", wrappedCode) shouldBe LambdaBody(
                """
                body 1
                body 2
                """.trimIndent()
            )
        }
    }

    @Test fun parse_or_null__invalid() = listOf(*invalidSingleLineLambdaStrings, *invalidMultiLineLambdaStrings).testAll { code ->
        LambdaBody.parseOrNull("foo", code) shouldBe null
    }

    @Test fun parse_or_null__missing_name() = listOf(*validSingleLineLambdaStrings, *validMultiLineLambdaStrings).testAll { code ->
        LambdaBody.parseOrNull("bar", code) shouldBe null
    }

    @Test fun parse_or_null__stacktrace_single_line() = testAll {
        LambdaBody.parseOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } })
            .shouldBe(LambdaBody("foo { bar { throw RuntimeException() } }"))

        LambdaBody.parseOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } }, "foo")
            .shouldBe(LambdaBody("bar { throw RuntimeException() }"))

        LambdaBody.parseOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } }, "bar")
            .shouldBe(LambdaBody("throw RuntimeException()"))

        LambdaBody.parseOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } }, "invalid", "bar")
            .shouldBe(LambdaBody("throw RuntimeException()"))

        LambdaBody.parseOrNull(raiseStackTraceElement { foo { bar { throw RuntimeException() } } }, "invalid")
            .shouldBeNull()
    }

    @Test fun parse_or_null__stacktrace_multi_line() = testAll {
        LambdaBody.parseOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }).shouldBe(
            LambdaBody(
                """
                    val now = Instant.now()
                    throw RuntimeException("failed at ${'$'}now")
                """.trimIndent()
            )
        )

        LambdaBody.parseOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }, "foo").shouldBe(
            LambdaBody(
                """
                    bar {
                        val now = Instant.now()
                        throw RuntimeException("failed at ${'$'}now")
                    }
                """.trimIndent()
            )
        )

        LambdaBody.parseOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }, "bar").shouldBe(
            LambdaBody(
                """
                    val now = Instant.now()
                    throw RuntimeException("failed at ${'$'}now")
                """.trimIndent()
            )
        )

        LambdaBody.parseOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }, "invalid", "bar").shouldBe(
            LambdaBody(
                """
                    val now = Instant.now()
                    throw RuntimeException("failed at ${'$'}now")
                """.trimIndent()
            )
        )

        LambdaBody.parseOrNull(raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }, "invalid").shouldBeNull()
    }

    @Test fun parse_or_null__extension() = testAll {
        raiseStackTraceElement { foo { bar { throw RuntimeException() } } }
            .getLambdaBodyOrNull().shouldBe(LambdaBody("foo { bar { throw RuntimeException() } }"))
        raise { foo { bar { throw RuntimeException() } } }
            .getLambdaBodyOrNull().shouldBe(LambdaBody("foo { bar { throw RuntimeException() } }"))

        raiseStackTraceElement { foo { bar { throw RuntimeException() } } }
            .getLambdaBodyOrNull("foo").shouldBe(LambdaBody("bar { throw RuntimeException() }"))
        raise { foo { bar { throw RuntimeException() } } }
            .getLambdaBodyOrNull("foo").shouldBe(LambdaBody("bar { throw RuntimeException() }"))

        raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.getLambdaBodyOrNull().shouldBe(
            LambdaBody(
                """
                    val now = Instant.now()
                    throw RuntimeException("failed at ${'$'}now")
                """.trimIndent()
            )
        )
        raise {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.getLambdaBodyOrNull().shouldBe(
            LambdaBody(
                """
                    val now = Instant.now()
                    throw RuntimeException("failed at ${'$'}now")
                """.trimIndent()
            )
        )

        raiseStackTraceElement {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.getLambdaBodyOrNull("foo").shouldBe(
            LambdaBody(
                """
                    bar {
                        val now = Instant.now()
                        throw RuntimeException("failed at ${'$'}now")
                    }
                """.trimIndent()
            )
        )
        raise {
            foo {
                bar {
                    val now = Instant.now()
                    throw RuntimeException("failed at $now")
                }
            }
        }.getLambdaBodyOrNull("foo").shouldBe(
            LambdaBody(
                """
                    bar {
                        val now = Instant.now()
                        throw RuntimeException("failed at ${'$'}now")
                    }
                """.trimIndent()
            )
        )
    }
}

internal val validSingleLineLambdaStrings = arrayOf(
    "foo { body }",
    "foo{ body }",
    " foo { body }",
    "foo  { body }",
    "foo { body } ",
    "foo {body }",
    "foo { body}",
    """
        foo {
            body
        }
    """.trimIndent(),
    """
        foo {
            body }
    """.trimIndent(),
    """
        foo { body
        }
    """.trimIndent(),
).flatMap {
    listOf(
        it,
        it.replace("foo", "foo (\"arg\")"),
        it.replace("foo", "foo(\"arg\")"),
        it.replace("foo", "foo <Type>"),
        it.replace("foo", "foo<Type>"),
    )
}.toTypedArray()

internal val invalidSingleLineLambdaStrings = arrayOf(
    "foo < body }",
    "foo { body >",
    "foo  body }",
    "foo { body ",
    """
        foo <
            body }
    """.trimIndent(),
    """
        foo {
            body >
    """.trimIndent(),
    """
        foo 
            body }
    """.trimIndent(),
    """
        foo {
            body 
    """.trimIndent(),
)
internal val validMultiLineLambdaStrings = arrayOf(
    """
        foo {
            body 1
            body 2
        }
    """.trimIndent(),
    """
        foo {
          body 1
          body 2
        }
    """.trimIndent()
).flatMap {
    listOf(
        it,
        it.replace("foo", "foo (\"arg\")"),
        it.replace("foo", "foo(\"arg\")"),
        it.replace("foo", "foo\n(\"arg\")"),
        it.replace("foo", "foo <Type>"),
        it.replace("foo", "foo<Type>"),
        it.replace("foo", "foo\n<Type>"),
    )
}.toTypedArray()

internal val invalidMultiLineLambdaStrings = arrayOf(
    """
        foo < 
            body 1 
            body 2 
        }
    """.trimIndent(),
    """
        foo {
            body 1 
            body 2
        >
    """.trimIndent(),
    """
        foo
            body 1 
            body 2
        }
    """.trimIndent(),
    """
        foo {
            body 1 
            body 2 
    """.trimIndent(),
    """
        foo <
            body 1 
            body 2 
            }
    """.trimIndent(),
    """
        foo {
            body 1 
            body 2 >
    """.trimIndent(),
    """
        foo 
            body 1 
            body 2
             }
    """.trimIndent(),
    """
        foo {
            body 1 
            body 2 
    """.trimIndent(),
)

internal fun wrapped(vararg bodyString: String): Array<String> =
    bodyString.flatMap { code ->
        listOf(
            """
                bar { $code }
            """.trimIndent(),
            """
                bar {$code }
            """.trimIndent(),
            """
                bar { $code}
            """.trimIndent(),
            """
                bar { 
                $code }
            """.trimIndent(),
            """
                bar { $code 
                }
            """.trimIndent(),
            """
                bar 
                { $code }
            """.trimIndent(),
            """
                bar 
                {
                  ${code.prependIndent("    ")}
                }
            """.trimIndent(),
            """
                bar { $code }
            """.trimIndent(),
        )
    }.toTypedArray()
