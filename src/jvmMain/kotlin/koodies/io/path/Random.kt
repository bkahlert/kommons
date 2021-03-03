package koodies.io.path

import koodies.text.randomString
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * Returns this [Path] with a path segment added.
 *
 * The path segment is created based on [base] and [extension] and a random
 * string in between.
 *
 * The newly created [Path] is guaranteed to not already exist.
 */
public tailrec fun Path.randomPath(base: String = randomString(4), extension: String = ""): Path {
    val minLength = 6
    val length = base.length + extension.length
    val randomLength = (minLength - length).coerceAtLeast(3)
    val randomPath = resolve("$base${randomString(randomLength)}$extension")
    return randomPath.takeUnless { it.exists() } ?: randomPath(base, extension)
}

/**
 * Creates a random directory inside this [Path].
 *
 * Eventually missing directories are automatically created.
 */
public fun Path.randomDirectory(base: String = randomString(4), extension: String = "-tmp"): Path =
    randomPath(base, extension).createDirectories()

/**
 * Creates a random file inside this [Path].
 *
 * Eventually missing directories are automatically created.
 */
fun Path.randomFile(base: String = randomString(4), extension: String = ".tmp"): Path =
    randomPath(base, extension).apply { parent.createDirectories() }.createFile()
