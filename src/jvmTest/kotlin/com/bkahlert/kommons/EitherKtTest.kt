package com.bkahlert.kommons

import com.bkahlert.kommons.Either.Left
import com.bkahlert.kommons.Either.Right
import com.bkahlert.kommons.EitherKtTest.Return.A
import com.bkahlert.kommons.EitherKtTest.Return.B
import com.bkahlert.kommons.EitherKtTest.Return.C
import com.bkahlert.kommons.EitherKtTest.Return.D

class EitherKtTest {

    object This
    object That

    object Foo
    object Bar : List<Baz> by emptyList()
    object Baz

    sealed interface Return {
        object A : Return
        object B : Return
        object C : Return
        object D : Return
    }

    val foo: Either<This, Throwable> = Right(RuntimeException())
    val bar: Either<List<That>, Throwable> = Right(RuntimeException())

    // TODO implement actual test
    fun testB(): Unit {
        val x1: Either<A, Throwable> = foo.map { A }
        val x2: Return = x1.or { B }
    }

    fun testC(): Unit {
        val x1: Either<Return, Throwable> = foo.map { bar.map { A } or { B } }
        val x: Return = x1.or { B }
    }

    fun testD(): Unit {
        val x1: Either<Return, Throwable> = foo.map { bar.map { A } or { B } }
        val x: Return = foo.map { bar.map { A } or { B } } or { bar.map { C } or { D } }
    }

    fun testE() {
        val either: Either<This, That> = Either.Left(This)
        when (either) {
            is Left -> either.let { it.toString() }
            is Right -> TODO()
        }
    }
}
