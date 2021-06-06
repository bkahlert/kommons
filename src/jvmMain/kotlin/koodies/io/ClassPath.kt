package koodies.io

import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

/**
 * Default implementation of a class path based [InMemoryFile].
 *
 * If the resource addressed by the specified [pathString] is a
 * readable file then [data] contains its contents.
 * Otherwise accessing that field will throw an exception.
 *
 * @see ClassPathDirectory
 * @see ClassPathFile
 */
public open class ClassPath(public val pathString: String) {
    protected val nativePath: Path = pathString.asPath()
    public val isAbsolute: Boolean get() = nativePath.isAbsolute
    public val fileName: Path get() = nativePath.fileName
    public val parent: ClassPathDirectory? get() = nativePath.parent?.let { ClassPathDirectory(it.pathString) }
    public val requiredParent: ClassPathDirectory get() = parent ?: error("Parent required but $pathString has none.")
    override fun toString(): String = pathString
}

/**
 * A class path based fixture than is guaranteed to point at
 * an existing directory.
 */
public open class ClassPathDirectory(pathString: String) : ClassPath(pathString) {

    init {
        require(use { isDirectory() }) { "$this is no directory" }
    }

    public val name: String get() = fileName.pathString

    public fun dir(path: String): Dir = Dir(path)
    public fun file(path: String): File = File(path)

    public open inner class Dir(dir: String) : ClassPathDirectory(nativePath.resolve(dir).pathString)
    public open inner class File(file: String) : ClassPathFile(nativePath.resolve(file).pathString)
}

/**
 * A class path based fixture than is guaranteed to point to
 * an existing file.
 */
public open class ClassPathFile(pathString: String) : ClassPath(pathString), InMemoryFile {

    init {
        require(use { isRegularFile() }) { "$this is no regular file" }
    }

    override val name: String get() = fileName.pathString
    override val data: ByteArray by lazy { use { readBytes() } }
}

public inline fun <reified T> ClassPath.use(crossinline transform: Path.() -> T): T =
    useClassPath(pathString, transform) ?: error("Error processing ${pathString.quoted}")
