package koodies.test

import koodies.io.classPath
import koodies.io.file.quoted
import koodies.io.noSuchFile
import koodies.io.path.asString
import koodies.io.path.copyTo
import koodies.io.path.copyToDirectory
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.readBytes

open class ClassPathFixture(val path: String) : Fixture {
    override val name: String by lazy { Path.of(path).fileName.asString() }
    override val data: ByteArray by lazy { classPath(path) { readBytes() } ?: throw noSuchFile(path) }

    open inner class SubFixture(subPath: String) : ClassPathFixture("$path/$subPath")
}

fun ClassPathFixture.copyTo(target: Path): Path = classPath(path, fun Path.(): Path = this.copyTo(target))
    ?: error("Error copying ${path.quoted} to ${target.quoted}")

fun ClassPathFixture.copyToDirectory(target: Path): Path = classPath(path, fun Path.(): Path = this.copyToDirectory(target))
    ?: error("Error copying ${path.quoted} to directory ${target.quoted}")

inline operator fun <reified T> ClassPathFixture.invoke(crossinline transform: Path.() -> T) = classPath(path, transform)
    ?: error("Error processing ${path.quoted}")
