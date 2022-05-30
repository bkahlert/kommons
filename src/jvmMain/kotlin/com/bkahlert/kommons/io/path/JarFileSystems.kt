package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.tempFile
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
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

/**
 * Attempts to create a [FileSystem] from an existing `jar` file.
 *
 * @see tempJar
 */
public fun Path.asNewJarFileSystem(vararg env: Pair<String, Any?>): FileSystem =
    FileSystems.newFileSystem(URI.create("jar:${toUri()}"), env.toMap())

/**
 * Creates an empty `jar` file system the system's temp directory.
 *
 * @see tempJar
 * @see asNewJarFileSystem
 */
public fun Path.tempJarFileSystem(base: String = "", extension: String = ".jar"): FileSystem =
    tempJar(base, extension).asNewJarFileSystem()
