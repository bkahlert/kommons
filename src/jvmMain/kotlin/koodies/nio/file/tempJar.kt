package koodies.io.file

import koodies.io.path.bufferedOutputStream
import koodies.io.path.tempFile
import java.nio.file.Path
import java.util.jar.JarOutputStream

/**
 * Creates an empty `jar` file in the system's temp directory.
 *
 * @see asNewJarFileSystem
 */
public fun Path.tempJar(base: String = "", extension: String = ".jar"): Path =
    tempFile(base, extension).apply {
        JarOutputStream(bufferedOutputStream()).use { }
    }
