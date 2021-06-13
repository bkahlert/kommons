package koodies.io.path

import koodies.collections.head
import koodies.text.takeUnlessBlank
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.extension


public class Extensions(private val path: Path) : List<String> by path.fileNameParts.drop(1) {

    public fun add(extensions: String, vararg more: String): Path {
        val normalized = normalizedExtensionString(extensions, *more)
        return path.resolveSibling(path.fileName.pathString + normalized)
    }

    public fun remove(extensions: String, vararg more: String): Path {
        val normalized = normalizedExtensionString(extensions, *more)
        require(path.fileName.pathString.endsWith(normalized))
        return path.resolveSibling(path.fileName.pathString.removeSuffix(normalized))
    }

    public fun hasExtension(extensions: String, vararg more: String): Boolean {
        val normalized = normalizedExtensionString(extensions, *more)
        return path.fileName.pathString.endsWith(normalized)
    }

    /**
     * Returns a string of the form `.ext1[…[.extn]]`.
     */
    private fun normalizedExtensionString(extensions: String, vararg more: String): String =
        normalizedExtensionString(listOf(extensions) + more.toList())

    /**
     * Returns a string of the form `.ext1[…[.extn]]`.
     */
    private fun normalizedExtensionString(extensions: List<String>): String = extensions
        .flatMap { it.split(".") }
        .filter { it.isNotBlank() }
        .joinToString("") { ".${it.lowercase(Locale.getDefault())}" }

    override fun toString(): String =
        normalizedExtensionString(this)
}

private val Path.fileNameParts: List<String> get() = fileName.pathString.split(".")
private val Path.fileBaseName: String get() = fileNameParts.head


/**
 * Returns the base name of the file described by this [Path].
 * Example: `/path/file.pdf` would return `file`.
 *
 * @see [basePath]
 */
public val Path.baseName: Path
    get() = fileName.basePath

/**
 * Returns the base path of the file described by this [Path].
 * Example: `/path/file.pdf` would return `/path/file`.
 *
 * @see [baseName]
 */
public val Path.basePath: Path
    get() {
        return fileName.extensionIndex.let { extensionIndex ->
            if (extensionIndex >= 0) resolveSibling(fileName.pathString.take(extensionIndex))
            else this
        }
    }


/**
 * Contains the position of the period `.` separating the extension from this
 * [asString] path.
 */
public val Path.extensionIndex: Int
    get() = pathString.lastIndexOf(".").takeIf { fileName.pathString.contains(".") } ?: -1

/**
 * Returns the extension of the file described by this [Path].
 * Example: `/path/file.pdf` would return `pdf`.
 *
 * If no extension is present, `null` is returned.
 */
public val Path.extensionOrNull: String?
    get() = extension.takeUnlessBlank()

public val Path.extensions: Extensions
    get() = Extensions(this)

public fun Path.addExtensions(extensions: String, vararg more: String): Path =
    this.extensions.add(extensions, *more)

/**
 * Returns whether this path [Path.getFileName] is the specified [extensions].
 *
 * The extension is compared ignoring the case and an eventually leading period by default.
 *
 * That is, `.ext`, `ext`, `.EXT` and `EXT` are all treated the same way.
 */
public fun Path.hasExtensions(extensions: String, vararg more: String): Boolean =
    this.extensions.hasExtension(extensions, *more)

/**
 * Returns this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
public fun Path.withExtension(extension: String): Path =
    resolveSibling(fileNameWithExtension(extension))

/**
 * Returns the name of the file described by this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
public fun Path.fileNameWithExtension(extension: String): String = "$baseName.$extension"

/**
 * Removes [extensionOrNull] from this [Path].
 *
 * Example: `Path.of("/path/file.foo.bar").removeExtension("bar")` returns path `/path/file.foo`.
 *
 * @throws IllegalArgumentException if the [extensionOrNull] to be removed is not present
 */
public fun Path.removeExtensions(extensions: String, vararg more: String): Path =
    this.extensions.remove(extensions, *more)
