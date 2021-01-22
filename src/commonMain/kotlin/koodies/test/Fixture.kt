package koodies.test

/**
 * A well known piece of data especially suited for tests scenarios.
 *
 * Each fixture has a [name] (e.g. `username` or `sample.html`) and
 * provides access to its [data] in a binary form and [text] form.
 *
 * @see TextFixture
 * @see BinaryFixture
 */
interface Fixture {
    val name: String
    val data: ByteArray
    val text: String get() = data.decodeToString()
}

/**
 * Default implementation of a text-based [Fixture].
 *
 * The [data] field contains the text encoded using `UTF-8`.
 */
open class TextFixture(override val name: String, data: String) : Fixture {
    override val data: ByteArray = data.encodeToByteArray()
}

/**
 * Default implementation of a binary-based [Fixture].
 */
open class BinaryFixture(override val name: String, override val data: ByteArray) : Fixture {
    companion object {
        fun unsigned(name: String, vararg data: UByte) = BinaryFixture(name, data.toByteArray())
    }
}
