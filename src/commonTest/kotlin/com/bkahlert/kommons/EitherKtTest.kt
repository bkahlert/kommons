package com.bkahlert.kommons

import com.bkahlert.kommons.Either.Left
import com.bkahlert.kommons.Either.Right
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun should_return_value() {
        assertTrue(foo is Left)
        assertEquals(foo.left, Foo)
        assertEquals(foo.value, Foo)

        assertTrue(bar is Right)
        assertEquals(bar.right, Bar)
        assertEquals(bar.value, Bar)
    }

    @Test
    fun should_throw_otherwise() {
        assertEquals(Foo, foo.leftOrThrow())
        assertTrue(kotlin.runCatching { foo.rightOrThrow() }.exceptionOrNull() is NoSuchElementException)
        assertTrue(kotlin.runCatching { bar.leftOrThrow() }.exceptionOrNull() is NoSuchElementException)
        assertEquals(Bar, bar.rightOrThrow())
    }

    @Test
    fun should_return_else_otherwise() {
        assertEquals(Foo, foo.leftOrElse { "$it-else" })
        assertEquals("Foo-else", foo.rightOrElse { "$it-else" })
        assertEquals("Bar-else", bar.leftOrElse { "$it-else" })
        assertEquals(Bar, bar.rightOrElse { "$it-else" })
    }

    @Test
    fun should_return_default_otherwise() {
        assertEquals(Foo, foo.leftOrDefault("default"))
        assertEquals("default", foo.rightOrDefault("default"))
        assertEquals("default", bar.leftOrDefault("default"))
        assertEquals(Bar, bar.rightOrDefault("default"))
    }

    @Test
    fun should_return_null_otherwise() {
        assertEquals(Foo, foo.leftOrNull())
        assertEquals(null, foo.rightOrNull())
        assertEquals(null, bar.leftOrNull())
        assertEquals(Bar, bar.rightOrNull())
    }

    @Test
    fun should_fold() {
        assertEquals("Foo-left", foo.fold({ "$it-left" }, { "$it-right" }))
        assertEquals("Bar-right", bar.fold({ "$it-left" }, { "$it-right" }))
    }

    @Test
    fun should_map() {
        assertEquals(Left("Foo-left"), foo.mapLeft { "$it-left" })
        assertEquals(Left(Foo), foo.mapRight { "$it-right" })
        assertEquals(Right(Bar), bar.mapLeft { "$it-left" })
        assertEquals(Right("Bar-right"), bar.mapRight { "$it-right" })
    }

    @Test
    fun should_do() {
        assertEquals("left", buildList { foo.onLeft { add("left") } }.firstOrNull())
        assertEquals(null, buildList { foo.onRight { add("left") } }.firstOrNull())
        assertEquals(null, buildList { bar.onLeft { add("right") } }.firstOrNull())
        assertEquals("right", buildList { bar.onRight { add("right") } }.firstOrNull())

        assertEquals(foo, foo.onLeft { })
        assertEquals(foo, foo.onRight { })
        assertEquals(bar, bar.onLeft { })
        assertEquals(bar, bar.onRight { })
    }
}
