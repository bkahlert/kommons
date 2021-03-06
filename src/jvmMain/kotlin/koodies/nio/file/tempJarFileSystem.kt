package koodies.io.file

import java.nio.file.FileSystem

/**
 * Creates an empty `jar` file system the system's temp directory.
 *
 * @see tempJar
 * @see asNewJarFileSystem
 */
public fun tempJarFileSystem(base: String = "", extension: String = ".jar"): FileSystem =
    tempJar(base, extension).asNewJarFileSystem()
