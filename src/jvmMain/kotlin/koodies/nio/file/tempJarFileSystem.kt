package koodies.io.file

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Creates an empty `jar` file system the system's temp directory.
 *
 * @see tempJar
 * @see asNewJarFileSystem
 */
fun tempJarFileSystem(base: String = "", extension: String = ".jar"): FileSystem =
    tempJar(base, extension).asNewJarFileSystem()

/**
 * Creates an empty `jar` file system in this directory.
 *
 * @see tempJar
 * @see asNewJarFileSystem
 */
fun Path.tempJarFileSystem(base: String = "", extension: String = ".jar"): FileSystem =
    tempJar(base, extension).asNewJarFileSystem()
