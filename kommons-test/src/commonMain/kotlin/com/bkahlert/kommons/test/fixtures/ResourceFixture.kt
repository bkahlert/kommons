package com.bkahlert.kommons.test.fixtures

import com.bkahlert.kommons.decodeFromBase64
import com.bkahlert.kommons.uri.DataUri
import io.ktor.http.ContentType

/** Defined fixed resource for testing purposes. */
public interface ResourceFixture<out T> {

    /** The name of this fixture. */
    public val name: String

    /** The MIME type of this fixture. */
    public val contentType: ContentType

    /** The content of this fixture. */
    public val contents: T

    /** The content of this fixture as a [ByteArray]. */
    public val bytes: ByteArray

    /**
     * The fixture as a [DataUri].
     */
    public val dataUri: DataUri
}

/** Textual resource for testing purposes. */
public class TextResourceFixture(
    override val name: String,
    override val contentType: ContentType,
    override val contents: String,
) : ResourceFixture<String> {

    /** Creates an instance with the specified [name], [contentType], and the specified [bytes]. */
    public constructor(name: String, contentType: ContentType, bytes: ByteArray) : this(name, contentType, bytes.decodeToString())

    override val bytes: ByteArray by lazy { contents.encodeToByteArray() }

    override val dataUri: DataUri by lazy { DataUri(contentType, bytes) }
}

/** Binary resource for testing purposes. */
public class BinaryResourceFixture(
    override val name: String,
    override val contentType: ContentType,
    override val contents: ByteArray,
) : ResourceFixture<ByteArray> {
    /** Creates an instance with the specified [name], [contentType], and the specified [base64EncodedString]. */
    public constructor(name: String, contentType: ContentType, base64EncodedString: String) : this(name, contentType, base64EncodedString.decodeFromBase64())

    override val bytes: ByteArray get() = contents

    override val dataUri: DataUri by lazy { DataUri(contentType, bytes) }
}
