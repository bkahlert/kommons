package koodies.test

import koodies.io.classPath
import koodies.io.file.quoted
import koodies.io.noSuchFile
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.baseName
import koodies.io.path.randomPath
import koodies.io.path.toPath
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readBytes

open class ClassPathFixture(val path: String) : Fixture {
    override val name: String by lazy { Path.of(path).fileName.asString() }
    override val data: ByteArray by lazy { classPath(path) { readBytes() } ?: throw noSuchFile(path) }

    open inner class SubFixture(subPath: String) : ClassPathFixture("$path/$subPath")
}

fun ClassPathFixture.copyToTemp(
    base: String = "${name.toPath().baseName}.",
    extension: String = name.toPath().extension,
): Path = copyTo(Locations.Temp.randomPath(base, extension))

fun ClassPathFixture.copyTo(target: Path): Path = classPath(path, fun Path.(): Path = copyTo(target))
    ?: error("Error copying ${path.quoted} to ${target.quoted}")

fun ClassPathFixture.copyToDirectory(target: Path): Path = classPath(path, fun Path.(): Path = copyToDirectory(target))
    ?: error("Error copying ${path.quoted} to directory ${target.quoted}")

inline operator fun <reified T> ClassPathFixture.invoke(crossinline transform: Path.() -> T) = classPath(path, transform)
    ?: error("Error processing ${path.quoted}")
