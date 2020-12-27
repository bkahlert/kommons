package koodies.test

open class Utf8Fixture(override val name: String, data: String) : Fixture {
    override val data: ByteArray = data.encodeToByteArray()
}
