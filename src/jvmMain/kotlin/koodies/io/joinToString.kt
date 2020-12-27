package koodies.io

import org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Joins this [List] of [ByteArray] instances to a [String]
 */
fun Iterable<ByteArray>.joinToString(charset: Charset = Charsets.UTF_8): String =
    ByteArrayOutputStream().also { outputStream -> forEach { outputStream.write(it) } }.toString(charset)
