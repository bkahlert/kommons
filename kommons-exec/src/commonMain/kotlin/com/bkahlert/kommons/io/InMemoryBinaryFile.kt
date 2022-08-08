package com.bkahlert.kommons.io

/**
 * Default implementation of a binary-based [InMemoryFile].
 */
public open class InMemoryBinaryFile(
    override val name: String,
    override val data: ByteArray,
) : InMemoryFile
