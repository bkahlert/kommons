package koodies.io.path

import koodies.collections.head
import koodies.text.withoutSuffix
import java.nio.file.Path
import kotlin.io.path.extension


class Extensions(private val path: Path) : List<String> by path.fileNameParts.drop(1) {

    fun add(extensions: String, vararg more: String): Path {
        val normalized = normalizedExtensionString(extensions, *more)
        return path.resolveSibling(path.fileName.asString() + normalized)
    }

    fun remove(extensions: String, vararg more: String): Path {
        val normalized = normalizedExtensionString(extensions, *more)
        require(path.fileName.asString().endsWith(normalized))
        return path.resolveSibling(path.fileName.asString().withoutSuffix(normalized))
    }

    fun withoutLast(n: Int = 1): Path =
        path.extensionOrNull?.let { path.removeExtensions(it) } ?: path

    fun hasExtension(extensions: String, vararg more: String): Boolean {
        val normalized = normalizedExtensionString(extensions, *more)
        return path.fileName.asString().endsWith(normalized)
    }

    /**
     * Returns a string of the form `.ext1[...[.extn]]`.
     */
    private fun normalizedExtensionString(extensions: String, vararg more: String): String =
        normalizedExtensionString(listOf(extensions) + more.toList())

    /**
     * Returns a string of the form `.ext1[...[.extn]]`.
     */
    private fun normalizedExtensionString(extensions: List<String>): String = extensions
        .flatMap { it.split(".") }
        .filter { it.isNotBlank() }
        .joinToString("") { ".${it.toLowerCase()}" }

    override fun toString(): String =
        normalizedExtensionString(this)
}

private val Path.fileNameParts: List<String> get() = fileName.asString().split(".")
private val Path.fileBaseName: String get() = fileNameParts.head


/**
 * Returns the base name of the file described by this [Path].
 * Example: `/path/file.pdf` would return `file`.
 *
 * @see [basePath]
 */
val Path.baseName: Path
    get() = fileName.basePath

/**
 * Returns the base path of the file described by this [Path].
 * Example: `/path/file.pdf` would return `/path/file`.
 *
 * @see [baseName]
 */
val Path.basePath: Path
    get() {
        return fileName.extensionIndex.let { extensionIndex ->
            if (extensionIndex >= 0) resolveSibling(fileName.asString().take(extensionIndex))
            else this
        }
    }


/**
 * Contains the position of the period `.` separating the extension from this
 * [asString] path.
 */
val Path.extensionIndex
    get() = asString().lastIndexOf(".").takeIf { fileName.asString().contains(".") } ?: -1

/**
 * Returns the extension of the file described by this [Path].
 * Example: `/path/file.pdf` would return `pdf`.
 *
 * If no extension is present, `null` is returned.
 */
val Path.extensionOrNull: String?
    get() = extension.takeUnless { it.isBlank() }

val Path.extensions: Extensions
    get() = Extensions(this)

fun Path.addExtensions(extensions: String, vararg more: String): Path =
    this.extensions.add(extensions, *more)

/**
 * Returns whether this path [Path.getFileName] is the specified [extensions].
 *
 * The extension is compared ignoring the case and an eventually leading period by default.
 *
 * That is, `.ext`, `ext`, `.EXT` and `EXT` are all treated the same way.
 */
fun Path.hasExtensions(extensions: String, vararg more: String): Boolean =
    this.extensions.hasExtension(extensions, *more)

/**
 * Returns this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.withExtension(extension: String): Path =
    resolveSibling(fileNameWithExtension(extension))

/**
 * Returns the name of the file described by this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.fileNameWithExtension(extension: String): String = "$baseName.$extension"

/**
 * Removes [extensionOrNull] from this [Path].
 *
 * Example: `Path.of("/path/file.foo.bar").removeExtension("bar")` returns path `/path/file.foo`.
 *
 * @throws IllegalArgumentException if the [extensionOrNull] to be removed is not present
 */
fun Path.removeExtensions(extensions: String, vararg more: String): Path =
    this.extensions.remove(extensions, *more)
