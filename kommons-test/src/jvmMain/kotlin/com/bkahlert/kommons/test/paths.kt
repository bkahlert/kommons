package com.bkahlert.kommons.test

import com.bkahlert.kommons.createTempFile
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.jar.JarOutputStream
import kotlin.io.path.outputStream

/**
 * Creates an empty `jar` file in the system's temp directory.
 *
 * @see toNewJarFileSystem
 */
public fun Path.createTempJarFile(prefix: String? = null, suffix: String? = ".jar"): Path =
    createTempFile(prefix, suffix).apply {
        JarOutputStream(outputStream().buffered()).use { }
    }

/**
 * Attempts to create a [FileSystem] from this existing `jar` file.
 *
 * @see createTempJarFile
 */
public fun Path.toNewJarFileSystem(vararg env: Pair<String, Any?>): FileSystem =
    FileSystems.newFileSystem(URI.create("jar:${toUri()}"), env.toMap())

/**
 * Creates an empty `jar` file system the default temp directory.
 *
 * @see createTempJarFile
 * @see toNewJarFileSystem
 */
public fun Path.createTempJarFileSystem(base: String = "", extension: String = ".jar"): FileSystem =
    createTempJarFile(base, extension).toNewJarFileSystem()
