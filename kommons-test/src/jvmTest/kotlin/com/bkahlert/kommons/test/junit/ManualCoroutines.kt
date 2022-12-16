package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.Test

class Coroutines {

    @Test
    fun testCoroutine() {
        build {
            foo()
            bar()
            foo()
            foo()
            bar()
        }.forEach {
            it.execute()
        }
        /*
         Prints:

         Executing block foo
         Executing block bar
         Executing block foo
         Executing block foo
         Executing block bar
         */
    }
}

fun build(block: Builder.() -> Unit): Sequence<Block> {
    val list = mutableListOf<Block>()
    val callback: (Block) -> Unit = {
        list.add(it)
    }
    Builder(callback).block()
    return list.asSequence()
}

data class Block(private val name: String) {
    fun execute() {
        println("Executing block $name")
    }
}


class Builder(
    private val callback: (Block) -> Unit
) {

    fun foo(): Unit {
        val block = Block("foo")
        callback(block)
    }

    fun bar(): Unit {
        val block = Block("bar")
        callback(block)
    }
}
