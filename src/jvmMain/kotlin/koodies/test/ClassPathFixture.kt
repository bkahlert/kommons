package koodies.test

import koodies.io.file.quoted
import koodies.io.noSuchFile
import koodies.io.path.asString
import koodies.io.path.copyTo
import koodies.io.path.copyToDirectory
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeBytes
import koodies.io.useClassPath
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * Default implementation of a class path based [Fixture].
 *
 * If the resource addressed by the specified [path] is a
 * readable file then [data] contains its contents.
 * Otherwise accessing that field will throw an exception.
 *
 * @see ClassPathDirectoryFixture
 * @see ClassPathFileFixture
 */
public open class ClassPathFixture(public val path: String) : Fixture {
    override val name: String by lazy { Path.of(path).fileName.asString() }
    override val data: ByteArray by lazy { useClassPath(path) { readBytes() } ?: throw noSuchFile(path) }
}

/**
 * A class path based fixture than is guaranteed to point at
 * an existing directory.
 */
public open class ClassPathDirectoryFixture(path: String) : ClassPathFixture(path) {
    init {
        require(this { isDirectory() }) { "$this is no directory" }
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
        require(this { isRegularFile() }) { "$this is no regular file" }
    }
}


public fun Fixture.copyTo(target: Path): Path = when (this) {
    is ClassPathFixture -> useClassPath(path, fun Path.(): Path = this.copyTo(target))
    else -> target.writeBytes(data)
} ?: error("Error copying ${name.quoted} to ${target.quoted}")

public fun Fixture.copyToDirectory(target: Path): Path = when (this) {
    is ClassPathFixture -> useClassPath(path, fun Path.(): Path = this.copyToDirectory(target))
    else -> target.resolve(name).withDirectoriesCreated().writeBytes(data)
} ?: error("Error copying ${name.quoted} to ${target.quoted}")

public inline operator fun <reified T> ClassPathFixture.invoke(crossinline transform: Path.() -> T): T = useClassPath(path, transform)
    ?: error("Error processing ${path.quoted}")
