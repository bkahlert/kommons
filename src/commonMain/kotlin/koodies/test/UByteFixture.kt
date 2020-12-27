package koodies.test

open class UByteFixture(override val name: String, vararg data: UByte) : Fixture {
    override val data: ByteArray = data.toByteArray()
}
