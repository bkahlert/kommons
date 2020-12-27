package koodies.io.file

import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Attempts to create a [FileSystem] from an existing `jar` file.
 *
 * @see tempJar
 */
fun Path.asNewJarFileSystem(vararg env: Pair<String, Any?>): FileSystem =
    FileSystems.newFileSystem(URI.create("jar:${toUri()}"), env.toMap())
