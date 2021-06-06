package koodies.test

import koodies.io.ClassPathFile

public open class ClassPathFixture(pathString: String) : ClassPathFile(pathString), Fixture<ByteArray> {
    override val contents: ByteArray get() = data
}
