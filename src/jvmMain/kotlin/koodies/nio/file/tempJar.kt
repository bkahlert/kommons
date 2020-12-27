package koodies.io.file

import koodies.io.path.tempFile
import java.nio.file.Path
import java.util.jar.JarOutputStream

/**
 * Creates an empty `jar` file in the system's temp directory.
 *
 * @see asNewJarFileSystem
 */
fun tempJar(base: String = "", extension: String = ".jar"): Path =
    tempFile(base, extension).apply {
        JarOutputStream(bufferedOutputStream()).use { }
    }

/**
 * Creates an empty `jar` file in this directory.
 *
 * @see asNewJarFileSystem
 */
fun Path.tempJar(base: String = "", extension: String = ".jar"): Path =
    tempFile(base, extension).apply {
        JarOutputStream(bufferedOutputStream()).use { }
    }
