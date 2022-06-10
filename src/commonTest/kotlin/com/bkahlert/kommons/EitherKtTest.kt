package com.bkahlert.kommons

import com.bkahlert.kommons.Either.Left
import com.bkahlert.kommons.Either.Right
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.test.tests
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

    @Test
    fun should_return_value() = test {
        foo.shouldBeInstanceOf<Left<Foo, *>>() should {
            it.left shouldBe Foo
            it.value shouldBe Foo
        }
        bar.shouldBeInstanceOf<Right<*, Bar>>() should {
            it.right shouldBe Bar
            it.value shouldBe Bar
        }
    }

    @Test
    fun should_throw_otherwise() = tests {
        foo.leftOrThrow() shouldBe Foo
        shouldThrow<NoSuchElementException> { foo.rightOrThrow() }
        shouldThrow<NoSuchElementException> { bar.leftOrThrow() }
        bar.rightOrThrow() shouldBe Bar
    }

    @Test
    fun should_return_else_otherwise() = tests {
        foo.leftOrElse { "$it-else" } shouldBe Foo
        foo.rightOrElse { "$it-else" } shouldBe "Foo-else"
        bar.leftOrElse { "$it-else" } shouldBe "Bar-else"
        bar.rightOrElse { "$it-else" } shouldBe Bar
    }

    @Test
    fun should_return_default_otherwise() = tests {
        foo.leftOrDefault("default") shouldBe Foo
        foo.rightOrDefault("default") shouldBe "default"
        bar.leftOrDefault("default") shouldBe "default"
        bar.rightOrDefault("default") shouldBe Bar
    }

    @Test
    fun should_return_null_otherwise() = tests {
        foo.leftOrNull() shouldBe Foo
        foo.rightOrNull() shouldBe null
        bar.leftOrNull() shouldBe null
        bar.rightOrNull() shouldBe Bar
    }

    @Test
    fun should_fold() = tests {
        foo.fold({ "$it-left" }, { "$it-right" }) shouldBe "Foo-left"
        bar.fold({ "$it-left" }, { "$it-right" }) shouldBe "Bar-right"
    }

    @Test
    fun should_map() = tests {
        foo.mapLeft { "$it-left" } shouldBe Left("Foo-left")
        foo.mapRight { "$it-right" } shouldBe Left(Foo)
        bar.mapLeft { "$it-left" } shouldBe Right(Bar)
        bar.mapRight { "$it-right" } shouldBe Right("Bar-right")
    }

    @Test
    fun should_do() = tests {
        buildList { foo.onLeft { add("left") } }.firstOrNull() shouldBe "left"
        buildList { foo.onRight { add("left") } }.firstOrNull() shouldBe null
        buildList { bar.onLeft { add("right") } }.firstOrNull() shouldBe null
        buildList { bar.onRight { add("right") } }.firstOrNull() shouldBe "right"

        foo.onLeft { } shouldBe foo
        foo.onRight { } shouldBe foo
        bar.onLeft { } shouldBe bar
        bar.onRight { } shouldBe bar
    }
}
