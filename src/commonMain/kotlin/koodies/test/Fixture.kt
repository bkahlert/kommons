package koodies.test

interface Fixture {
    val name: String
    val data: ByteArray
    val text: String get() = data.decodeToString()
}

open class StringFixture(override val name: String, data: String) : Fixture {
    override val data: ByteArray = data.encodeToByteArray()
}

open class ByteFixture(override val name: String, override val data: ByteArray) : Fixture {
    companion object {
        fun unsigned(name: String, vararg data: UByte) = ByteFixture(name, data.toByteArray())
    }
}
