package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.Test

class Coroutines {

    @Test
    fun testCoroutine() {
        val operation: (Block) -> Unit = {
            it.execute()
        }

        val blockIterator = build(operation) {
            foo()
            bar()
            foo()
            foo()
            bar()
        }

        while (blockIterator.hasNext()) {
            val block = blockIterator.next()
            block.execute()
        }
    }
}

fun build(operation: (Block) -> Unit, block: Builder.() -> Unit): Iterator<Block> {
    val list = mutableListOf<Block>()
    val callback: (Block) -> Unit = {
        operation(it)
        list.add(it)
    }
    Builder(callback).block()
    return object : Iterator<Block> {
        var i = 0
        override fun hasNext(): Boolean {
            return i < list.size
        }

        override fun next(): Block {
            return list[i++]
        }
    }
}

data class Block(private val name: String) {
    init {
        println("Creating block $name")
    }

    fun execute() {
        println("Executing block $name")
    }
}


class Builder(
    private val callback: (Block) -> Unit,
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
