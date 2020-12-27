package koodies.nio.file

import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.readBytes

/**
 * Converts this path using Base64.
 */
fun Path.toBase64(): String = Base64.getEncoder().encodeToString(readBytes())
