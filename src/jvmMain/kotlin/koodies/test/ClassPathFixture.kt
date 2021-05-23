package koodies.test

import koodies.io.InMemoryFile
import koodies.io.noSuchFile
import koodies.io.path.pathString
import koodies.io.useClassPath
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * Default implementation of a class path based [InMemoryFile].
 *
 * If the resource addressed by the specified [path] is a
 * readable file then [data] contains its contents.
 * Otherwise accessing that field will throw an exception.
 *
 * @see ClassPathDirectoryFixture
 * @see ClassPathFileFixture
 */
public open class ClassPathFixture(public val path: String) : Fixture<ByteArray>, InMemoryFile {
    override val name: String by lazy { Path.of(path).fileName.pathString }
    override val data: ByteArray by lazy { useClassPath(path) { readBytes() } ?: throw noSuchFile(path) }
    override val contents: ByteArray get() = data
}

/**
 * A class path based fixture than is guaranteed to point at
 * an existing directory.
 */
public open class ClassPathDirectoryFixture(path: String) : ClassPathFixture(path) {
    init {
        require(this.use { isDirectory() }) { "$this is no directory" }
    }

    public fun dir(dir: String): Dir = Dir(dir)
    public fun file(file: String): File = File(file)

    public open inner class Dir(dir: String) : ClassPathDirectoryFixture("$path/$dir")
    public open inner class File(file: String) : ClassPathFileFixture("$path/$file")
}

/**
 * A class path based fixture than is guaranteed to point to
 * an existing file.
 */
public open class ClassPathFileFixture(path: String) : ClassPathFixture(path) {
    init {
        require(this.use { isRegularFile() }) { "$this is no regular file" }
    }

    public val text: String = data.decodeToString()
}

public inline fun <reified T> ClassPathFixture.use(crossinline transform: Path.() -> T): T =
    useClassPath(path, transform) ?: error("Error processing ${path.quoted}")
