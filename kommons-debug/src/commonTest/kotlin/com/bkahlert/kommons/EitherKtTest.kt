package com.bkahlert.kommons

import com.bkahlert.kommons.Either.Left
import com.bkahlert.kommons.Either.Right
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class EitherKtTest {

    object Foo {
        override fun toString(): String {
            return "Foo"
        }
    }

    object Bar {
        override fun toString(): String {
            return "Bar"
        }
    }

    val foo: Either<Foo, Bar> = Left(Foo)
    val bar: Either<Foo, Bar> = Right(Bar)

    @Test fun either() = testAll {
        foo.shouldBeInstanceOf<Left<Foo, *>>().value shouldBe Foo
        bar.shouldBeInstanceOf<Right<*, Bar>>().value shouldBe Bar
    }

    @Test fun get_left_or_throw() = testAll {
        foo.getLeftOrThrow() shouldBe Foo
        shouldThrow<NoSuchElementException> { bar.getLeftOrThrow() }
    }

    @Test
    fun get_right_or_throw() = testAll {
        bar.getRightOrThrow() shouldBe Bar
        shouldThrow<NoSuchElementException> { foo.getRightOrThrow() }
    }

    @Test fun get_left_or_else() = testAll {
        foo.getLeftOrElse { "$it-else" } shouldBe Foo
        bar.getLeftOrElse { "$it-else" } shouldBe "Bar-else"
    }

    @Test fun get_right_or_else() = testAll {
        bar.getRightOrElse { "$it-else" } shouldBe Bar
        foo.getRightOrElse { "$it-else" } shouldBe "Foo-else"
    }

    @Test fun get_left_or_default() = testAll {
        foo.getLeftOrDefault("default") shouldBe Foo
        bar.getLeftOrDefault("default") shouldBe "default"
    }

    @Test fun get_right_or_default() = testAll {
        bar.getRightOrDefault("default") shouldBe Bar
        foo.getRightOrDefault("default") shouldBe "default"
    }

    @Test fun get_left_or_null() = testAll {
        foo.getLeftOrNull() shouldBe Foo
        bar.getLeftOrNull() shouldBe null
    }

    @Test fun get_right_or_null() = testAll {
        bar.getRightOrNull() shouldBe Bar
        foo.getRightOrNull() shouldBe null
    }

    @Test fun fold() = testAll {
        foo.fold({ "$it-left" }, { "$it-right" }) shouldBe "Foo-left"
        bar.fold({ "$it-left" }, { "$it-right" }) shouldBe "Bar-right"
    }

    @Test fun map_left() = testAll {
        foo.mapLeft { "$it-left" } shouldBe Left("Foo-left")
        bar.mapLeft { "$it-left" } shouldBe Right(Bar)
    }

    @Test fun map_right() = testAll {
        bar.mapRight { "$it-right" } shouldBe Right("Bar-right")
        foo.mapRight { "$it-right" } shouldBe Left(Foo)
    }

    @Test fun on_left() = testAll {
        buildList { foo.onLeft { add("left") } }.firstOrNull() shouldBe "left"
        buildList { bar.onLeft { add("right") } }.firstOrNull() shouldBe null
        foo.onLeft { } shouldBe foo
        bar.onLeft { } shouldBe bar
    }

    @Test fun on_right() = testAll {
        buildList { bar.onRight { add("right") } }.firstOrNull() shouldBe "right"
        buildList { foo.onRight { add("left") } }.firstOrNull() shouldBe null
        foo.onRight { } shouldBe foo
        bar.onRight { } shouldBe bar
    }

    @Test fun to_result() = testAll {
        foo.mapRight { IllegalArgumentException("message") }.toResult() should {
            it.isSuccess
            it.getOrThrow() shouldBe Foo
        }
        bar.mapRight { IllegalArgumentException("message") }.toResult() should {
            it.isFailure
            shouldThrow<IllegalArgumentException> { it.getOrThrow() }
        }
    }

    @Test fun to_either() = testAll {
        Result.success(Foo).toEither().shouldBeInstanceOf<Left<Foo, *>>().value shouldBe Foo
        Result.failure<Foo>(IllegalArgumentException("message")).toEither().shouldBeInstanceOf<Right<Foo, IllegalArgumentException>>().value should {
            it.shouldBeInstanceOf<IllegalArgumentException>()
            it.message shouldBe "message"
        }
    }

    @Test
    fun get_or_exception() = testAll {
        kotlin.runCatching { 42 }.getOrException() shouldBe (42 to null)
        val ex = IllegalStateException()
        kotlin.runCatching { throw ex }.getOrException<Nothing?>() shouldBe (null to ex)
    }
}
