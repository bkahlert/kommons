package com.bkahlert.kommons.test.fixtures

import com.bkahlert.kommons.decodeFromBase64
import com.bkahlert.kommons.encodeToBase64

/** Defined fixed resource for testing purposes. */
public interface ResourceFixture<out T> {

    /** The name of this fixture. */
    public val name: String

    /** The MIME type of this fixture. */
    public val mimeType: String

    /** The content of this fixture. */
    public val contents: T

    /** The content of this fixture as a [ByteArray]. */
    public val bytes: ByteArray

    /**
     * The fixture as a [data URI](https://en.wikipedia.org/wiki/Data_URI_scheme)
     * of the form `data:[<media type>][;base64],<data>`, e.g. `data:image/gif;base64,…`.
     */
    public val dataURI: String get() = "data:$mimeType;base64,${bytes.encodeToBase64(chunked = false)}"
}

/** Textual resource for testing purposes. */
public open class TextResourceFixture(
    override val name: String,
    override val mimeType: String,
    override val contents: String,
) : ResourceFixture<String> {
    /** Creates an instance with the specified [name], [mimeType], and the specified [bytes]. */
    public constructor(name: String, mimeType: String, vararg bytes: Byte) : this(name, mimeType, bytes.decodeToString())

    override val bytes: ByteArray by lazy { contents.encodeToByteArray() }

    override val dataURI: String by lazy { super.dataURI }
}

/** Binary resource for testing purposes. */
public open class BinaryResourceFixture(
    override val name: String,
    override val mimeType: String,
    override val contents: ByteArray,
) : ResourceFixture<ByteArray> {
    /** Creates an instance with the specified [name], [mimeType], and the specified [base64EncodedString]. */
    public constructor(name: String, mimeType: String, base64EncodedString: String) : this(name, mimeType, base64EncodedString.decodeFromBase64())

    override val bytes: ByteArray get() = contents

    override val dataURI: String by lazy { super.dataURI }
}
