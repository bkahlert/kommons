package koodies.io

import koodies.io.file.quoted
import koodies.io.path.asPath
import koodies.io.path.copyTo
import koodies.io.path.copyToDirectory
import koodies.io.path.extensionOrNull
import koodies.io.path.pathString
import koodies.text.quoted
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
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
    public val parentOrNull: ClassPathDirectory? get() = nativePath.parent?.let { ClassPathDirectory(it.pathString) }
    public val parent: ClassPathDirectory get() = parentOrNull ?: error("Parent required but $pathString has none.")
    override fun toString(): String = pathString

    /**
     * Copies this resource to the specified [target].
     *
     * @see copyToDirectory
     * @see copyToTemp
     */
    public fun copyTo(target: Path, overwrite: Boolean = false): Path =
        useClassPath(pathString, fun Path.(): Path = copyTo(target, overwrite = overwrite))
            ?: error("Error copying ${fileName.quoted} to ${target.quoted}")

    /**
     * Copies this resource to the specified [target] directory.
     *
     * @see copyTo
     * @see copyToTemp
     */
    public fun copyToDirectory(target: Path): Path =
        useClassPath(pathString, fun Path.(): Path = this.copyToDirectory(target))
            ?: error("Error copying ${fileName.quoted} to ${target.quoted}")

    /**
     * Copies resource to a temporary directory—the name based on the
     * optional [base] and [extension].
     */
    public fun copyToTemp(
        base: String = nativePath.nameWithoutExtension,
        extension: String = nativePath.extensionOrNull?.let { ".$it" } ?: "",
    ): Path = copyTo(Koodies.FilesTemp.tempFile(base, extension), overwrite = true)
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
public open class ClassPathFile(pathString: String) : ClassPath(pathString) {

    init {
        require(use { isRegularFile() }) { "$this is no regular file" }
    }

    public val name: String get() = fileName.pathString
    public val data: ByteArray by lazy { use { readBytes() } }
}

public inline fun <reified T> ClassPath.use(crossinline transform: Path.() -> T): T =
    useClassPath(pathString, transform) ?: error("Error processing ${pathString.quoted}")

public inline val ClassPathFile.text: String
    get() = data.decodeToString()
