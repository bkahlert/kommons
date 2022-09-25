package com.bkahlert.kommons_deprecated.io

/**
 * Default implementation of a text-based [InMemoryFile].
 *
 * The [data] field contains the text encoded using `UTF-8`.
 */
public open class InMemoryTextFile(
    override val name: String,
    public val text: String,
) : InMemoryFile {
    public constructor(name: String, vararg data: UByte) : this(name, data.toByteArray().decodeToString())

    override val data: ByteArray = text.encodeToByteArray()
}
