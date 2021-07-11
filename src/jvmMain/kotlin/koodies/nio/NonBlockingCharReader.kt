package koodies.nio

import io.opentelemetry.api.trace.Span
import koodies.exec.mock.SlowInputStream
import koodies.io.ByteArrayOutputStream
import koodies.runWrapping
import koodies.text.withRandomSuffix
import koodies.time.seconds
import org.jline.utils.NonBlocking
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import kotlin.time.Duration
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

    private val timeoutMillis: Long = timeout.inWholeMilliseconds
    private inline val inlineTimeoutMillis get() = timeoutMillis

    public var reader: JLineNonBlockingReader? = NonBlocking.nonBlocking(name, inputStream, charset)

    public fun read(buffer: CharArray, off: Int): Int = if (reader == null) -1 else
//        spanning(
//            name = NonBlockingCharReader::class.simpleName + ".read()",
//            renderer = {
//                if (isDebugging) it(copy(decorationFormatter = { ANSI.Colors.brightCyan.invoke(it) }))
//                else Renderer.Companion.NOOP
//            },
//            tracer = if (isDebugging) Tracer else Tracer.NOOP,
//        ) {
        runWrapping({
            (inputStream as? SlowInputStream)?.run { parentSpan.also { parentSpan = Span.current() } }
        }, {
            (inputStream as? SlowInputStream)?.parentSpan = it ?: error("No parent span to restore")
        }) {
            when (val read = kotlin.runCatching { reader?.read(inlineTimeoutMillis) ?: throw IOException("No reader. Likely already closed.") }
                .recover {
                    reader?.close()
                    -1
                }.getOrThrow()) {
                -1 -> {
//                        log(IO.Meta typed "EOF")
                    -1
                }
                -2 -> {
//                        log(IO.Meta typed "TIMEOUT")
                    0
                }
                else -> {
//                        log(IO.Meta typed "SUCCESSFULLY READ ${read.debug}")
                    buffer[off] = read.toChar()
                    1
                }
            }.also {
                (inputStream as? SlowInputStream)?.parentSpan = Span.getInvalid()
            }
//            }
        }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = read(cbuf, off)

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = "NonBlockingCharReader(inputStream=$inputStream, timeout=$timeout, charset=$charset, reader=$reader)"
}
