package koodies.io.file

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Path

/**
 * Constructs a buffered output stream wrapping a new [FileOutputStream] of this file and returns it as a result.
 */
fun Path.bufferedOutputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream = outputStream().buffered(bufferSize)
