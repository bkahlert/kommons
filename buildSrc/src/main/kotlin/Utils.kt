import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.stream.Stream
import kotlin.streams.toList
import kotlin.text.toBoolean as kotlinToBoolean

private val camelCaseRegex = Regex("(?<lowerLeftChar>[a-z0-9]|(?=[A-Z]))(?<upperRightChar>[A-Z])")
private fun CharSequence.convertCamelCase(separator: Char, transformator: (String) -> String): String = camelCaseRegex
    .replace(this.toString().decapitalize(), "\${lowerLeftChar}$separator\${upperRightChar}")
    .let(transformator)

private fun CharSequence.camelCaseToScreamingSnakeCase() = convertCamelCase('_', String::toUpperCase)

fun String?.toBoolean(default: Boolean = false): Boolean =
    this?.run { isBlank() || kotlinToBoolean() } ?: default

fun Project.findBooleanPropertyEverywhere(name: String, default: Boolean = false): Boolean =
    findPropertyEverywhere(name).toBoolean(default)

fun Project.findPropertyEverywhere(name: String): String? =
    extra.properties[name]?.toString()
        ?: findProperty(name)?.toString()
        ?: System.getenv(name.camelCaseToScreamingSnakeCase())

fun Project.findPropertyEverywhere(name: String, defaultValue: String): String =
    findPropertyEverywhere(name) ?: defaultValue

private var _releasingFinal: Boolean? = null
var Project.releasingFinal: Boolean
    get() = _releasingFinal ?: findBooleanPropertyEverywhere("releasingFinal", true)
    set(value) {
        _releasingFinal = value
    }
val Project.baseUrl: String get() = findPropertyEverywhere("baseUrl", "https://github.com/bkahlert/koodies")

private fun Path.getPathMatcher(glob: String): PathMatcher? {
    // avoid creating a matcher if all entries are required.
    if (glob == "*" || glob == "**" || glob == "**/*") return null

    // create a matcher and return a filter that uses it.
    return fileSystem.getPathMatcher("glob:$glob")
}

private fun Path.streamContentsRecursively(glob: String = "*"): Stream<Path> {
    if (!Files.isDirectory(this)) throw NotDirectoryException(toString())
    val walk = Files.walk(this).filter { it != this }
    return getPathMatcher(glob)
        ?.let { matcher -> walk.filter { path -> matcher.matches(path) } }
        ?: walk
}

/**
 * Returns a list of the entries in this directory and its sub directories
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.walk
 */
fun Path.listDirectoryEntriesRecursively(glob: String = "*"): List<Path> =
    streamContentsRecursively(glob).toList()

fun File.listDirectoryEntriesRecursively(glob: String = "*"): List<File> =
    toPath().streamContentsRecursively(glob).map { it.toFile() }.toList()


/**
 * Returns whether this object represents a final version number
 * of the format `<major>.<minor.<patch>`.
 */
fun Any.isFinal(): Boolean =
    Regex("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)").matches(toString())
