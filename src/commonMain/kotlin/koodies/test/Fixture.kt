package koodies.test

interface Fixture {
    val name: String
    val data: ByteArray
    val text: String get() = data.decodeToString()
}
