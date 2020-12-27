package koodies.io

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Constructs a buffered input stream wrapping a new [FileInputStream] of this file and returns it as a result.
 */
fun File.bufferedInputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream =
    inputStream().buffered(bufferSize)

/**
 * Constructs a buffered output stream wrapping a new [FileOutputStream] of this file and returns it as a result.
 */
fun File.bufferedOutputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream =
    outputStream().buffered(bufferSize)
