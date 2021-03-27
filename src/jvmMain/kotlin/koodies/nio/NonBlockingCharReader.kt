package koodies.nio

import koodies.concurrent.process.IO
import koodies.debug.debug
import koodies.io.ByteArrayOutputStream
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.text.withRandomSuffix
import org.jline.utils.NonBlocking
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.seconds
import org.jline.utils.NonBlockingReader as JLineNonBlockingReader

/**
 * Reads a [ByteArrayOutputStream] line-wise using a special rule:
 * If no complete line is available the currently available content is returned.
 * This happens all over again until that line is complete, that is, terminated
 * by `\r\n` or `\n`.
 */
public class NonBlockingCharReader(
    private val inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val charset: Charset = Charsets.UTF_8,
    name: String = "ImgCstmzr-${NonBlockingCharReader::class.simpleName}".withRandomSuffix(),
) : Reader() {

    private val timeoutMillis: Long = timeout.toLongMilliseconds()
    private inline val inlineTimeoutMillis get() = timeoutMillis

    public var reader: JLineNonBlockingReader? = NonBlocking.nonBlocking(name, inputStream, charset)

    public fun read(buffer: CharArray, off: Int, logger: RenderingLogger): Int = if (reader == null) -1 else
        logger.compactLogging(NonBlockingCharReader::class.simpleName + ".read(CharArray, Int, Int, Logger)") {
            when (val read = kotlin.runCatching { reader?.read(inlineTimeoutMillis) ?: throw IOException("No reader. Likely already closed.") }
                .recover {
                    reader?.close()
                    -1
                }.getOrThrow()) {
                -1 -> {
                    logStatus { IO.META typed "EOF" }
                    -1
                }
                -2 -> {
                    logStatus { IO.META typed "TIMEOUT" }
                    0
                }
                else -> {
                    logStatus { IO.META typed "SUCCESSFULLY READ ${read.debug}" }
                    buffer[off] = read.toChar()
                    1
                }
            }
        }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = read(cbuf, off, MutedRenderingLogger())

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = "NonBlockingCharReader(inputStream=$inputStream, timeout=$timeout, charset=$charset, reader=$reader)"
}
