package koodies.io.file

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.file.Path

/**
 * Constructs a buffered input stream wrapping a new [FileInputStream] of this file and returns it as a result.
 */
fun Path.bufferedInputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream = inputStream().buffered(bufferSize)
